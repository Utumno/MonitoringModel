package gr.uoa.di.monitoring.model;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import gr.uoa.di.monitoring.android.files.Parser;
import gr.uoa.di.monitoring.android.files.ParserException;
import gr.uoa.di.monitoring.android.files.Persist;
import gr.uoa.di.monitoring.android.files.Store;

import org.apache.http.util.EncodingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;

public final class Battery extends Data {

	String status;
	private static final String FILE_PREFIX = "batt";

	private Battery() {}

	private enum BatteryFields implements Fields<Intent, Battery, List<Byte>> {
		TIME {

			@Override
			public List<byte[]> getData(Intent data, final Battery out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				// NB : I just get the time of the method invocation
				final long currentTimeMillis = System.currentTimeMillis();
				out.time = currentTimeMillis;
				arrayList.add(currentTime(currentTimeMillis));
				return arrayList;
			}

			@Override
			public void parse(List<Byte> list, final Battery bat)
					throws ParserException {
				// reset internal Battery instance here ***
				try {
					bat.time = listToLong(list);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				}
			}
		},
		STATUS {

			@Override
			public List<byte[]>
					getData(Intent batteryStatus, final Battery out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				final String stat = batteryStatus.getIntExtra(
					BatteryManager.EXTRA_LEVEL, -1) + "";
				out.status = stat;
				arrayList.add(EncodingUtils.getAsciiBytes(stat));
				return arrayList;
			}

			@Override
			public void parse(List<Byte> list, final Battery bat)
					throws ParserException {
				try {
					bat.status = listToString(list, Store.FILES_ENCODING);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
			}
		};

		@Override
		public boolean isList() {
			return false; // no lists here
		}
	}

	// =========================================================================
	// Static API
	// =========================================================================
	// TODO move this into base class Data and make it generic ***
	public static List<Battery> parse(File f) throws IOException,
			ParserException {
		final File file = new File(f, FILE_PREFIX);
		final FileInputStream fis = new FileInputStream(file);
		List<EnumMap<BatteryFields, List<Byte>>> entries;
		try {
			entries = Parser.getEntries(fis, BatteryFields.class);
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				// could not close the file ?
				e.printStackTrace();
			}
		}
		final List<Battery> data = new ArrayList<Battery>();
		for (EnumMap<BatteryFields, List<Byte>> enumMap : entries) {
			Battery bat = new Battery(); // this is what prevents genericity - I
			// need to reflectively create a new instance - except if it were
			// created by the Fields somehow and were reset in TIME (***) and
			// then filled up - awkward
			for (BatteryFields field : enumMap.keySet()) {
				field.parse(enumMap.get(field), bat);
			}
			data.add(bat);
		}
		return data;
	}

	/**
	 * Constructs a Battery instance from the given string. Only the fields that
	 * matter to {@link #fairlyEqual(Data)} are filled (and time for debugging
	 * purposes)
	 */
	public static Battery fromString(String s) {
		if (s == null || s.trim().equals("")) return null;
		final Battery b = new Battery();
		String[] split = s.split(N);
		b.time = Long.valueOf(split[0]);
		b.status = split[1].split(IS)[1].trim();
		return b;
	}

	public static <T extends Enum<T> & Fields<?, ?, ?>> Battery saveData(
			Context ctx, Intent data) throws IOException {
		final Battery out = new Battery();
		List<byte[]> listByteArrays = createListOfByteArrays(data, out);
		Persist.saveData(ctx, FILE_PREFIX, listByteArrays);
		return out;
	}

	private static List<byte[]> createListOfByteArrays(Intent data,
			final Battery out) {
		if (out == null)
			throw new NullPointerException("out parameter can't be null");
		final List<byte[]> listByteArrays = new ArrayList<byte[]>();
		for (BatteryFields bs : BatteryFields.values()) {
			if (!bs.isList()) listByteArrays.add(bs.getData(data, out).get(0));
		}
		return listByteArrays;
	}

	// =========================================================================
	// API
	// =========================================================================
	@Override
	public String toString() {
		return super.toString() + N + "Status" + IS + status;
	}

	@Override
	public String stringForm() {
		return time + N + "Status" + IS + status;
	}

	/**
	 * Two Battery instances are fairlyEqual if they have the same status
	 *
	 * @throws NullPointerException
	 *             if d.status == null
	 */
	@Override
	public boolean fairlyEqual(final Data d) throws NullPointerException {
		if (d == null || !(d instanceof Battery)) return false;
		final Battery b = (Battery) d;
		return b.status.equals(this.status); // NPE here
	}
}
