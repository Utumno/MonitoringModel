package gr.uoa.di.monitoring.model;

public abstract class Data {

	/**
	 * "Uniquely" identifies the device (IMEI is for phones only). Must be
	 * String (not long) to cater for cases when IMEI is not available and for
	 * preparing the ground for better unique identifiers
	 */
	private String imei;

	public Data(String imei) {
		this.imei = imei;
	}

	// =========================================================================
	// Accessors
	// =========================================================================
	public String getImei() {
		return imei;
	}
}
