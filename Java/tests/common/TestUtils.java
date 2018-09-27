package common;

import java.math.BigInteger;

public class TestUtils {

	public static byte[] hexToBytes(String hex) {
		byte[] output = new byte[hex.length() / 2];
		byte[] converted = new BigInteger("00" + hex, 16).toByteArray();

		int convertedLength = Math.min(output.length, converted.length);
		int convertedStart = converted.length - convertedLength;

		int outputStart = output.length - convertedLength;

		System.arraycopy(converted, convertedStart, output, outputStart, convertedLength);

		return output;
	}

}
