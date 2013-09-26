package gr.uoa.di.java.helpers;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class Utils {

	private Utils() {}

	// Standard charsets' NAMES
	// see http://stackoverflow.com/questions/1684040/ for a discussion
	// in Java 7 there is a StandardCharsets class :
	// docs.oracle.com/javase/7/docs/api/java/nio/charset/StandardCharsets.html
	// ---------------------------------------------------------------------- //
	/**
	 * Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the
	 * Unicode character set
	 */
	public static final String ASCII = "US-ASCII";
	/** ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1 */
	public static final String ISO8859 = "ISO-8859-1";
	/** Eight-bit UCS Transformation Format */
	public static final String UTF8 = "UTF-8";
	/** Sixteen-bit UCS Transformation Format, big-endian byte order */
	public static final String UTF16BE = "UTF-16BE";
	/** Sixteen-bit UCS Transformation Format, little-endian byte order */
	public static final String UTF16LE = "UTF-16LE";
	/**
	 * Sixteen-bit UCS Transformation Format, byte order identified by an
	 * optional byte-order mark
	 */
	public static final String UTF16 = "UTF-16";

	public static String listToString(List<Byte> lb, String charsetName)
			throws UnsupportedEncodingException, NumberFormatException {
		// see http://stackoverflow.com/questions/1096868/
		// TODO better tests
		if (lb == null)
			throw new NullPointerException("List<Byte> can't be null");
		byte[] array = new byte[lb.size()];
		{
			int i = 0;
			for (byte current : lb) {
				array[i] = current;
				++i;
			}
		}
		return new String(array, charsetName);
	}

	public static long listToLong(List<Byte> lb)
			throws UnsupportedEncodingException, NumberFormatException {
		// TODO better tests
		if (lb == null)
			throw new NullPointerException("List<Byte> can't be null");
		return new BigDecimal(listToString(lb, ASCII)).longValue();
	}

	/**
	 * If list is empty 0 is returned. If List is null is thrown
	 *
	 * @param lb
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws NullPointerException
	 *             if ld is null
	 */
	public static double listToDouble(List<Byte> lb)
			throws UnsupportedEncodingException, NumberFormatException {
		// TODO better tests
		if (lb == null)
			throw new NullPointerException("List<Byte> can't be null");
		return new BigDecimal(listToString(lb, ASCII)).doubleValue();
	}

	public static List<Byte> listFromArray(byte[] ba) {
		if (ba == null) throw new NullPointerException("byte[] can't be null");
		final List<Byte> lb = new ArrayList<Byte>();
		for (byte b : ba) {
			lb.add(b);
		}
		return lb;
	}
	// public static <T> T[] concat(T[] first, T[] second) {
	// T[] result = Arrays.copyOf(first, first.length + second.length); // NOT
	// // fucki9ng available
	// System.arraycopy(second, 0, result, first.length, second.length);
	// return result;
	// }
}
