package gr.uoa.di.monitoring.model;

import android.content.Context;
import android.net.wifi.ScanResult;

import gr.uoa.di.monitoring.android.files.Parser;
import gr.uoa.di.monitoring.android.files.ParserException;
import gr.uoa.di.monitoring.android.files.Persist;
import gr.uoa.di.monitoring.android.files.Store;

import org.apache.http.util.EncodingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;

public final class Wifi extends Data {

	final List<Network> networks = new ArrayList<Wifi.Network>();
	private static final String FILE_PREFIX = "wifi";

	private Wifi() {}

	private enum WifiFields implements
			Fields<List<ScanResult>, Wifi, List<List<Byte>>> {
		/*
		 * getData takes a List<ScanResult> and produces a List<byte[]>. For
		 * TIME the List<byte[]> has a single element byte[] - the time in
		 * bytes. For the rest of Fields those are lists of byte[] and each
		 * byte[] is the respective network property for each network
		 */
		TIME(false) {

			@Override
			public List<byte[]>
					getData(List<ScanResult> scanRes, final Wifi out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				// NB : I just get the time of the method invocation
				final long currentTimeMillis = System.currentTimeMillis();
				out.time = currentTimeMillis;
				arrayList.add(currentTime(currentTimeMillis));
				return arrayList;
			}

			@Override
			public void parse(List<List<Byte>> list, final Wifi wi)
					throws ParserException {
				try {
					// yeah when the Field has lists all List<T> are
					// List<List<Byte>>
					wi.time = listToLong(list.get(0));
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				}
			}
		},
		SSID(true) {

			@Override
			public List<byte[]>
					getData(List<ScanResult> scanRes, final Wifi out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				final List<Network> nets = out.networks;
				if (scanRes != null) {
					for (ScanResult loc : scanRes) {
						// ISSUE 6 - must get the SSID from the service and
						// check for UTF and non printable characters. If any
						// found I must replace it with a non present char and
						// add this info on the string byte[]. Also check what
						// do I do with empty null SSIDs and hidden ones
						final String ssid = loc.SSID;
						// wi.networks FIRST POPULATED HERE !!!!!!!!!!!!!!!!!!!!
						Network n = new Network();
						n.ssid = ssid;
						nets.add(n);
						arrayList.add(EncodingUtils.getAsciiBytes(ssid));
					}
				}
				return arrayList;
			}

			@Override
			public void parse(List<List<Byte>> list, final Wifi wi)
					throws ParserException {
				// wi.networks FIRST POPULATED HERE !!!!!!!!!!!!!!!!!!!!!!!!!!!
				final List<Network> nets = wi.networks;
				try {
					for (List<Byte> lb : list) {
						String ssid = listToString(lb, Store.FILES_ENCODING);
						// ISSUE 6 - here I must parse the extra info to see if
						// the SSID I got is unicode or has "exotic" (non
						// printable) characters
						Network n = new Network();
						n.ssid = ssid;
						nets.add(n);
					}
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
			}
		},
		BSSID(true) {

			@Override
			public List<byte[]>
					getData(List<ScanResult> scanRes, final Wifi out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				final List<Network> nets = out.networks;
				if (scanRes != null) {
					int i = 0;
					for (ScanResult loc : scanRes) {
						final String bssid = loc.BSSID;
						nets.get(i++).bssid = bssid;
						arrayList.add(EncodingUtils.getAsciiBytes(bssid));
					}
				}
				return arrayList;
			}

			@Override
			public void parse(List<List<Byte>> list, final Wifi wi)
					throws ParserException {
				final List<Network> nets = wi.networks;
				try {
					{
						int i = 0;
						for (List<Byte> lb : list) {
							String bssid = listToString(lb,
								Store.FILES_ENCODING);
							Network n = nets.get(i++);
							n.bssid = bssid;
						}
						if (i < nets.size())
							throw new ParserException(
								"Malformed file : extra SSIDs with no BSSID");
					}
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file : missing ", e);
				} catch (IndexOutOfBoundsException e) {
					throw new ParserException(
						"Malformed file : extra BSSIDs with no SSID", e);
				}
			}
		},
		FREQUENCY(true) {

			@Override
			public List<byte[]>
					getData(List<ScanResult> scanRes, final Wifi out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				final List<Network> nets = out.networks;
				if (scanRes != null) {
					int i = 0;
					for (ScanResult loc : scanRes) {
						final int freq = loc.frequency;
						nets.get(i++).frequency = freq;
						arrayList.add(EncodingUtils.getAsciiBytes(freq + ""));
					}
				}
				return arrayList;
			}

			@Override
			public void parse(List<List<Byte>> list, final Wifi wi)
					throws ParserException {
				final List<Network> nets = wi.networks;
				try {
					{
						int i = 0;
						for (List<Byte> lb : list) {
							int freq = (int) listToLong(lb);
							Network n = nets.get(i++);
							n.frequency = freq;
						}
						if (i < nets.size())
							throw new ParserException(
								"Malformed file : extra SSIDs with no "
									+ "frequencies");
					}
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (IndexOutOfBoundsException e) {
					throw new ParserException(
						"Malformed file : extra frequencies with no "
							+ "frequencies", e);
				}
			}
		},
		LEVEL(true) {

			@Override
			public List<byte[]>
					getData(List<ScanResult> scanRes, final Wifi out) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				final List<Network> nets = out.networks;
				if (scanRes != null) {
					int i = 0;
					for (ScanResult loc : scanRes) {
						final int lev = loc.level;
						nets.get(i++).level = lev;
						arrayList.add(EncodingUtils.getAsciiBytes(lev + ""));
					}
				}
				return arrayList;
			}

			@Override
			public void parse(List<List<Byte>> list, final Wifi wi)
					throws ParserException {
				final List<Network> nets = wi.networks;
				try {
					{
						int i = 0;
						for (List<Byte> lb : list) {
							int level = (int) listToLong(lb);
							Network n = nets.get(i++);
							n.level = level;
						}
						if (i < nets.size())
							throw new ParserException(
								"Malformed file : extra SSIDs with no level");
					}
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (IndexOutOfBoundsException e) {
					throw new ParserException(
						"Malformed file : extra level with no SSIDs", e);
				}
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
	}

	private static class Network {

		String ssid;
		String bssid;
		int frequency;
		int level;
		private static final String SEP = ", ";

		Network() {}

		@Override
		public String toString() {
			return "Ssid" + IS + ssid + SEP + "Bssid" + IS + bssid + SEP
				+ "Frequency" + IS + frequency + SEP + "Level" + IS + level;
		}

		/**
		 * Returns true if the given net has the same ssid and bssid as this
		 *
		 * @throws NullPointerException
		 *             if net.ssid or net.bssid == null
		 */
		boolean fairlyEqual(final Network net) {
			if (net == null) return false;
			return net.ssid.equals(this.ssid) && net.bssid.equals(this.bssid);
		}

		static Network fromString(String string) {
			final Network network = new Network();
			String[] split = string.split(SEP);
			network.ssid = split[0].split(IS)[1].trim();
			network.bssid = split[1].split(IS)[1].trim();
			return network;
		}
	}

	// =========================================================================
	// Static API
	// =========================================================================
	public static List<Wifi> parse(File f) throws IOException, ParserException {
		final File file = new File(f, FILE_PREFIX);
		final FileInputStream fis = new FileInputStream(file);
		List<EnumMap<WifiFields, List<List<Byte>>>> entries;
		try {
			entries = Parser.getEntries(fis, WifiFields.class);
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				// could not close the file ?
				e.printStackTrace();
			}
		}
		final List<Wifi> data = new ArrayList<Wifi>();
		for (EnumMap<WifiFields, List<List<Byte>>> enumMap : entries) {
			Wifi bat = new Wifi();
			for (WifiFields field : enumMap.keySet()) {
				field.parse(enumMap.get(field), bat);
			}
			data.add(bat);
		}
		return data;
	}

	/**
	 * Constructs a Wifi instance from the given string. Only the fields that
	 * matter to {@link #fairlyEqual(Data)} are filled (and time for debugging
	 * purposes)
	 */
	public static Wifi fromString(String s) {
		if (s == null || s.trim().equals("")) return null;
		final Wifi p = new Wifi();
		String[] split = s.split(N);
		p.time = Long.valueOf(split[0]);
		for (int i = 1; i < split.length; ++i) {
			p.networks.add(Wifi.Network.fromString(split[i]));
		}
		return p;
	}

	public static Wifi saveData(Context ctx, List<ScanResult> data)
			throws IOException {
		final Wifi out = new Wifi();
		List<List<byte[]>> listOfListsOfByteArrays = createListOfListsOfByteArrays(
			data, out);
		Persist.saveData(ctx, FILE_PREFIX, listOfListsOfByteArrays,
			WifiFields.class);
		return out;
	}

	/**
	 * Extracts from data given by android the bytes representing the data we
	 * are interested in. The returned List<List<byte[]>> has as many elements
	 * as the Fields. The elements of this List for the *list* fields - those
	 * fields that isList() returns true (the properties for *each* network) -
	 * are List<byte[]> which have as many elements as the available networks -
	 * one byte[] for each. The non list fields (currently TIME) are
	 * List<byte[]> with a single byte[] element (containing the time as a
	 * byte[] in our case)
	 *
	 * @param data
	 *            the list of ScanResult
	 * @return a List<List<byte[]>> which has as many elements as the Fields
	 *         each of which has as many elements as the networks scanned for
	 *         the isList() fields or a single element otherwise
	 */
	private static List<List<byte[]>> createListOfListsOfByteArrays(
			List<ScanResult> data, final Wifi out) {
		if (out == null)
			throw new NullPointerException("out parameter can't be null");
		final List<List<byte[]>> listofListsOfByteArrays = new ArrayList<List<byte[]>>();
		for (WifiFields bs : WifiFields.values()) {
			listofListsOfByteArrays.add(bs.getData(data, out));
		}
		return listofListsOfByteArrays;
	}

	// =========================================================================
	// API
	// =========================================================================
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (Network net : networks) {
			sb.append(net).append(N);
		}
		return super.toString() + N + sb.toString();
	}

	@Override
	public String stringForm() {
		final StringBuilder sb = new StringBuilder();
		for (Network net : networks) {
			sb.append(net).append(N);
		}
		return time + N + sb.toString();
	}

	/**
	 * Two Wifi instances are fairlyEqual if they contain the same number of
	 * fairly equal (ssid and bssid) networks
	 *
	 * @throws NullPointerException
	 *             if d.networks == null or if any of the w.networks has null
	 *             ssid or bssid
	 */
	@Override
	public boolean fairlyEqual(final Data d) {
		if (d == null || !(d instanceof Wifi)) return false;
		final Wifi w = (Wifi) d;
		if (w.networks.size() != this.networks.size()) return false;
		main: for (Network net : w.networks) {
			for (Network thisNet : this.networks) {
				if (thisNet.fairlyEqual(net)) continue main;
			}
			return false;
		}
		return true;
	}
}
