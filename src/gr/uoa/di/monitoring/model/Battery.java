package gr.uoa.di.monitoring.model;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import gr.uoa.di.monitoring.android.persist.FileStore;
import gr.uoa.di.monitoring.android.persist.FileStore.Fields;
import gr.uoa.di.monitoring.android.persist.ParserException;
import gr.uoa.di.monitoring.android.persist.Persist;

import org.apache.http.util.EncodingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static gr.uoa.di.java.helpers.Utils.listFromArray;
import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;

public final class Battery extends Data {

	private String status;
	private static final String FILE_PREFIX = "batt";

	public static enum BatteryFields implements
			Fields<Intent, Battery, List<Byte>> {
		TIME {

			@Override
			public List<byte[]> getData(Intent data) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				// NB : I just get the time of the method invocation
				arrayList.add(currentTime());
				return arrayList;
			}

			@Override
			public Battery parse(List<Byte> list, Battery bat)
					throws ParserException {
				try {
					bat.time = listToLong(list);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				}
				return bat;
			}
		},
		STATUS {

			@Override
			public List<byte[]> getData(Intent batteryStatus) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(batteryStatus
					.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) + ""));
				return arrayList;
			}

			@Override
			public Battery parse(List<Byte> list, Battery bat)
					throws ParserException {
				try {
					bat.status = listToString(list, FileStore.FILES_ENCODING);
					return bat;
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
			}
		};

		@Override
		public boolean isList() {
			return false; // no lists here
		}

		public static List<byte[]> createListOfByteArrays(Intent data) {
			final List<byte[]> listByteArrays = new ArrayList<byte[]>();
			for (BatteryFields bs : BatteryFields.values()) {
				if (!bs.isList()) listByteArrays.add(bs.getData(data).get(0));
			}
			return listByteArrays;
		}
	}

	// TODO move this into base class Data and make it abstract
	public static List<Battery> parse(File f) throws IOException,
			ParserException {
		final FileInputStream fis = new FileInputStream(f);
		List<EnumMap<BatteryFields, List<Byte>>> entries;
		try {
			entries = FileStore.getEntries(fis, BatteryFields.class);
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
			Battery bat = new Battery();
			for (BatteryFields field : enumMap.keySet()) {
				/* bat = */field.parse(enumMap.get(field), bat);
			}
			data.add(bat);
		}
		return data;
	}

	public static <T extends Enum<T> & Fields<?, ?, ?>> void saveData(
			Context ctx, List<byte[]> listByteArrays)
			throws FileNotFoundException, IOException {
		Persist.saveData(ctx, FILE_PREFIX, listByteArrays);
	}

	@Override
	public String getFilename() {
		return FILE_PREFIX;
	}

	@Override
	public String toString() {
		return super.toString() + N + "Status" + IS + status;
	}

	@Override
	public String stringForm() {
		return time + N + "Status" + IS + status;
	}

	public static Battery fromBytes(List<byte[]> lb) throws ParserException {
		Battery battery = new Battery();
		int i = 0;
		for (BatteryFields bf : BatteryFields.values()) {
			bf.parse(listFromArray(lb.get(i++)), battery);
		}
		return battery;
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

	// =========================================================================
	// Accessors
	// =========================================================================
	public String getStatus() {
		return status;
	}
}
