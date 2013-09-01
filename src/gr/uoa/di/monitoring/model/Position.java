package gr.uoa.di.monitoring.model;

import gr.uoa.di.monitoring.android.persist.FileStore;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.EncodingUtils;

import android.location.Location;

public final class Position {

	private long time;
	private double latitude;
	private double longitude;
	private String provider;

	public static enum LocationFields implements FileStore.Fields {
		TIME {

			@Override
			public <T> List<byte[]> getData(T data) {
				Location loc = (Location) data;
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(loc.getTime() + ""));
				return arrayList;
			}
		},
		LAT {

			@Override
			public <T> List<byte[]> getData(T data) {
				Location loc = (Location) data;
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(loc.getLatitude()
					+ ""));
				return arrayList;
			}
		},
		LONG {

			@Override
			public <T> List<byte[]> getData(T data) {
				Location loc = (Location) data;
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(loc.getLongitude()
					+ ""));
				return arrayList;
			}
		},
		PROVIDER {

			@Override
			public <T> List<byte[]> getData(T data) {
				Location loc = (Location) data;
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(loc.getProvider()
					+ ""));
				return arrayList;
			}
		};

		@Override
		public boolean isList() {
			return false; // no lists here
		}

		public static <T> List<byte[]> createListOfByteArrays(T data) {
			final List<byte[]> listByteArrays = new ArrayList<byte[]>();
			for (LocationFields bs : LocationFields.values()) {
				listByteArrays.add(bs.getData(data).get(0));
			}
			return listByteArrays;
		}
	}
}
