package gr.uoa.di.monitoring.model;

import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;
import gr.uoa.di.monitoring.android.persist.FileStore;
import gr.uoa.di.monitoring.android.persist.FileStore.Fields;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.apache.http.util.EncodingUtils;

import android.content.Intent;
import android.os.BatteryManager;

public final class Battery {

	private long time;
	private String status;

	public static String hallo() {
		return "Hello server";
	}

	public static enum BatteryFields implements
			Fields<Intent, Battery, List<Byte>> {
		TIME {

			@Override
			public List<byte[]> getData(Intent data) {
				// TODO time()
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(System
						.currentTimeMillis() + ""));
				return arrayList;
			}

			@Override
			public Battery parse(List<Byte> list, Battery bat)
					throws ParserException {
				try {
					bat.time = listToLong(list);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
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
		List<EnumMap<BatteryFields, List<Byte>>> entries = FileStore
				.getEntries(fis, BatteryFields.class);
		final List<Battery> data = new ArrayList<Battery>();
		for (EnumMap<BatteryFields, List<Byte>> enumMap : entries) {
			Battery bat = new Battery();
			for (BatteryFields field : enumMap.keySet()) {
				field.parse(enumMap.get(field), bat);
			}
		}
		return data;
	}

	public long getTime() {
		return time;
	}

	public String getStatus() {
		return new String(status);
	}
}
