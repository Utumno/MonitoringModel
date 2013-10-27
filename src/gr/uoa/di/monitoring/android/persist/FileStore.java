package gr.uoa.di.monitoring.android.persist;

import gr.uoa.di.android.helpers.DeviceIdentifier;
import gr.uoa.di.android.helpers.DeviceIdentifier.DeviceIDException;
import gr.uoa.di.android.helpers.FileIO;
import gr.uoa.di.android.helpers.Zip.CompressException;
import gr.uoa.di.java.helpers.Utils;
import gr.uoa.di.monitoring.model.Battery;
import gr.uoa.di.monitoring.model.Data;
import gr.uoa.di.monitoring.model.ParserException;
import gr.uoa.di.monitoring.model.Position;
import gr.uoa.di.monitoring.model.Wifi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context; // TODO : .... coupling

public final class FileStore {

	private static final String FILENAME_SEPA = "_";

	private FileStore() {}

	// TODO : can any of my data contain those delimiters ?
	private static final byte DELIMITER = 0;
	private static final byte ARRAY_DELIMITER = 1;
	private static final byte NEWLINE = '\n';
	private static final int INPUT_STREAM_BUFFER_SIZE = 8192; // vanilla default
	/**
	 * The parsers need to create strings from the files they parse - this field
	 * specifies the expected encoding of the files. Should be used by monitors
	 * whenever they write text files also TODO
	 */
	public static final String FILES_ENCODING = Utils.UTF8;
	public static final Object FILE_STORE_LOCK = new Object();

	/**
	 * Data Class + filename - would be great if overriding of static methods
	 * were permitted plus one could access them as Battery.getFilename()
	 * (getFilename() being static and abstract and defined in Data). Notice
	 * also that {@code Map<Class<? extends Enum<?> & Fields<?, ?, ?>>, String>}
	 * will fail to compile in a field declaration see <a
	 * href="http://stackoverflow.com/a/6643378/281545">here</a> TODO ask
	 *
	 * @return
	 */
	private final static Map<Class<? extends Data>, String> DATA_CLASSES = new HashMap<Class<? extends Data>, String>();
	static {
		DATA_CLASSES.put(Battery.class, new Battery("").getFilename());
		DATA_CLASSES.put(Position.class, new Position("").getFilename());
		DATA_CLASSES.put(Wifi.class, new Wifi("").getFilename());
	}

	// =========================================================================
	// Persistence
	// =========================================================================
	private static final String NO_IMEI = "NO_IMEI";
	/** ALWAYS access this via {@link #getRootFolder(Context)} */
	private static volatile File sRootFolder; // TODO : cache but can lead to
	// NPEs - ask : enforce access to static fields via getter

	/**
	 * Returns the root folder in internal storage where the data is kept. Will
	 * create it if it does not exist. {@code sRootFolder} must ONLY be accessed
	 * via this method. For now this folder is named according to a device UUID
	 * as returned by
	 * {@link DeviceIdentifier#getDeviceIdentifier(Context, boolean)} - this is
	 * subject to change
	 *
	 * @param ctx
	 *            the Android context of the Monitors
	 * @return the root folder where the data files are saved
	 * @throws IOException
	 *             if the directory does not exist and fails to create it
	 */
	private static synchronized File getRootFolder(Context ctx)
			throws IOException {
		File result = sRootFolder;
		if (result == null) {
			synchronized (FileStore.class) {
				result = sRootFolder;
				if (result == null) {
					String rootFoldername;
					try {
						rootFoldername = DeviceIdentifier.getDeviceIdentifier(
							ctx, false);
					} catch (DeviceIDException e) {
						// w("No imei today : " + e);
						rootFoldername = NO_IMEI;
					}
					sRootFolder = result = FileIO.createDirInternal(ctx,
						rootFoldername);
				}
			}
		}
		return result;
	}

	/**
	 * Gets the device ID from the filename as it comes from the mobile
	 *
	 * @param filename
	 *            just the filename not a path
	 * @return the deviceID
	 */
	public static String getDeviceID(String filename) {
		// as it is the device ID is the first part of the filename
		return filename.split(FILENAME_SEPA)[0];
	}

	/**
	 * Creates the filename for the zip to send to the server. For now it is in
	 * the format imei_currentTimeMillis
	 *
	 * @param rootPath
	 *            the root folder the data are saved - the device ID (imei) -
	 *            just the folder name not a path
	 * @return the filename
	 */
	private static String filename(String rootPath) {
		return rootPath + FILENAME_SEPA + System.currentTimeMillis();
	}

	/**
	 * Returns a File instance representing a file in an internal directory. The
	 * internalDir specified must exist and be a directory.
	 *
	 * @param internalDir
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	private static File dataFileInInternalStorage(File internalDir,
			String filename) throws IOException {
		// File internalDir = FileIO.createDirInternal(ctx, sRootFolder);
		if (internalDir.exists() && internalDir.isDirectory())
			return new File(internalDir, filename);
		throw new IOException(internalDir.getAbsolutePath()
			+ " does not exist or is not a directory.");
	}

	// public
	public static void saveData(Context ctx, String filename,
			List<byte[]> listByteArrays) throws FileNotFoundException,
			IOException {
		// internal storage
		FileStore.persist(
			dataFileInInternalStorage(FileStore.getRootFolder(ctx), filename),
			listByteArrays);
	}

	public static <T extends Enum<T> & Fields<?, ?, ?>> void saveData(
			Context ctx, String filename, List<byte[]> listByteArrays,
			List<List<byte[]>> listOfListsOfByteArrays, Class<T> fields)
			throws FileNotFoundException, IOException {
		// internal storage
		FileStore.persist(
			dataFileInInternalStorage(FileStore.getRootFolder(ctx), filename),
			fields, listByteArrays, listOfListsOfByteArrays);
	}

	/**
	 * Persists the items (arrays of bytes) contained in {@code listByteArrays}
	 * in the given {@code file} which may lie in external or internal storage.
	 * It acquires the {@code FILE_STORE_LOCK} to do so.
	 *
	 * @param file
	 * @param listByteArrays
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void persist(final File file,
			final List<byte[]> listByteArrays) throws FileNotFoundException,
			IOException {
		/*
		 * The bytes contained in each item in the list are persisted in turn
		 * and are separated by the next chunk of bytes by DELIMITER. The last
		 * one is followed by NEWLINE (not DELIMITER).
		 */
		byte[] result = listOfArraysToByteArray(listByteArrays, DELIMITER,
			new byte[0]);
		result[result.length - 1] = NEWLINE;
		synchronized (FILE_STORE_LOCK) {
			FileIO.append(file, result);
		}
	}

	/**
	 * Persists the given data in the given {@code file} which may lie in
	 * external or internal storage. It acquires the {@code FILE_STORE_LOCK} to
	 * do so.
	 *
	 * @param <T>
	 *            must be an enum that extends Fields
	 * @param file
	 * @param fields
	 * @param listByteArrays
	 * @param listsOfByteArrays
	 * @throws FileNotFoundException
	 *             if the {@code file} is not found
	 * @throws IOException
	 */
	public static <T extends Enum<T> & Fields<?, ?, ?>> void persist(
			final File file, final Class<T> fields,
			final List<byte[]> listByteArrays,
			final List<List<byte[]>> listsOfByteArrays)
			throws FileNotFoundException, IOException {
		/*
		 * Each item (chunk of bytes) is separated by the next one by DELIMITER.
		 * The items may be either a byte[] (an element of the {@code
		 * listByteArrays}), or a whole List<byte[]> (member of {@code
		 * listsOfByteArrays}). In the latter case each byte[] belonging to
		 * {@code listsOfByteArrays} is persisted in turn and separated by the
		 * next by ARRAY_DELIMITER. The last array is separated by the next
		 * chunk by DELIMITER as before. The order of the items is given by the
		 * order of the Lists themselves in combination with the {@code fields}
		 * which also provides the info whether the next item is to be retrieved
		 * from {@code listsOfByteArrays} or {@code listByteArrays}. The last
		 * item is followed by NEWLINE (not DELIMITER).
		 */
		byte[] result = new byte[0];
		int nextArray = 0;
		int nextListOfArrays = 0;
		for (T field : fields.getEnumConstants()) {
			if (field.isList()) {
				result = listOfArraysToByteArray(
					listsOfByteArrays.get(nextListOfArrays++), ARRAY_DELIMITER,
					result);
				result[result.length - 1] = DELIMITER;
			} else {
				result = appendArrayToByteArray(
					listByteArrays.get(nextArray++), DELIMITER, result);
			}
		}
		result[result.length - 1] = NEWLINE;
		synchronized (FILE_STORE_LOCK) {
			FileIO.append(file, result);
		}
	}

	/**
	 * Checks if there are available data by checking the size of the internal
	 * directory where the data is saved. If the internal directory does not
	 * exist this method will try to create it
	 *
	 * @param ctx
	 *            needed to retrieve the internal directory
	 * @return true if the directory exists (will exist except if IOException is
	 *         thrown on creation) and is not empty, false otherwise
	 * @throws IOException
	 *             if the internal directory can't be created
	 */
	public static boolean availableData(Context ctx) throws IOException {
		return !FileIO.isEmptyOrAbsent(getRootFolder(ctx)); // won't be absent,
		// getRootFolder() will create it
	}

	/**
	 * Returns a list of application files in the internal directory where the
	 * data is saved. If the internal directory does not exist this method will
	 * try to create it
	 *
	 * @param ctx
	 *            needed to retrieve the internal directory
	 * @return true if the directory exists and is not empty, false otherwise
	 * @throws IOException
	 *             if the internal directory can't be created
	 */
	public static List<File> internalFiles(Context ctx) throws IOException {
		return FileIO.listFiles(getRootFolder(ctx));
	}

	/**
	 * Returns a zip file containing the internal folder with the data files
	 *
	 * @param ctx
	 *            Context needed to access the internal storage
	 * @return a zip file containing the folder with the data files
	 * @throws IOException
	 *             if the internal folder can't be accessed or the zip file
	 *             can't be created
	 */
	public static File file(Context ctx) throws IOException {
		try {
			final String rootPath = getRootFolder(ctx).getAbsolutePath();
			final String destination = filename(rootPath);
			return gr.uoa.di.android.helpers.Zip.zipFolder(rootPath,
				destination).getFile();
			// final String backFilename = System.currentTimeMillis() + ".zip";
			// FileIO.copyFileFromInternalToExternalStorage(destination,
			// LOG_DIR, backFilename);
		} catch (CompressException e) {
			throw new IOException("Unable to create zip file :" + e);
		}
	}

	// helpers
	/**
	 * Turns {@code listByteArrays} (a List<byte[]>) to a byte[] array appended
	 * to {@code result}. Each item in the list is separated by its next with
	 * {@code delimiter}. The {@code result} must end in delimiter if not empty
	 *
	 * @param listByteArrays
	 * @param delimiter
	 * @param result
	 * @return
	 */
	private static byte[] listOfArraysToByteArray(
			final List<byte[]> listByteArrays, final byte delimiter,
			byte[] result) {
		for (byte[] array : listByteArrays) {
			result = appendArrayToByteArray(array, delimiter, result);
		}
		return result;
	}

	// =========================================================================
	// String versions // FIXME locks or maybe delete them altogether
	// =========================================================================
	public static void persist(File file, String encodingName,
			List<String> listString) throws FileNotFoundException,
			UnsupportedEncodingException, IOException {
		byte[] result = listOfStringsToByteArray(listString, encodingName,
			DELIMITER, new byte[0]);
		result[result.length - 1] = NEWLINE;
		FileIO.append(file, result);
	}

	public static <T extends Enum<T> & Fields<?, ?, ?>> void persist(
			final File file, final Class<T> fields, final String encodingName,
			final List<String> data, final List<String>... listsOfData)
			throws FileNotFoundException, UnsupportedEncodingException,
			IOException {
		byte[] result = new byte[0];
		int nextString = 0;
		int nextList = 0;
		for (T field : fields.getEnumConstants()) {
			if (field.isList()) {
				result = listOfStringsToByteArray(listsOfData[nextList++],
					encodingName, ARRAY_DELIMITER, result);
				result[result.length - 1] = DELIMITER;
			} else {
				result = appendStringToByteArray(data.get(nextString++),
					encodingName, DELIMITER, result);
			}
		}
		result[result.length - 1] = NEWLINE;
		FileIO.append(file, result);
	}

	// helpers
	private static byte[] listOfStringsToByteArray(final List<String> data,
			final String encodingName, final byte delimiter, byte[] result)
			throws UnsupportedEncodingException {
		for (String string : data) {
			result = appendStringToByteArray(string, encodingName, delimiter,
				result);
		}
		return result;
	}

	private static byte[] appendStringToByteArray(final String string,
			final String encodingName, final byte delimiter, byte[] result)
			throws UnsupportedEncodingException {
		final byte[] second = string.getBytes(encodingName);
		final byte[] first = result.clone();
		result = new byte[first.length + second.length + 1];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		result[result.length - 1] = delimiter;
		return result;
	}

	/**
	 * Appends {@code array} to {@code result} and adds the {@code delimiter} at
	 * the last position of the returned array.
	 *
	 * @param array
	 * @param delimiter
	 * @param result
	 * @return
	 */
	private static byte[] appendArrayToByteArray(final byte[] array,
			final byte delimiter, byte[] result) {
		final byte[] first = result.clone();
		result = new byte[first.length + array.length + 1];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(array, 0, result, first.length, array.length);
		result[result.length - 1] = delimiter;
		return result;
	}

	// =========================================================================
	// Fields
	// =========================================================================
	/**
	 * Interface meant to be implemented by enums that encapsulate in their
	 * constants the methods to get the Data needed from android objects and
	 * parse the Lists of bytes from those serialized in the files sent to the
	 * servers
	 *
	 * @param <T>
	 *            the type of the data provided by android, ex. Location
	 * @param <D>
	 *            the corresponding type of the data in the model, ex. Position
	 * @param <K>
	 *            the list that the parser expects ({@code List<Byte>}or
	 *            {@code List<List<Byte>>}
	 */
	public static interface Fields<T, D extends Data, K> {

		boolean isList();

		List<byte[]> getData(T data);

		D parse(K list, D objectToModify) throws ParserException;
	}

	// =========================================================================
	// Parsers
	// =========================================================================
	public static <T extends Data> Map<Class<? extends Data>, List<T>> parse(
			String rootPath, String imei) throws ParserException {
		Map<Class<? extends Data>, List<T>> lol = new HashMap<Class<? extends Data>, List<T>>();
		for (Entry<Class<? extends Data>, String> entry : DATA_CLASSES
				.entrySet()) {
			final File file = new File(rootPath, entry.getValue());
			final Class<? extends Data> dataCls = entry.getKey();
			Method pars = null;
			try {
				pars = dataCls.getMethod("parse", File.class, String.class);
			} catch (NoSuchMethodException e) {
				throw new ParserException("Reflection failure", e);
			}
			List<T> invoke = null;
			try {
				invoke = (List<T>) pars.invoke(null, file, imei);
			} catch (IllegalArgumentException e) {
				throw new ParserException("Reflection failure", e);
			} catch (IllegalAccessException e) {
				throw new ParserException("Reflection failure", e);
			} catch (InvocationTargetException e) {
				// FIXME - reflection is rather awkward - drop it ?
				if (e.getCause() instanceof FileNotFoundException) {
					// the file was not found - not a fatal error
					invoke = Collections.emptyList();
				}
			}
			lol.put(dataCls, invoke);
		}
		return lol;
	}

	/**
	 * Meant to be used in the server so uses Lists internally instead of
	 * arrays. Buffers the input stream so do not pass in a BufferedInputStream.
	 * See <a href="http://stackoverflow.com/questions/19067244/">What is the
	 * result of buffering a buffered stream in java?</a>
	 *
	 * @param <K>
	 *
	 * @param is
	 *            an input stream
	 * @param f
	 *            the Fields class of interest
	 * @return A list (one element per entry in the file) of EnumMapS mapping
	 *         from the fields to either a list of bytes or a list of lists of
	 *         bytes for fields where isList() returns true
	 * @throws IOException
	 */
	public static <D, T extends Enum<T> & Fields<?, ?, D>> List<EnumMap<T, D>> getEntries(
			InputStream is, Class<T> fields) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is,
				INPUT_STREAM_BUFFER_SIZE);
		final List<List<Byte>> entries = new ArrayList<List<Byte>>();
		{
			// get the entries
			boolean kaboom = true;
			for (int c = bis.read(), line = -1; c != -1; c = bis.read()) {
				if (kaboom) {
					entries.add(new ArrayList<Byte>());
					kaboom = false;
					++line;
				}
				if (c == NEWLINE) kaboom = true;
				else entries.get(line).add((byte) c);
			}
		}
		// friggin FIXME : malformed files ? empty lines ?
		final boolean hasLists = hasLists(fields);
		// this is the only unchecked warning
		@SuppressWarnings({ "unchecked", "rawtypes" })
		GetEntries<D, T> getEntries = hasLists ? new GetByteListEntries(fields)
				: new GetByteEntries(fields);
		return getEntries.invoke(entries);
	}

	private abstract static class GetEntries<D, T extends Enum<T> & Fields<?, ?, D>> {

		Class<T> fields;

		public GetEntries(Class<T> fields) {
			this.fields = fields;
		}

		public List<EnumMap<T, D>> invoke(List<List<Byte>> entries) {
			final List<EnumMap<T, D>> daBytes = new ArrayList<EnumMap<T, D>>();
			final int numOfEntries = entries.size();
			for (int currentEntry = 0; currentEntry < numOfEntries; ++currentEntry) {
				List<Byte> entry = entries.get(currentEntry);
				daBytes.add(new EnumMap<T, D>(fields));
				EnumMap<T, D> map = daBytes.get(currentEntry);
				// and now the party - getting the actual fields for this entry
				for (T daField : fields.getEnumConstants()) {
					List<Byte> field = new ArrayList<Byte>();
					int position = 0;
					for (byte b : entry) {
						++position;
						if (b == DELIMITER) {
							entry = new ArrayList<Byte>(entry.subList(position,
								entry.size())); // chop off the bytes I read
							break;
						}
						field.add(b);
					} // got the field bytes
					map.put(daField, getFieldData(daField, field));
				}
			}
			return daBytes;
		}

		abstract D getFieldData(T daField, List<Byte> field);
	}

	private final static class GetByteEntries<T extends Enum<T> & Fields<?, ?, List<Byte>>>
			extends GetEntries<List<Byte>, T> {

		public GetByteEntries(Class<T> fields) {
			super(fields);
		}

		@Override
		List<Byte> getFieldData(T daField, List<Byte> field) {
			return field;
		}
	}

	private final static class GetByteListEntries<T extends Enum<T> & Fields<?, ?, List<List<Byte>>>>
			extends GetEntries<List<List<Byte>>, T> {

		public GetByteListEntries(Class<T> fields) {
			super(fields);
		}

		@Override
		List<List<Byte>> getFieldData(T daField, List<Byte> field) {
			final List<List<Byte>> fieldEntries = new ArrayList<List<Byte>>();
			if (!daField.isList()) {
				fieldEntries.add(field); // one entry only
			} else {
				// parse the field
				boolean kaboom = true;
				int fieldEntry = -1;
				for (byte b : field) {
					if (kaboom) {
						fieldEntries.add(new ArrayList<Byte>());
						kaboom = false;
						++fieldEntry;
					}
					if (b == ARRAY_DELIMITER) kaboom = true;
					else fieldEntries.get(fieldEntry).add(b);
				}
			}
			return fieldEntries;
		}
	}

	// That's what you get for nor being able to override static methods
	private static <T extends Enum<T> & Fields<?, ?, ?>> boolean hasLists(
			Class<T> fields) {
		boolean hasLists = false;
		for (T field : fields.getEnumConstants()) {
			if (field.isList()) {
				hasLists = true;
				break;
			}
		}
		return hasLists;
	}
}
