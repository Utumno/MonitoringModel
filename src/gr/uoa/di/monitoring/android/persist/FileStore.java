package gr.uoa.di.monitoring.android.persist;

import gr.uoa.di.java.helpers.Utils;
import gr.uoa.di.monitoring.model.Battery;
import gr.uoa.di.monitoring.model.Data;
import gr.uoa.di.monitoring.model.Position;
import gr.uoa.di.monitoring.model.Wifi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class FileStore {

	private FileStore() {}

	static final String FILENAME_SEPA = "_";
	// ISSUE 6 : only the SSIDs can contain it - I must provide for it
	static final byte DELIMITER = 0;
	static final byte ARRAY_DELIMITER = 1;
	static final byte NEWLINE = '\n';
	private static final int INPUT_STREAM_BUFFER_SIZE = 8192; // vanilla default
	/**
	 * The parsers need to create strings from the files they parse - this field
	 * specifies the expected encoding of the files. Should be used by monitors
	 * whenever they write text files also TODO
	 */
	public static final String FILES_ENCODING = Utils.UTF8;
	/**
	 * Data Class + filename - would be great if overriding of static methods
	 * were permitted plus one could access them as Battery.getFilename()
	 * (getFilename() being static and abstract and defined in Data). Notice
	 * also that {@code Map<Class<? extends Enum<?> & Fields<?, ?, ?>>, String>}
	 * will fail to compile in a field declaration see <a
	 * href="http://stackoverflow.com/a/6643378/281545">here</a> TODO ask
	 */
	private final static Map<Class<? extends Data>, String> DATA_CLASSES =
			new HashMap<Class<? extends Data>, String>();
	static {
		DATA_CLASSES.put(Battery.class, new Battery().getFilename());
		DATA_CLASSES.put(Position.class, new Position().getFilename());
		DATA_CLASSES.put(Wifi.class, new Wifi().getFilename());
	}

	/**
	 * Gets the device ID from the filename as it comes from the mobile. TODO:
	 * pass this with the data so I can avoid the import of this class in the
	 * servlet
	 *
	 * @param filename
	 *            just the filename not a path
	 * @return the deviceID
	 */
	public static String getDeviceID(String filename) {
		// as it is the device ID is the first part of the filename
		return filename.split(FILENAME_SEPA)[0];
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

		List<byte[]> getData(T data, D output);

		D parse(K list, D objectToModify) throws ParserException;
	}

	// =========================================================================
	// Parsers
	// =========================================================================
	public static <T extends Data> Map<Class<? extends Data>, List<T>> parse(
			String rootPath) throws ParserException {
		Map<Class<? extends Data>, List<T>> lol =
				new HashMap<Class<? extends Data>, List<T>>();
		for (Entry<Class<? extends Data>, String> entry : DATA_CLASSES
			.entrySet()) {
			final File file = new File(rootPath, entry.getValue());
			final Class<? extends Data> dataCls = entry.getKey();
			Method pars = null;
			try {
				pars = dataCls.getMethod("parse", File.class);
			} catch (NoSuchMethodException e) {
				throw new ParserException("Reflection failure", e);
			}
			List<T> invoke = null;
			try {
				invoke = (List<T>) pars.invoke(null, file);
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
	 * @param is
	 *            an input stream, should be a FileInputStream of a file
	 *            containing data to be parsed by the Fields class given
	 * @param f
	 *            the Fields class of interest
	 * @return A list (one element per entry in the file) of EnumMapS mapping
	 *         from the fields to either a list of bytes or a list of lists of
	 *         bytes for fields where isList() returns true
	 * @throws IOException
	 */
	public static <D, T extends Enum<T> & Fields<?, ?, D>> List<EnumMap<T, D>>
			getEntries(InputStream is, Class<T> fields) throws IOException {
		final BufferedInputStream bis = new BufferedInputStream(is,
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
