package gr.uoa.di.monitoring.model;

import gr.uoa.di.monitoring.android.persist.ParserException;

import java.util.List;

/**
 * Interface meant to be implemented by enums that encapsulate in their
 * constants the fields of the {@link Data} subclasses. The enum constants
 * should provide methods to construct the Data objects from android supplied
 * data as well as methods to retrieve those data from their storage. Currently
 * retrieving the data means parsing the files sent to the server
 *
 * @param <T>
 *            the type of the data provided by android, ex. Location
 * @param <D>
 *            the corresponding type of the data in the model, ex. Position
 * @param <K>
 *            the type of the list that the parser expects ( {@code List<Byte>}
 *            or {@code List<List<Byte>>})
 */
public interface Fields<T, D extends Data, K> {

	/**
	 * Returns true if the field corresponds to a list of values, for instance a
	 * list of SSIDs
	 *
	 * @return true for a list field
	 */
	boolean isList();

	/**
	 * Accepts an android object and extracts its data as a List of byte[]. If
	 * the field is not a list this list will contain a single item. It also
	 * accepts an instance of Data and populates its relevant field.
	 *
	 * @param data
	 *            an android object to extract our values from
	 * @param output
	 *            a Data instance to be populated
	 * @return a List<byte[]> representing the field value(s)
	 */
	List<byte[]> getData(T data, D output);

	/**
	 * Accepts a List<List<Byte>> or a List<Byte> and returns a data instance.
	 * The data instance is passed in as the second argument. This method is to
	 * be called for all the values of the Fields enum supplying a new data
	 * instance. When all invocations are finished the data instance is fully
	 * constructed. The method does not accept a file so as to decouple its
	 * implementation from the particular details of the File Store policy (such
	 * as delimiters and the like)
	 *
	 * @param list
	 *            contains List<Byte> representing the data instance to
	 *            construct
	 * @param objectToModify
	 *            a Data instance which must not be null
	 * @return a Data instance in the process of being constructed
	 * @throws ParserException
	 *             if the list can't be parsed
	 */
	D parse(K list, D objectToModify) throws ParserException;
}
