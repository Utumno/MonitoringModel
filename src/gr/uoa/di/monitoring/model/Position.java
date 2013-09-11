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

	public static enum LocationFields implements
			FileStore.Fields<Location, Position, Byte> {
		TIME {

			@Override
			public List<byte[]> getData(Location loc) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(loc.getTime() + ""));
				return arrayList;
			}

			@Override
			public Position parse(List<Byte> list, Position pos)
					throws ParserException {
				try {
					pos.time = listToLong(list);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
				return pos;
			}
		},
		LAT {

			@Override
			public List<byte[]> getData(Location loc) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(loc.getLatitude()
					+ ""));
				return arrayList;
			}

			@Override
			public Position parse(List<Byte> list, Position pos)
					throws ParserException {
				try {
					pos.latitude = listToDouble(list);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
				return pos;
			}
		},
		LONG {

			@Override
			public List<byte[]> getData(Location loc) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(loc.getLongitude()
					+ ""));
				return arrayList;
			}

			@Override
			public Position parse(List<Byte> list, Position pos)
					throws ParserException {
				try {
					pos.longitude = listToDouble(list);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
				return pos;
			}
		},
		PROVIDER {

			@Override
			public List<byte[]> getData(Location loc) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				arrayList.add(EncodingUtils.getAsciiBytes(loc.getProvider()
					+ ""));
				return arrayList;
			}

			@Override
			public Position parse(List<Byte> list, Position pos)
					throws ParserException {
				try {
					pos.provider = listToString(list, FileStore.FILES_ENCODING);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
				return pos;
			}
		};

		@Override
		public boolean isList() {
			return false; // no lists here
		}

		public static List<byte[]> createListOfByteArrays(Location data) {
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
