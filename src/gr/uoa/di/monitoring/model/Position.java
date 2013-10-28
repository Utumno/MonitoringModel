package gr.uoa.di.monitoring.model;

import static gr.uoa.di.java.helpers.Utils.listToDouble;
import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;

import android.content.Context;
import android.location.Location;

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

public final class Position extends Data {

	private double latitude;
	private double longitude;
	private String provider;
	private static final String FILE_PREFIX = "loc";

	public Position(String imei) {
		super(imei);
	}

	public static enum LocationFields implements
			FileStore.Fields<Location, Position, List<Byte>> {
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

	public static <T extends Enum<T> & Fields<?, ?, ?>> void saveData(
			Context ctx, List<byte[]> listByteArrays)
			throws FileNotFoundException, IOException {
		FileStore.saveData(ctx, FILE_PREFIX, listByteArrays);
	}

	// TODO move this into base class Data and make it abstract
	public static List<Position> parse(File f, String imei) throws IOException,
			ParserException {
		final FileInputStream fis = new FileInputStream(f);
		List<EnumMap<LocationFields, List<Byte>>> entries = FileStore
			.getEntries(fis, LocationFields.class);
		final List<Position> data = new ArrayList<Position>();
		for (EnumMap<LocationFields, List<Byte>> enumMap : entries) {
			Position bat = new Position(imei);
			for (LocationFields field : enumMap.keySet()) {
				/* bat = */field.parse(enumMap.get(field), bat);
			}
			data.add(bat);
		}
		return data;
	}

	@Override
	public String getFilename() {
		return FILE_PREFIX;
	}

	@Override
	public String toString() {
		return super.toString() + N + "Longitude : " + longitude + N
			+ "Latitude : " + latitude + N + "Provider : " + provider;
	}

	// =========================================================================
	// Accessors
	// =========================================================================
	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getProvider() {
		return provider;
	}
}
