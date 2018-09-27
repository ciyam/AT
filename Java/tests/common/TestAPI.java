package common;

import org.ciyam.at.API;
import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionData;
import org.ciyam.at.IllegalFunctionCodeException;
import org.ciyam.at.MachineState;
import org.ciyam.at.Timestamp;

public class TestAPI implements API {

	private static final int BLOCK_PERIOD = 10 * 60; // average period between blocks in seconds

	@Override
	public int getCurrentBlockHeight() {
		return 10;
	}

	@Override
	public int getATCreationBlockHeight(MachineState state) {
		return 5;
	}

	@Override
	public void putPreviousBlockHashInA(MachineState state) {
		state.a1 = 9L;
		state.a2 = 9L;
		state.a3 = 9L;
		state.a4 = 9L;
	}

	@Override
	public void putTransactionAfterTimestampInA(Timestamp timestamp, MachineState state) {
		// Cycle through transactions: 1 -> 2 -> 3 -> 0 -> 1 ...
		state.a1 = (timestamp.transactionSequence + 1) % 4;
		state.a2 = state.a1;
		state.a3 = state.a1;
		state.a4 = state.a1;
	}

	@Override
	public long getTypeFromTransactionInA(MachineState state) {
		return 0L;
	}

	@Override
	public long getAmountFromTransactionInA(MachineState state) {
		return 123L;
	}

	@Override
	public long getTimestampFromTransactionInA(MachineState state) {
		return 1536227162000L;
	}

	@Override
	public long generateRandomUsingTransactionInA(MachineState state) {
		if (state.steps != 0) {
			// First call
			System.out.println("generateRandomUsingTransactionInA: first call - sleeping");

			// Perform init?

			state.isSleeping = true;

			return 0L; // not used
		} else {
			// Second call
			System.out.println("generateRandomUsingTransactionInA: second call - returning random");

			// HASH(A and new block hash)
			return (state.a1 ^ 9L) << 3 ^ (state.a2 ^ 9L) << 12 ^ (state.a3 ^ 9L) << 5 ^ (state.a4 ^ 9L);
		}
	}

	@Override
	public void putMessageFromTransactionInAIntoB(MachineState state) {
		state.b1 = state.a4;
		state.b2 = state.a3;
		state.b3 = state.a2;
		state.b4 = state.a1;
	}

	@Override
	public void putAddressFromTransactionInAIntoB(MachineState state) {
		// Dummy address
		state.b1 = 0xaaaaaaaaaaaaaaaaL;
		state.b2 = 0xaaaaaaaaaaaaaaaaL;
		state.b3 = 0xaaaaaaaaaaaaaaaaL;
		state.b4 = 0xaaaaaaaaaaaaaaaaL;
	}

	@Override
	public void putCreatorAddressIntoB(MachineState state) {
		// Dummy creator
		state.b1 = 0xccccccccccccccccL;
		state.b2 = 0xccccccccccccccccL;
		state.b3 = 0xccccccccccccccccL;
		state.b4 = 0xccccccccccccccccL;
	}

	@Override
	public long getCurrentBalance(MachineState state) {
		return 12345L;
	}

	@Override
	public long getPreviousBalance(MachineState state) {
		return 10000L;
	}

	@Override
	public void payAmountToB(long value1, MachineState state) {
	}

	@Override
	public void payCurrentBalanceToB(MachineState state) {
	}

	@Override
	public void payPreviousBalanceToB(MachineState state) {
	}

	@Override
	public void messageAToB(MachineState state) {
	}

	@Override
	public long addMinutesToTimestamp(Timestamp timestamp, long minutes, MachineState state) {
		timestamp.blockHeight = ((int) minutes * 60) / BLOCK_PERIOD;
		return timestamp.longValue();
	}

	@Override
	public void onFatalError(MachineState state, ExecutionException e) {
		System.out.println("Fatal error: " + e.getMessage());
		System.out.println("No error address set - refunding to creator and finishing");
	}

	@Override
	public void platformSpecificPreExecuteCheck(short functionCodeValue, int paramCount, boolean returnValueExpected) throws IllegalFunctionCodeException {
		Integer requiredParamCount;
		Boolean returnsValue;

		switch (functionCodeValue) {
			case 0x0501:
				// take one arg, no return value
				requiredParamCount = 1;
				returnsValue = false;
				break;

			case 0x0502:
				// take no arg, return a value
				requiredParamCount = 0;
				returnsValue = true;
				break;

			default:
				// Unrecognised platform-specific function code
				throw new IllegalFunctionCodeException("Unrecognised platform-specific function code 0x" + String.format("%04x", functionCodeValue));
		}

		if (requiredParamCount == null || returnsValue == null)
			throw new IllegalFunctionCodeException("Error during platform-specific function pre-execute check");

		if (paramCount != requiredParamCount)
			throw new IllegalFunctionCodeException("Passed paramCount (" + paramCount + ") does not match platform-specific function code 0x"
					+ String.format("%04x", functionCodeValue) + " required paramCount (" + requiredParamCount + ")");

		if (returnValueExpected != returnsValue)
			throw new IllegalFunctionCodeException("Passed returnValueExpected (" + returnValueExpected + ") does not match platform-specific function code 0x"
					+ String.format("%04x", functionCodeValue) + " return signature (" + returnsValue + ")");
	}

	@Override
	public void platformSpecificPostCheckExecute(short functionCodeValue, FunctionData functionData, MachineState state) throws ExecutionException {
		switch (functionCodeValue) {
			case 0x0501:
				System.out.println("Platform-specific function 0x0501 called with 0x" + String.format("%016x", functionData.value1));
				break;

			case 0x0502:
				System.out.println("Platform-specific function 0x0502 called!");
				functionData.returnValue = 0x0502L;
				break;

			default:
				// Unrecognised platform-specific function code
				throw new IllegalFunctionCodeException("Unrecognised platform-specific function code 0x" + String.format("%04x", functionCodeValue));
		}
	}

}
