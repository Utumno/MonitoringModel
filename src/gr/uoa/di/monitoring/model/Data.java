package gr.uoa.di.monitoring.model;

import org.apache.http.util.EncodingUtils;

import java.util.Date;

/**
 * Base class for the Data classes. Those must be implemented so as to be
 * effectively immutable - meaning that once instances are safely published they
 * should be thread safe. No mutators or public constructors should be provided.
 */
public abstract class Data {

	long time; // the subclasses set this
	static final String N = System.getProperty("line.separator");
	static final String IS = ": ";

	// =========================================================================
	// API
	// =========================================================================
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

	@Override
	public String toString() {
		return "Time" + IS + new Date(time);
	}

	// helper
	/**
	 * Returns a byte array representation of current time. This is the default
	 * time field for a Data subclass that does not extract its time from the
	 * android provided data. Current time will be just the moment of the method
	 * invocation
	 *
	 * @param currentTimeMillis
	 *            meant to be passed {@link System#currentTimeMillis()}) in
	 *
	 * @return a byte[] with current time
	 */
	static byte[] currentTime(long currentTimeMillis) {
		return EncodingUtils.getAsciiBytes(currentTimeMillis + "");
	}
}
