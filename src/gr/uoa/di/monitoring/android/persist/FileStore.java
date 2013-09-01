package gr.uoa.di.monitoring.android.persist;

import gr.uoa.di.android.helpers.FileIO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public final class FileStore {

	private static final byte DELIMITER = 0;
	private static final byte ARRAY_DELIMITER = 1;
	private static final byte NEWLINE = '\n';

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
	}
	// public static <T> T[] concat(T[] first, T[] second) {
	// T[] result = Arrays.copyOf(first, first.length + second.length); // NOT
	// // fucki9ng available
	// System.arraycopy(second, 0, result, first.length, second.length);
	// return result;
	// }
}
