package gr.uoa.di.monitoring.model;

import org.apache.http.util.EncodingUtils;

import java.util.Date;

public abstract class Data {

	long time; // the subclasses set this
	static final String N = System.getProperty("line.separator");
	static final String IS = ": ";

	/**
	 * Returns the filename the data are saved in for each Data subclass
	 *
	 * @return the filename the data are saved in
	 */
	public abstract String getFilename();

	/**
	 * Returns a byte array representation of current time (wraps
	 * {@link System#currentTimeMillis()}). This is the default time field for a
	 * Data subclass that does not extract its time from the android provided
	 * data. Current time means just the moment of the method invocation
	 *
	 * @return a byte[] with current time
	 */
	static byte[] currentTime() {
		return EncodingUtils.getAsciiBytes(System.currentTimeMillis() + "");
	}

	@Override
	public String toString() {
		return "Time" + IS + new Date(time);
	}

	/**
	 * Compares two data instances to see if they are fairly equal. Fairly equal
	 * is defined in individual types. It checks equality of some basic
	 * attributes
	 *
	 * @param d
	 *            the other data
	 * @return true if this is fairly equal with the other, false otherwise
	 *         (including the case they are instances of different types or d is
	 *         null)
	 * @throws NullPointerException
	 *             if any of the String required attributes is null
	 */
	abstract public boolean fairlyEqual(final Data d)
			throws NullPointerException;

	/**
	 * Use this to persist the datum in string form. It keeps the time as long
	 * to easily manipulate it.
	 *
	 * @return a string representation of the Data object to be persisted
	 */
	abstract public String stringForm();

	// =========================================================================
	// Accessors
	// =========================================================================
	public long getTime() {
		return time;
	}
}
