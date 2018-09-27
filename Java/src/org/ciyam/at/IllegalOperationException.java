package org.ciyam.at;

@SuppressWarnings("serial")
public class IllegalOperationException extends ExecutionException {

	public IllegalOperationException() {
	}

	public IllegalOperationException(String message) {
		super(message);
	}

	public IllegalOperationException(Throwable cause) {
		super(cause);
	}

	public IllegalOperationException(String message, Throwable cause) {
		super(message, cause);
	}

}
