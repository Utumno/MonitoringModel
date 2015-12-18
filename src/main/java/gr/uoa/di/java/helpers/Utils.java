package gr.uoa.di.java.helpers;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
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

	/**
	 * Returns the String that the given List of Bytes represents in the given
	 * charset. If an empty list is given the empty string is returned
	 * immediately. Otherwise the Bytes are decoded using the charset specified
	 * by its name. Note that if the Bytes given are not valid in the given
	 * charset the behavior of this method is unspecified (due to the
	 * unspecified behavior of the {@link String#String(byte[], String)}
	 * constructor).
	 *
	 * @param lb
	 *            a list of Bytes
	 * @param charsetName
	 *            the charset name those bytes will be decoded with
	 * @return a sting constructed from the bytes in lb
	 * @throws UnsupportedEncodingException
	 *             if the charset given is unsupported (except if an empty list
	 *             is given where the charset is not used)
	 * @throws NullPointerException
	 *             if lb is null
	 */
	public static String listToString(List<Byte> lb, String charsetName)
			throws UnsupportedEncodingException {
		// see http://stackoverflow.com/questions/1096868/
		if (lb == null)
			throw new NullPointerException("List<Byte> can't be null");
		if (lb.isEmpty()) return "";
		final byte[] array = new byte[lb.size()];
		{
			int i = 0;
			for (byte current : lb) {
				array[i++] = current;
			}
		}
		return new String(array, charsetName); // The behavior of this
		// constructor when the given bytes are not valid in the given charset
		// is unspecified -- !!!!
	}

	/**
	 * Returns a long whose ASCII representation are the bytes in the given
	 * list. If lb is null NullPointerException is thrown. If lb is empty
	 * NumberFormatException is thrown. Otherwise lb must contain the ASCII
	 * representation of the digits of a long number. If other characters are
	 * contained a {@link NumberFormatException} is thrown. If characters
	 * outside the ASCII range are contained the
	 * {@link String#String(byte[], String)} constructor used internally will
	 * result in unspecified behavior which in turn will lead to a
	 * NumberFormatException (hopefully). Careful - the l/L long suffix must not
	 * be included in the bytes - will result in NumberFormatException
	 *
	 * @param lb
	 *            the list of bytes that represent the characters representing a
	 *            double
	 * @return
	 * @throws NumberFormatException
	 *             if the bytes in lb are converted to a string that can't be
	 *             converted to a double
	 * @throws NullPointerException
	 *             if lb is null
	 */
	public static long listToLong(List<Byte> lb) throws NumberFormatException {
		if (lb == null)
			throw new NullPointerException("List<Byte> can't be null");
		try {
			return new BigInteger(listToString(lb, ASCII)).longValue();
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e); // "ASCII unsupported"
		}
	}

	/**
	 * Returns a double whose ASCII representation are the bytes in the given
	 * list. If lb is null NullPointerException is thrown. If lb is empty
	 * NumberFormatException is thrown. Otherwise lb must contain the ASCII
	 * representation of the digits of a double number and possibly an exponent
	 * or a decimal part etc. If other characters are contained a
	 * {@link NumberFormatException} is thrown. If characters outside the ASCII
	 * range are contained the {@link String#String(byte[], String)} constructor
	 * used internally will result in unspecified behavior which in turn will
	 * lead to a NumberFormatException (hopefully). Careful - the d/D double
	 * suffix must not be included in the bytes - will result in
	 * NumberFormatException
	 *
	 * @param lb
	 *            the list of bytes that represent the characters representing a
	 *            double
	 * @return
	 * @throws NumberFormatException
	 *             if the bytes in lb are converted to a string that can't be
	 *             converted to a double
	 * @throws NullPointerException
	 *             if lb is null
	 */
	public static double listToDouble(List<Byte> lb)
			throws NumberFormatException {
		if (lb == null)
			throw new NullPointerException("List<Byte> can't be null");
		try {
			return new BigDecimal(listToString(lb, ASCII)).doubleValue();
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e); // "ASCII unsupported"
		}
	}

	public static List<Byte> listFromArray(byte[] ba) {
		if (ba == null) throw new NullPointerException("byte[] can't be null");
		final List<Byte> lb = new ArrayList<Byte>();
		for (byte b : ba) {
			lb.add(b);
		}
		return lb;
	}
}
