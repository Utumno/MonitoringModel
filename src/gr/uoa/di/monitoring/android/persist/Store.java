package gr.uoa.di.monitoring.android.persist;

import gr.uoa.di.java.helpers.Utils;

public final class Store {

	private Store() {}

	static final String FILENAME_SEPA = "_";
	// ISSUE 6 : only the SSIDs can contain it - I must provide for it
	static final byte DELIMITER = 0;
	static final byte ARRAY_DELIMITER = 1;
	static final byte NEWLINE = '\n';
	/**
	 * The parsers need to create strings from the files they parse - this field
	 * specifies the expected encoding of the files. The only place where I need
	 * to be careful is ISSUE#6 related - otherwise this might as well be ASCII.
	 * Used by the Data Fields enums - which return ASCII bytes in their
	 * getData()
	 */
	public static final String FILES_ENCODING = Utils.UTF8;
}
