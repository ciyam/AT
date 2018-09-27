package org.ciyam.at;

@SuppressWarnings("serial")
public class InvalidAddressException extends ExecutionException {

	public InvalidAddressException() {
	}

	public InvalidAddressException(String message) {
		super(message);
	}

	public InvalidAddressException(Throwable cause) {
		super(cause);
	}

	public InvalidAddressException(String message, Throwable cause) {
		super(message, cause);
	}

}
