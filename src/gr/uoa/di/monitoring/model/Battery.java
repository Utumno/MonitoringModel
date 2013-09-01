package gr.uoa.di.monitoring.model;

import gr.uoa.di.monitoring.android.persist.FileStore.Fields;

import java.util.ArrayList;
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

	public long getTime() {
		return time;
	}

	public String getStatus() {
		return new String(status);
	}
}
