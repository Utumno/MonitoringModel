package gr.uoa.di.monitoring.model;

import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import gr.uoa.di.monitoring.android.persist.FileStore;
import gr.uoa.di.monitoring.android.persist.FileStore.Fields;

import org.apache.http.util.EncodingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public final class Battery extends Data {

	public Battery(String imei) {
		super(imei);
	}

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
	public static List<Battery> parse(File f, String imei) throws IOException,
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
			Battery bat = new Battery(imei);
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
		FileStore.saveData(ctx, FILE_PREFIX, listByteArrays);
	}

	@Override
	public String getFilename() {
		return FILE_PREFIX;
	}

	@Override
	public String toString() {
		return super.toString() + N + "Status : " + status;
	}

	// =========================================================================
	// Accessors
	// =========================================================================
	public String getStatus() {
		return status;
	}
}
