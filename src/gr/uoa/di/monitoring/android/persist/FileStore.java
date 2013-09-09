package gr.uoa.di.monitoring.android.persist;

import gr.uoa.di.android.helpers.FileIO;
import gr.uoa.di.java.helpers.Utils;
import gr.uoa.di.monitoring.model.ParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public final class FileStore {

	// TODO : can any of my data contain those delimiters ?
	private static final byte DELIMITER = 0;
	private static final byte ARRAY_DELIMITER = 1;
	private static final byte NEWLINE = '\n';
	private static final int INPUT_STEAM_BUFFER_SIZE = 8192; // vanilla default
	/**
	 * The parsers need to create strings from the files they parse - this field
	 * specifies the expected encoding of the files. Should be used by monitors
	 * whenever they write text files also TODO
	 */
	public static final String FILES_ENCODING = Utils.UTF8;

	private FileStore() {}

	/**
	 * Persists the items (arrays of bytes) contained in {@code listByteArrays}
	 * in the given {@code file} which may lie in external or internal storage.
	 * The bytes contained in each item in the list are persisted in turn and
	 * are separated by the next chunk of bytes by DELIMITER. The last one is
	 * followed by NEWLINE (not DELIMITER).
	 *
	 * @param file
	 * @param listByteArrays
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void persist(final File file,
			final List<byte[]> listByteArrays) throws FileNotFoundException,
			IOException {
		byte[] result = listOfArraysToByteArray(listByteArrays, DELIMITER,
			new byte[0]);
		result[result.length - 1] = NEWLINE;
		FileIO.append(file, result);
	}

	/**
	 * Persists the given data in the given {@code file} which may lie in
	 * external or internal storage. Each item (chunk of bytes) is separated by
	 * the next one by DELIMITER. The items may be either a byte[] (an element
	 * of the {@code listByteArrays}), or a whole List<byte[]> (member of
	 * {@code listsOfByteArrays}). In the latter case each byte[] belonging to
	 * {@code listsOfByteArrays} is persisted in turn and separated by the next
	 * by ARRAY_DELIMITER. The last array is separated by the next chunk by
	 * DELIMITER as before. The order of the items is given by the order of the
	 * Lists themselves in combination with the {@code fields} which also
	 * provides the info whether the next item is to be retrieved from
	 * {@code listsOfByteArrays} or {@code listByteArrays}. The last item is
	 * followed by NEWLINE (not DELIMITER).
	 *
	 * @param <T>
	 *            must be an enum that extends Fields
	 *
	 * @param file
	 * @param fields
	 * @param listByteArrays
	 * @param listsOfByteArrays
	 * @throws FileNotFoundException
	 *             if the {@code file} is not found
	 * @throws IOException
	 */
	public static <T extends Enum<T> & Fields> void persist(final File file,
			final Class<T> fields, final List<byte[]> listByteArrays,
			final List<List<byte[]>> listsOfByteArrays)
			throws FileNotFoundException, IOException {
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
		FileIO.append(file, result);
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
	// String versions
	// =========================================================================
	public static void persist(File file, String encodingName,
			List<String> listString) throws FileNotFoundException,
			UnsupportedEncodingException, IOException {
		byte[] result = listOfStringsToByteArray(listString, encodingName,
			DELIMITER, new byte[0]);
		result[result.length - 1] = NEWLINE;
		FileIO.append(file, result);
	}

	public static <T extends Enum<T> & Fields> void persist(final File file,
			final Class<T> fields, final String encodingName,
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

	public static interface Fields {

		// <T> List<byte[]> createListOfByteArrays(T data);
		boolean isList();

		<T> List<byte[]> getData(T data);

		<K, D> D parse(List<K> list, D objectToModify)
				throws ParserException;
	}

	// public static <T> T[] concat(T[] first, T[] second) {
	// T[] result = Arrays.copyOf(first, first.length + second.length); // NOT
	// // fucki9ng available
	// System.arraycopy(second, 0, result, first.length, second.length);
	// return result;
	// }
	/**
	 * Meant to be used in the server so uses Lists internally instead of arrays
	 * Buffers the input stream so do not pass in a BufferedInputStream (TODO
	 * ask - maybe not important)
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
	public static <D, T extends Enum<T> & Fields> List<EnumMap<T, D>> getEntries(
			InputStream is, Class<T> fields) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is,
				INPUT_STEAM_BUFFER_SIZE);
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
		final boolean hasLists = hasLists(fields);
		List<D> daBytes;
		if (hasLists) {
			daBytes = (List<D>) new ArrayList<EnumMap<T, List<List<Byte>>>>();
		} else {
			daBytes = (List<D>) new ArrayList<EnumMap<T, List<Byte>>>();
		}
		final int numOfEntries = entries.size();
		for (int currentEntry = 0; currentEntry < numOfEntries; ++currentEntry) {
			List<Byte> entry = entries.get(currentEntry);
			// add an element in daBytes for this entry
			if (hasLists) {
				daBytes.add((D) new EnumMap<T, List<List<Byte>>>(fields));
			} else {
				daBytes.add((D) new EnumMap<T, List<Byte>>(fields));
			}
			// and now the party - getting the actual fields for this entry
			for (T daField : fields.getEnumConstants()) {
				List<Byte> field = new ArrayList<Byte>();
				{
					int position = 0;
					for (byte b : entry) {
						++position;
						if (b == DELIMITER) {
							entry = new ArrayList<Byte>(entry.subList(position,
								entry.size())); // chop off the bytes I read
							break;
						} else field.add(b);
					}
				}// got the field bytes
				D map = daBytes.get(currentEntry);
				if (hasLists) {
					List<List<Byte>> fieldEntries = new ArrayList<List<Byte>>();
					if (!daField.isList()) {
						// one entry only
						fieldEntries.add(field);
					} else {
						{
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
					}
					((EnumMap<T, List<List<Byte>>>) map).put(daField,
						fieldEntries);
				} else {
					((EnumMap<T, List<Byte>>) map).put(daField, field);
				}
			}
		}
		return (List<EnumMap<T, D>>) daBytes;
	}

	// That's what you get for nor being able to override static methods
	private static <T extends Enum<T> & Fields> boolean hasLists(Class<T> fields) {
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
