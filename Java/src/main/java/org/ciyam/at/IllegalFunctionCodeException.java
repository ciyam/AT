package org.ciyam.at;

@SuppressWarnings("serial")
public class IllegalFunctionCodeException extends ExecutionException {

	public IllegalFunctionCodeException() {
	}

	public IllegalFunctionCodeException(String message) {
		super(message);
	}

	public IllegalFunctionCodeException(Throwable cause) {
		super(cause);
	}

	public IllegalFunctionCodeException(String message, Throwable cause) {
		super(message, cause);
	}

}
