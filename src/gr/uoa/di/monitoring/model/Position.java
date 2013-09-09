package gr.uoa.di.monitoring.model;

import static gr.uoa.di.java.helpers.Utils.listToDouble;
import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;
import gr.uoa.di.monitoring.android.persist.FileStore;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.EncodingUtils;

import android.location.Location;

public final class Position /* TODO extends Data */{

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

			@Override
			public <T, D> D parse(List<T> list, D objectToModify) {
				Position pos = (Position) objectToModify;
				try {
					pos.time = listToLong((List<Byte>) list);
				} catch (NumberFormatException e) {
					// TODO parser exception
					throw new IllegalStateException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					// TODO parser exception
					throw new IllegalStateException("Malformed file", e);
				}
				return (D) pos;
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

			@Override
			public <T, D> D parse(List<T> list, D objectToModify) {
				Position pos = (Position) objectToModify;
				try {
					pos.latitude = listToDouble((List<Byte>) list);
				} catch (NumberFormatException e) {
					// TODO parser exception
					throw new IllegalStateException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					// TODO parser exception
					throw new IllegalStateException("Malformed file", e);
				}
				return (D) pos;
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

			@Override
			public <T, D> D parse(List<T> list, D objectToModify) {
				Position pos = (Position) objectToModify;
				try {
					pos.longitude = listToDouble((List<Byte>) list);
				} catch (NumberFormatException e) {
					// TODO parser exception
					throw new IllegalStateException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					// TODO parser exception
					throw new IllegalStateException("Malformed file", e);
				}
				return (D) pos;
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

			@Override
			public <T, D> D parse(List<T> list, D objectToModify) {
				Position pos = (Position) objectToModify;
				try {
					pos.provider = listToString((List<Byte>) list,
						FileStore.FILES_ENCODING);
				} catch (NumberFormatException e) {
					// TODO parser exception
					throw new IllegalStateException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					// TODO parser exception
					throw new IllegalStateException("Malformed file", e);
				}
				return (D) pos;
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

	public long getTime() {
		return time;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getProvider() {
		return new String(provider);
	}
}
