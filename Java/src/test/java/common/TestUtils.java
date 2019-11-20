package common;

import java.math.BigInteger;

import org.ciyam.at.MachineState;

public class TestUtils {

	// v3 constants replicated due to private cope in MachineState
	public static final int CODE_PAGE_SIZE = 1;
	public static final int DATA_PAGE_SIZE = MachineState.VALUE_SIZE;
	public static final int CALL_STACK_PAGE_SIZE = MachineState.ADDRESS_SIZE;
	public static final int USER_STACK_PAGE_SIZE = MachineState.VALUE_SIZE;

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
