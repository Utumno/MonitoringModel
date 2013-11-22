package gr.uoa.di.monitoring.model;

import android.content.Context;
import android.location.Location;

import gr.uoa.di.monitoring.android.persist.FileStore;
import gr.uoa.di.monitoring.android.persist.FileStore.Fields;
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
import static gr.uoa.di.java.helpers.Utils.listToDouble;
import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;

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
		Persist.saveData(ctx, FILE_PREFIX, listByteArrays);
	}

	// TODO move this into base class Data and make it abstract
	public static List<Position> parse(File f, String imei) throws IOException,
			ParserException {
		final FileInputStream fis = new FileInputStream(f);
		List<EnumMap<LocationFields, List<Byte>>> entries;
		try {
			entries = FileStore.getEntries(fis, LocationFields.class);
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				// could not close the file ?
				e.printStackTrace();
			}
		}
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
		return super.toString() + N + "Longitude" + IS + longitude + N
			+ "Latitude" + IS + latitude + N + "Provider" + IS + provider;
	}

	@Override
	public String stringForm() {
		return time + N + "Longitude" + IS + longitude + N + "Latitude" + IS
			+ latitude + N + "Provider" + IS + provider;
	}

	public static Position fromBytes(List<byte[]> lb) throws ParserException {
		Position battery = new Position("");
		int i = 0;
		for (LocationFields bf : LocationFields.values()) {
			bf.parse(listFromArray(lb.get(i++)), battery);
		}
		return battery;
	}

	/**
	 * Two Position instances are fairlyEqual if they have the same longitude,
	 * latitude and provider. FIXME : compare into some meters accuracy
	 *
	 * @throws NullPointerException
	 *             if d.provider == null
	 */
	@Override
	public boolean fairlyEqual(Data d) {
		if (d == null || !(d instanceof Position)) return false;
		final Position p = (Position) d;
		return p.latitude == this.latitude && p.longitude == this.longitude
			&& p.provider.equals(this.provider);
	}

	/**
	 * Constructs a Position instance from the given string. Only the fields
	 * that matter to {@link #fairlyEqual(Data)} are filled (and time for
	 * debugging purposes)
	 */
	public static Position fromString(String s) {
		if (s == null || s.trim().equals("")) return null;
		final Position p = new Position("");
		String[] split = s.split(N);
		p.time = Long.valueOf(split[0]);
		int i = 0;
		p.longitude = Double.valueOf(split[++i].split(IS)[1].trim());
		p.latitude = Double.valueOf(split[++i].split(IS)[1].trim());
		p.provider = split[++i].split(IS)[1].trim();
		return p;
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
