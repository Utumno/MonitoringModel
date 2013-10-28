package gr.uoa.di.monitoring.model;

import org.apache.http.util.EncodingUtils;

import java.util.Date;

public abstract class Data {

	/**
	 * "Uniquely" identifies the device (IMEI is for phones only). Must be
	 * String (not long) to cater for cases when IMEI is not available and for
	 * preparing the ground for better unique identifiers
	 */
	private String imei;
	long time; // the subclasses set this
	static final String N = System.getProperty("line.separator");

	public Data(String imei) {
		this.imei = imei;
	}

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
		return "Time : " + new Date(time);
	}

	// =========================================================================
	// Accessors
	// =========================================================================
	public String getImei() {
		return imei;
	}

	public long getTime() {
		return time;
	}
}
