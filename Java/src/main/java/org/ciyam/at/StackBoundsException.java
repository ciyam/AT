package org.ciyam.at;

@SuppressWarnings("serial")
public class StackBoundsException extends ExecutionException {

	public StackBoundsException() {
	}

	public StackBoundsException(String message) {
		super(message);
	}

	public StackBoundsException(Throwable cause) {
		super(cause);
	}

	public StackBoundsException(String message, Throwable cause) {
		super(message, cause);
	}

}
