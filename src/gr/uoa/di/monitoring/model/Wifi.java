package gr.uoa.di.monitoring.model;

import android.content.Context;
import android.net.wifi.ScanResult;

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
import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;

public final class Wifi extends Data {

	private List<Network> networks = new ArrayList<Wifi.Network>();
	private static final String FILE_PREFIX = "wifi";

	public Wifi(String imei) {
		super(imei);
	}

	public static enum WifiFields implements
			FileStore.Fields<List<ScanResult>, Wifi, List<List<Byte>>> {
		/*
		 * getData takes a List<ScanResult> and produces a List<byte[]>. For
		 * TIME the List<byte[]> gas a single element byte[] - the time in
		 * bytes. For the rest of Fields those are lists of byte[] and each
		 * byte[] is the respective network property for each network
		 */
		TIME(false) {

			@Override
			public List<byte[]> getData(List<ScanResult> scanRes) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				// NB : I just get the time of the method invocation
				arrayList.add(currentTime());
				return arrayList;
			}

			@Override
			public Wifi parse(List<List<Byte>> list, Wifi wi)
					throws ParserException {
				try {
					// yeah when the Field has lists all List<T> are
					// List<List<Byte>>
					wi.time = listToLong(list.get(0));
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				}
				return wi;
			}
		},
		SSID(true) {

			@Override
			public List<byte[]> getData(List<ScanResult> scanRes) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				if (scanRes != null) {
					for (ScanResult loc : scanRes) {
						arrayList.add(EncodingUtils.getAsciiBytes(loc.SSID));
					}
				}
				return arrayList;
			}

			@Override
			public Wifi parse(List<List<Byte>> list, Wifi wi)
					throws ParserException {
				final List<Network> nets = wi.networks;
				try {
					for (List<Byte> lb : list) {
						String ssid = listToString(lb, FileStore.FILES_ENCODING);
						// FIXME TODO networks first created here
						// TODO check what do I do with empty null SSIDs ?
						Network n = new Network();
						n.ssid = ssid;
						nets.add(n);
					}
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
				return wi;
			}
		},
		BSSID(true) {

			@Override
			public List<byte[]> getData(List<ScanResult> scanRes) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				if (scanRes != null) {
					for (ScanResult loc : scanRes) {
						arrayList.add(EncodingUtils.getAsciiBytes(loc.BSSID));
					}
				}
				return arrayList;
			}

			@Override
			public Wifi parse(List<List<Byte>> list, Wifi wi)
					throws ParserException {
				final List<Network> nets = wi.networks;
				try {
					{
						int i = 0;
						for (List<Byte> lb : list) {
							String bssid = listToString(lb,
								FileStore.FILES_ENCODING);
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
				return wi;
			}
		},
		FREQUENCY(true) {

			@Override
			public List<byte[]> getData(List<ScanResult> scanRes) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				if (scanRes != null) {
					for (ScanResult loc : scanRes) {
						arrayList.add(EncodingUtils.getAsciiBytes(loc.frequency
							+ ""));
					}
				}
				return arrayList;
			}

			@Override
			public Wifi parse(List<List<Byte>> list, Wifi wi)
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
				return wi;
			}
		},
		LEVEL(true) {

			@Override
			public List<byte[]> getData(List<ScanResult> scanRes) {
				List<byte[]> arrayList = new ArrayList<byte[]>();
				if (scanRes != null) {
					for (ScanResult loc : scanRes) {
						arrayList.add(EncodingUtils.getAsciiBytes(loc.level
							+ ""));
					}
				}
				return arrayList;
			}

			@Override
			public Wifi parse(List<List<Byte>> list, Wifi wi)
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
				return wi;
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

		/**
		 * This extracts from data the *only* the non list fields (currently
		 * TIME). TODO : unify with createListOfListsOfByteArrays
		 *
		 * @param data
		 *            the list of ScanResult
		 * @return a List<byte[]> which is essentially a one element list
		 *         containing the time as a byte[]
		 */
		public static List<byte[]>
				createListOfByteArrays(List<ScanResult> data) {
			final List<byte[]> listByteArrays = new ArrayList<byte[]>();
			for (WifiFields bs : WifiFields.values()) {
				if (!bs.isList()) listByteArrays.add(bs.getData(data).get(0));
			}
			return listByteArrays;
		}

		/**
		 * This extracts from data *only* the *list* fields (the properties of
		 * *each* network).
		 *
		 * @param data
		 *            the list of ScanResult
		 * @return a List<List<byte[]>> which is has as many elements as the
		 *         list Fields each of which has as many elements as the
		 *         networks scanned
		 */
		public static List<List<byte[]>> createListOfListsOfByteArrays(
				List<ScanResult> data) {
			final List<List<byte[]>> listofListsOfByteArrays = new ArrayList<List<byte[]>>();
			for (WifiFields bs : WifiFields.values()) {
				if (bs.isList()) listofListsOfByteArrays.add(bs.getData(data));
			}
			return listofListsOfByteArrays;
		}
	}

	// TODO move this into base class Data and make it abstract
	public static List<Wifi> parse(File f, String imei) throws IOException,
			ParserException {
		final FileInputStream fis = new FileInputStream(f);
		List<EnumMap<WifiFields, List<List<Byte>>>> entries;
		try {
			entries = FileStore.getEntries(fis, WifiFields.class);
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
			Wifi bat = new Wifi(imei);
			for (WifiFields field : enumMap.keySet()) {
				/* bat = */field.parse(enumMap.get(field), bat);
			}
			data.add(bat);
		}
		return data;
	}

	public static class Network {

		private String ssid;
		private String bssid;
		private int frequency;
		private int level;

		@Override
		public String toString() {
			return "Ssid=" + ssid + ", Bssid=" + bssid + ", Frequency="
				+ frequency + ", Level=" + level;
		}

		public String getSsid() {
			return ssid;
		}

		public String getBssid() {
			return bssid;
		}

		public int getFrequency() {
			return frequency;
		}

		public int getLevel() {
			return level;
		}
	}

	public static <T extends Enum<T> & Fields<?, ?, ?>> void saveData(
			Context ctx, List<byte[]> listByteArrays,
			List<List<byte[]>> listOfListsOfByteArrays, Class<T> fields)
			throws FileNotFoundException, IOException {
		Persist.saveData(ctx, FILE_PREFIX, listByteArrays,
			listOfListsOfByteArrays, fields);
	}

	@Override
	public String getFilename() {
		return FILE_PREFIX;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (Network net : networks) {
			sb.append(net).append(N);
		}
		return super.toString() + N + sb.toString();
	}

	public static Wifi fromBytes(List<byte[]> lb, List<List<byte[]>> llb)
			throws ParserException {
		Wifi wifi = new Wifi("");
		int nextArray = 0;
		int nextListOfArrays = 0;
		for (WifiFields bf : WifiFields.values()) {
			if (!bf.isList()) {
				final ArrayList<List<Byte>> al = new ArrayList<List<Byte>>();
				al.add(listFromArray(lb.get(nextArray++)));
				bf.parse(al, wifi);
			} else {
				final ArrayList<List<Byte>> al = new ArrayList<List<Byte>>();
				final List<byte[]> list = llb.get(nextListOfArrays++);
				for (byte[] bs : list) {
					al.add(listFromArray(bs));
				}
				bf.parse(al, wifi);
			}
		}
		return wifi;
	}

	// =========================================================================
	// Accessors
	// =========================================================================
	public List<Network> getNetworks() {
		return new ArrayList<Wifi.Network>(networks);
	}
}
