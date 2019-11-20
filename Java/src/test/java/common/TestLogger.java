package common;

import org.ciyam.at.LoggerInterface;

public class TestLogger implements LoggerInterface {
	@Override
	public void error(String message) {
		System.err.println("ERROR: " + message);
	}

	@Override
	public void debug(String message) {
		System.err.println("DEBUG: " + message);
	}

	@Override
	public void echo(String message) {
		System.err.println("ECHO: " + message);
	}

}
