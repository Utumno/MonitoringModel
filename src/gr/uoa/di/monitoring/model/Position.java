package gr.uoa.di.monitoring.model;

import android.content.Context;
import android.location.Location;

import gr.uoa.di.monitoring.android.persist.FileStore;
import gr.uoa.di.monitoring.android.persist.ParserException;
import gr.uoa.di.monitoring.android.persist.Persist;
import gr.uoa.di.monitoring.android.persist.Store;

import org.apache.http.util.EncodingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static gr.uoa.di.java.helpers.Utils.listToDouble;
import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;

public final class Position extends Data {

	double latitude;
	double longitude;
	String provider;
	private static final String FILE_PREFIX = "loc";

	public static enum LocationFields implements
			Fields<Location, Position, List<Byte>> {
		TIME {

			@Override
			public List<byte[]> getData(Location loc, final Position out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				final long t = loc.getTime();
				out.time = t;
				arrayList.add(EncodingUtils.getAsciiBytes(t + ""));
				return arrayList;
			}

			@Override
			public void parse(List<Byte> list, final Position pos)
					throws ParserException {
				try {
					pos.time = listToLong(list);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				}
			}
		},
		LAT {

			@Override
			public List<byte[]> getData(Location loc, final Position out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				final double lat = loc.getLatitude();
				out.latitude = lat;
				arrayList.add(EncodingUtils.getAsciiBytes(lat + ""));
				return arrayList;
			}

			@Override
			public void parse(List<Byte> list, final Position pos)
					throws ParserException {
				try {
					pos.latitude = listToDouble(list);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				}
			}
		},
		LONG {

			@Override
			public List<byte[]> getData(Location loc, final Position out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				final double lon = loc.getLongitude();
				out.longitude = lon;
				arrayList.add(EncodingUtils.getAsciiBytes(lon + ""));
				return arrayList;
			}

			@Override
			public void parse(List<Byte> list, final Position pos)
					throws ParserException {
				try {
					pos.longitude = listToDouble(list);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				}
			}
		},
		PROVIDER {

			@Override
			public List<byte[]> getData(Location loc, final Position out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				final String prov = loc.getProvider();
				out.provider = prov;
				arrayList.add(EncodingUtils.getAsciiBytes(prov + ""));
				return arrayList;
			}

			@Override
			public void parse(List<Byte> list, final Position pos)
					throws ParserException {
				try {
					pos.provider = listToString(list, Store.FILES_ENCODING);
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
			}
		};

		@Override
		public boolean isList() {
			return false; // no lists here
		}
	}

	// =========================================================================
	// Static API
	// =========================================================================
	public static List<Position> parse(File f) throws IOException,
			ParserException {
		final File file = new File(f, FILE_PREFIX);
		final FileInputStream fis = new FileInputStream(file);
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
			Position bat = new Position();
			for (LocationFields field : enumMap.keySet()) {
				field.parse(enumMap.get(field), bat);
			}
			data.add(bat);
		}
		return data;
	}

	/**
	 * Constructs a Position instance from the given string. Only the fields
	 * that matter to {@link #fairlyEqual(Data)} are filled (and time for
	 * debugging purposes)
	 */
	public static Position fromString(String s) {
		if (s == null || s.trim().equals("")) return null;
		final Position p = new Position();
		String[] split = s.split(N);
		p.time = Long.valueOf(split[0]);
		int i = 0;
		p.longitude = Double.valueOf(split[++i].split(IS)[1].trim());
		p.latitude = Double.valueOf(split[++i].split(IS)[1].trim());
		p.provider = split[++i].split(IS)[1].trim();
		return p;
	}

	public static <T extends Enum<T> & Fields<?, ?, ?>> Position saveData(
			Context ctx, Location data) throws IOException {
		final Position out = new Position();
		final List<byte[]> listByteArrays = createListOfByteArrays(data, out);
		Persist.saveData(ctx, FILE_PREFIX, listByteArrays);
		return out;
	}

	private static List<byte[]> createListOfByteArrays(Location data,
			final Position out) {
		if (out == null)
			throw new NullPointerException("out parameter can't be null");
		final List<byte[]> listByteArrays = new ArrayList<byte[]>();
		for (LocationFields bs : LocationFields.values()) {
			listByteArrays.add(bs.getData(data, out).get(0));
		}
		return listByteArrays;
	}

	// =========================================================================
	// API
	// =========================================================================
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
}
