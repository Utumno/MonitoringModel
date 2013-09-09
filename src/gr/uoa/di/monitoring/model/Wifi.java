package gr.uoa.di.monitoring.model;

import static gr.uoa.di.java.helpers.Utils.listToLong;
import static gr.uoa.di.java.helpers.Utils.listToString;
import gr.uoa.di.monitoring.android.persist.FileStore;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.EncodingUtils;

import android.net.wifi.ScanResult;

public final class Wifi {

	private long time;
	private List<Network> networks = new ArrayList<Wifi.Network>();

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

			@Override
			public <T, D> D parse(List<T> list, D objectToModify)
					throws ParserException {
				Wifi wi = (Wifi) objectToModify;
				try {
					// yeah when the Field has lists all List<T> are
					// List<List<Byte>>
					wi.time = listToLong(((List<List<Byte>>) list).get(0));
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				}
				return (D) wi;
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

			@Override
			public <T, D> D parse(List<T> list, D objectToModify)
					throws ParserException {
				final Wifi wi = (Wifi) objectToModify;
				final List<List<Byte>> doubleList = (List<List<Byte>>) list;
				final List<Network> nets = wi.networks;
				try {
					for (List<Byte> lb : doubleList) {
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
				return (D) wi;
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

			@Override
			public <T, D> D parse(List<T> list, D objectToModify)
					throws ParserException {
				final Wifi wi = (Wifi) objectToModify;
				final List<List<Byte>> doubleList = (List<List<Byte>>) list;
				final List<Network> nets = wi.networks;
				try {
					{
						int i = 0;
						for (List<Byte> lb : doubleList) {
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
				return (D) wi;
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

			@Override
			public <T, D> D parse(List<T> list, D objectToModify)
					throws ParserException {
				final Wifi wi = (Wifi) objectToModify;
				final List<List<Byte>> doubleList = (List<List<Byte>>) list;
				final List<Network> nets = wi.networks;
				try {
					{
						int i = 0;
						for (List<Byte> lb : doubleList) {
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
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				} catch (IndexOutOfBoundsException e) {
					throw new ParserException(
							"Malformed file : extra frequencies with no "
								+ "frequencies", e);
				}
				return (D) wi;
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

			@Override
			public <T, D> D parse(List<T> list, D objectToModify)
					throws ParserException {
				final Wifi wi = (Wifi) objectToModify;
				final List<List<Byte>> doubleList = (List<List<Byte>>) list;
				final List<Network> nets = wi.networks;
				try {
					{
						int i = 0;
						for (List<Byte> lb : doubleList) {
							int level = (int) listToLong(lb);
							Network n = nets.get(i++);
							n.level = level;
						}
						if (i < nets.size())
							throw new ParserException(
									"Malformed file : extra SSIDs with no "
										+ "level");
					}
				} catch (NumberFormatException e) {
					throw new ParserException("Malformed file", e);
				} catch (UnsupportedEncodingException e) {
					throw new ParserException("Malformed file", e);
				} catch (IndexOutOfBoundsException e) {
					throw new ParserException(
							"Malformed file : extra level with no SSIDs", e);
				}
				return (D) wi;
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

	public static class Network {

		private String ssid;
		private String bssid;
		private int frequency;
		private int level;

		public String getSsid() {
			return new String(ssid);
		}

		public String getBssid() {
			return new String(bssid);
		}

		public int getFrequency() {
			return frequency;
		}

		public int getLevel() {
			return level;
		}
	}

	public long getTime() {
		return time;
	}

	public List<Network> getNetworks() {
		return new ArrayList<Wifi.Network>(networks);
	}
}
