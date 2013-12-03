package gr.uoa.di.monitoring.android.files;

import gr.uoa.di.monitoring.model.Battery;
import gr.uoa.di.monitoring.model.Data;
import gr.uoa.di.monitoring.model.Fields;
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

import static gr.uoa.di.monitoring.android.files.Store.ARRAY_DELIMITER;
import static gr.uoa.di.monitoring.android.files.Store.DELIMITER;
import static gr.uoa.di.monitoring.android.files.Store.FILENAME_SEPA;
import static gr.uoa.di.monitoring.android.files.Store.NEWLINE;

public final class Parser {

	private Parser() {}

	private static final int INPUT_STREAM_BUFFER_SIZE = 8192; // vanilla default
	/**
	 * Data Class used in {@link #parse(String)} to call the appropriate static
	 * parse() method from the relevant Data subclass.
	 */
	private final static List<Class<? extends Data>> DATA_CLASSES =
			new ArrayList<Class<? extends Data>>();
	static {
		DATA_CLASSES.add(Battery.class);
		DATA_CLASSES.add(Position.class);
		DATA_CLASSES.add(Wifi.class);
	}

	// =========================================================================
	// API - used by the servlet
	// =========================================================================
	/**
	 * Gets the device ID from the filename as it comes from the mobile. TODO:
	 * pass this with the data so I can avoid the import of this class in the
	 * servlet
	 *
	 * @param filename
	 *            just the filename not a path
	 * @return the deviceID
	 */
	public static String getDeviceID(final String filename) {
		// as it is the device ID is the first part of the filename
		return filename.split(FILENAME_SEPA)[0];
	}

	/**
	 * Parses the files in rootPath directory and returns a map from the Data
	 * type to a List of Data instances.
	 *
	 * @param rootPath
	 *            a directory containing files to be parsed
	 * @return a Map with keys the Data classes and values Lists of data
	 *         instances
	 * @throws ParserException
	 *             if parsing failed
	 */
	public static <T extends Data> Map<Class<? extends Data>, List<T>> parse(
			String rootPath) throws ParserException {
		Map<Class<? extends Data>, List<T>> daMap =
				new HashMap<Class<? extends Data>, List<T>>();
		for (Class<? extends Data> dataCls : DATA_CLASSES) {
			Method pars = null;
			try {
				pars = dataCls.getMethod("parse", File.class);
			} catch (NoSuchMethodException e) {
				throw new ParserException("Reflection failure", e);
			}
			List<T> invoke = null;
			try {
				// TODO - reflection is rather awkward - drop it ?
				invoke = (List<T>) pars.invoke(null, new File(rootPath));
			} catch (IllegalArgumentException e) {
				throw new ParserException("Reflection failure", e);
			} catch (IllegalAccessException e) {
				throw new ParserException("Reflection failure", e);
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof FileNotFoundException) {
					// the file was not found - no data - not a fatal error
					invoke = Collections.emptyList();
				}
			}
			daMap.put(dataCls, invoke);
		}
		return daMap;
	}

	// =========================================================================
	// API - used by the model
	// =========================================================================
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

	// =========================================================================
	// Private helpers of getEntries()
	// =========================================================================
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
		for (T field : fields.getEnumConstants()) {
			if (field.isList()) return true;
		}
		return false;
	}
}
