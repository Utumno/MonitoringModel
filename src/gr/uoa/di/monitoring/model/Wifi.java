package gr.uoa.di.monitoring.model;

import gr.uoa.di.monitoring.android.persist.FileStore;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.EncodingUtils;

import android.net.wifi.ScanResult;

public final class Wifi {

	private long time;
	private String ssid;

	public static enum WifiFields implements FileStore.Fields {
		TIME(false) {

			@Override
			public <T> List<byte[]> getData(T data) {
				// TODO time()
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(System
						.currentTimeMillis() + ""));
				return arrayList;
			}
		},
		SSID(true) {

			@Override
			public <T> List<byte[]> getData(T data) {
				List<ScanResult> scanRes = (List<ScanResult>) data;
				List<byte[]> arrayList = new ArrayList<byte[]>();
				if (scanRes != null) {
					for (ScanResult loc : scanRes) {
						arrayList.add(EncodingUtils.getAsciiBytes(loc.SSID));
					}
				}
				return arrayList;
			}
		},
		BSSID(true) {

			@Override
			public <T> List<byte[]> getData(T data) {
				List<ScanResult> scanRes = (List<ScanResult>) data;
				List<byte[]> arrayList = new ArrayList<byte[]>();
				if (scanRes != null) {
					for (ScanResult loc : scanRes) {
						arrayList.add(EncodingUtils.getAsciiBytes(loc.BSSID));
					}
				}
				return arrayList;
			}
		},
		FREQUENCY(true) {

			@Override
			public <T> List<byte[]> getData(T data) {
				List<ScanResult> scanRes = (List<ScanResult>) data;
				List<byte[]> arrayList = new ArrayList<byte[]>();
				if (scanRes != null) {
					for (ScanResult loc : scanRes) {
						arrayList.add(EncodingUtils.getAsciiBytes(loc.frequency
							+ ""));
					}
				}
				return arrayList;
			}
		},
		LEVEL(true) {

			@Override
			public <T> List<byte[]> getData(T data) {
				List<ScanResult> scanRes = (List<ScanResult>) data;
				List<byte[]> arrayList = new ArrayList<byte[]>();
				if (scanRes != null) {
					for (ScanResult loc : scanRes) {
						arrayList.add(EncodingUtils.getAsciiBytes(loc.level
							+ ""));
					}
				}
				return arrayList;
			}
		};

		private boolean isList;

		private WifiFields(boolean isList) {
			this.isList = isList;
		}

		@Override
		public boolean isList() {
			return isList;
		}

		public static <T> List<byte[]> createListOfByteArrays(T data) {
			final List<byte[]> listByteArrays = new ArrayList<byte[]>();
			for (WifiFields bs : WifiFields.values()) {
				if (!bs.isList()) listByteArrays.add(bs.getData(data).get(0));
			}
			return listByteArrays;
		}

		public static <T> List<List<byte[]>> createListOfListsOfByteArrays(
				T data) {
			final List<List<byte[]>> listofListsOfByteArrays = new ArrayList<List<byte[]>>();
			for (WifiFields bs : WifiFields.values()) {
				if (bs.isList()) listofListsOfByteArrays.add(bs.getData(data));
			}
			return listofListsOfByteArrays;
		}
	}
}
