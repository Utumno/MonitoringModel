package gr.uoa.di.monitoring.android.files;

import android.content.Context;

import gr.uoa.di.android.helpers.DeviceIdentifier;
import gr.uoa.di.android.helpers.DeviceIdentifier.DeviceIDException;
import gr.uoa.di.android.helpers.files.FileUtils;
import gr.uoa.di.android.helpers.files.Writer;
import gr.uoa.di.java.helpers.Zip.CompressException;
import gr.uoa.di.monitoring.model.Fields;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static gr.uoa.di.monitoring.android.files.Store.ARRAY_DELIMITER;
import static gr.uoa.di.monitoring.android.files.Store.DELIMITER;
import static gr.uoa.di.monitoring.android.files.Store.FILENAME_SEPA;
import static gr.uoa.di.monitoring.android.files.Store.NEWLINE;

public final class Persist {

	private Persist() {}

	public static final Object FILE_STORE_LOCK = new Object();
	private static final String NO_IMEI = "NO_IMEI";
	/** ALWAYS access this via {@link #getRootFolder(Context)} */
	private static volatile File sRootFolder; // TODO : cache but can lead to
	// NPEs - ask : enforce access to static fields via getter

	// =========================================================================
	// Public API - called by Model to store data
	// =========================================================================
	public static void saveData(Context ctx, String filename,
			List<byte[]> listByteArrays) throws FileNotFoundException,
			IOException {
		// internal storage
		persist(dataFileInInternalStorage(getRootFolder(ctx), filename),
			listByteArrays);
	}

	public static <T extends Enum<T> & Fields<?, ?, ?>> void saveData(
			Context ctx, String filename,
			List<List<byte[]>> listOfListsOfByteArrays, Class<T> fields)
			throws IOException {
		// internal storage
		persist(dataFileInInternalStorage(getRootFolder(ctx), filename),
			fields, listOfListsOfByteArrays);
	}

	// =========================================================================
	// Public API - used by Network Service to access stored data
	// =========================================================================
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
		return !FileUtils.isEmptyOrAbsent(getRootFolder(ctx)); // won't be
		// absent, getRootFolder() will create it
	}

	/**
	 * Deletes the files stored in the internal application directory.
	 *
	 * @param ctx
	 *            needed to retrieve the internal directory
	 * @return true if the files were successfully deleted, false if any one of
	 *         them failed to delete or if the directory does not exist and we
	 *         failed to create it
	 */
	public static boolean deleteInternalFiles(Context ctx) {
		boolean deleted = true;
		try {
			for (File f : FileUtils.listFiles(Persist.getRootFolder(ctx))) {
				deleted &= f.delete();
			}
		} catch (IOException e) {
			// the directory does not exist and getRootFolder() failed to create
			// it
			return false;
		}
		return deleted;
	}

	/**
	 * Returns a zip file containing the internal folder with the data files.
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
			return gr.uoa.di.java.helpers.Zip.zipFolder(rootPath, destination)
				.getFile();
			// final String backFilename = System.currentTimeMillis() + ".zip";
			// FileUtils.copyFileFromInternalToExternalStorage(destination,
			// LOG_DIR, backFilename);
		} catch (CompressException e) {
			throw new IOException("Unable to create zip file :" + e);
		}
	}

	// private persist methods
	/**
	 * Persists the items (arrays of bytes) contained in {@code listByteArrays}
	 * in the given {@code file} which may lie in external or internal storage.
	 * It acquires the {@code FILE_STORE_LOCK} to do so.
	 *
	 * @param file
	 * @param listByteArrays
	 * @throws IOException
	 */
	private static void persist(final File file,
			final List<byte[]> listByteArrays) throws IOException {
		/*
		 * The bytes contained in each item in the list are persisted in turn
		 * and are separated by the next chunk of bytes by DELIMITER. The last
		 * one is followed by NEWLINE (not DELIMITER).
		 */
		byte[] result = listOfArraysToByteArray(listByteArrays, DELIMITER,
			new byte[0]);
		result[result.length - 1] = NEWLINE;
		synchronized (FILE_STORE_LOCK) {
			Writer.append(file, result);
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
	 * @param listsOfByteArrays
	 * @throws FileNotFoundException
	 *             if the {@code file} is not found
	 * @throws IOException
	 */
	private static <T extends Enum<T> & Fields<?, ?, ?>> void persist(
			final File file, final Class<T> fields,
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
		int nextListOfArrays = 0;
		for (T field : fields.getEnumConstants()) {
			if (field.isList()) {
				result = listOfArraysToByteArray(
					listsOfByteArrays.get(nextListOfArrays++), ARRAY_DELIMITER,
					result);
				result[result.length - 1] = DELIMITER;
			} else {
				result = appendArrayToByteArray(
					listsOfByteArrays.get(nextListOfArrays++).get(0),
					DELIMITER, result);
			}
		}
		result[result.length - 1] = NEWLINE;
		synchronized (FILE_STORE_LOCK) {
			Writer.append(file, result);
		}
	}

	// private filesystem methods
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
			synchronized (Parser.class) {
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
					sRootFolder = result = FileUtils.createDirInternal(ctx,
						rootFoldername);
				}
			}
		}
		return result;
	}

	/**
	 * Creates the filename for the zip to send to the server. For now it is in
	 * the format imei_currentTimeMillis
	 *
	 * @param rootDir
	 *            the root folder the data are saved - the device ID (imei) -
	 *            just the folder name not a path
	 * @return the filename
	 */
	private static String filename(String rootDir) {
		return rootDir + FILENAME_SEPA + System.currentTimeMillis();
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
		// File internalDir = FileUtils.createDirInternal(ctx, sRootFolder);
		if (internalDir.isDirectory()) return new File(internalDir, filename);
		throw new IOException(internalDir.getAbsolutePath()
			+ ((internalDir.exists()) ? " is not a directory."
					: " does not exist."));
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
}
