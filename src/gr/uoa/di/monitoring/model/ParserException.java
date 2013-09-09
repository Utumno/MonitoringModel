package gr.uoa.di.monitoring.model;


public final class ParserException extends Exception {

	private static final long serialVersionUID = -8001584517456048494L;

	public ParserException(String string, Exception e) {
		super(string, e);
	}

	public ParserException(String string) {
		super(string);
	}
}
