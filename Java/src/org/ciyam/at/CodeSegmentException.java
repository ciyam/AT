package org.ciyam.at;

@SuppressWarnings("serial")
public class CodeSegmentException extends ExecutionException {

	public CodeSegmentException() {
	}

	public CodeSegmentException(String message) {
		super(message);
	}

	public CodeSegmentException(Throwable cause) {
		super(cause);
	}

	public CodeSegmentException(String message, Throwable cause) {
		super(message, cause);
	}

}
