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

	public static enum BatteryFields implements Fields {
		TIME {

			@Override
			public <T> List<byte[]> getData(T data) {
				// TODO time()
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(System
						.currentTimeMillis() + ""));
				return arrayList;
			}

			@Override
			public <T, D> D parse(List<T> list, D objectToModify)
					throws ParserException {
				Battery bat = (Battery) objectToModify;
				try {
					bat.time = listToLong((List<Byte>) list);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
				return (D) bat;
			}
		},
		STATUS {

			@Override
			public <T> List<byte[]> getData(T data) {
				final Intent batteryStatus = (Intent) data;
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(batteryStatus
						.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) + ""));
				return arrayList;
			}

			@Override
			public <T, D> D parse(List<T> list, D objectToModify) {
				try {
					Battery bat = (Battery) objectToModify;
					bat.status = listToString((List<Byte>) list,
						FileStore.FILES_ENCODING);
					return (D) bat;
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
			}
		};

		@Override
		public boolean isList() {
			return false; // no lists here
		}

		public static <T> List<byte[]> createListOfByteArrays(T data) {
			final List<byte[]> listByteArrays = new ArrayList<byte[]>();
			for (BatteryFields bs : BatteryFields.values()) {
				if (!bs.isList()) listByteArrays.add(bs.getData(data).get(0));
			}
			return listByteArrays;
		}
	}

	// TODO move this into base class Data and make it abstract
	public List<Battery> parse(File f) throws IOException, ParserException {
		final FileInputStream fis = new FileInputStream(f);
		List<EnumMap<BatteryFields, Object>> entries = FileStore.getEntries(
			fis, BatteryFields.class);
		List<Battery> data = new ArrayList<Battery>();
		for (EnumMap<BatteryFields, Object> enumMap : entries) {
			Battery bat = new Battery();
			for (BatteryFields field : enumMap.keySet()) {
				field.parse((List<Byte>) enumMap.get(field), bat);
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
