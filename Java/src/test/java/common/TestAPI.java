package common;

import org.ciyam.at.API;
import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionData;
import org.ciyam.at.IllegalFunctionCodeException;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.ciyam.at.Timestamp;

public class TestAPI extends API {

	private static final int BLOCK_PERIOD = 10 * 60; // average period between blocks in seconds

	private int currentBlockHeight;
	private long currentBalance;

	public TestAPI() {
		this.currentBlockHeight = 10;
		this.currentBalance = 1234L;
	}

	public void bumpCurrentBlockHeight() {
		++this.currentBlockHeight;
	}

	@Override
	public int getMaxStepsPerRound() {
		return 500;
	}

	@Override
	public int getOpCodeSteps(OpCode opcode) {
		if (opcode.value >= OpCode.EXT_FUN.value && opcode.value <= OpCode.EXT_FUN_RET_DAT_2.value)
			return 10;

		return 1;
	}

	@Override
	public long getFeePerStep() {
		return 1L;
	}

	@Override
	public int getCurrentBlockHeight() {
		return this.currentBlockHeight;
	}

	@Override
	public int getATCreationBlockHeight(MachineState state) {
		return 5;
	}

	@Override
	public void putPreviousBlockHashInA(MachineState state) {
		this.setA1(state, 9L);
		this.setA2(state, 9L);
		this.setA3(state, 9L);
		this.setA4(state, 9L);
	}

	@Override
	public void putTransactionAfterTimestampInA(Timestamp timestamp, MachineState state) {
		// Cycle through transactions: 1 -> 2 -> 3 -> 0 -> 1 ...
		this.setA1(state, (timestamp.transactionSequence + 1) % 4);
		this.setA2(state, state.getA1());
		this.setA3(state, state.getA1());
		this.setA4(state, state.getA1());
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
		if (!isFirstOpCodeAfterSleeping(state)) {
			// First call
			System.out.println("generateRandomUsingTransactionInA: first call - sleeping");

			// first-call initialization would go here

			this.setIsSleeping(state, true);

			return 0L; // not used
		} else {
			// Second call
			System.out.println("generateRandomUsingTransactionInA: second call - returning random");

			// HASH(A and new block hash)
			return (state.getA1() ^ 9L) << 3 ^ (state.getA2() ^ 9L) << 12 ^ (state.getA3() ^ 9L) << 5 ^ (state.getA4() ^ 9L);
		}
	}

	@Override
	public void putMessageFromTransactionInAIntoB(MachineState state) {
		this.setB1(state, state.getA4());
		this.setB2(state, state.getA3());
		this.setB3(state, state.getA2());
		this.setB4(state, state.getA1());
	}

	@Override
	public void putAddressFromTransactionInAIntoB(MachineState state) {
		// Dummy address
		this.setB1(state, 0xaaaaaaaaaaaaaaaaL);
		this.setB2(state, 0xaaaaaaaaaaaaaaaaL);
		this.setB3(state, 0xaaaaaaaaaaaaaaaaL);
		this.setB4(state, 0xaaaaaaaaaaaaaaaaL);
	}

	@Override
	public void putCreatorAddressIntoB(MachineState state) {
		// Dummy creator
		this.setB1(state, 0xccccccccccccccccL);
		this.setB2(state, 0xccccccccccccccccL);
		this.setB3(state, 0xccccccccccccccccL);
		this.setB4(state, 0xccccccccccccccccL);
	}

	@Override
	public long getCurrentBalance(MachineState state) {
		return this.currentBalance;
	}

	// Debugging only
	public void setCurrentBalance(long currentBalance) {
		this.currentBalance = currentBalance;
	}

	@Override
	public void payAmountToB(long amount, MachineState state) {
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
	public void onFinished(long amount, MachineState state) {
		System.out.println("Finished - refunding remaining to creator");
	}

	@Override
	public void onFatalError(MachineState state, ExecutionException e) {
		System.out.println("Fatal error: " + e.getMessage());
		System.out.println("No error address set - will refund to creator and finish");
	}

	@Override
	public void platformSpecificPreExecuteCheck(int paramCount, boolean returnValueExpected, MachineState state, short rawFunctionCode)
			throws IllegalFunctionCodeException {
		Integer requiredParamCount;
		Boolean returnsValue;

		switch (rawFunctionCode) {
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
				throw new IllegalFunctionCodeException("Unrecognised platform-specific function code 0x" + String.format("%04x", rawFunctionCode));
		}

		if (requiredParamCount == null || returnsValue == null)
			throw new IllegalFunctionCodeException("Error during platform-specific function pre-execute check");

		if (paramCount != requiredParamCount)
			throw new IllegalFunctionCodeException("Passed paramCount (" + paramCount + ") does not match platform-specific function code 0x"
					+ String.format("%04x", rawFunctionCode) + " required paramCount (" + requiredParamCount + ")");

		if (returnValueExpected != returnsValue)
			throw new IllegalFunctionCodeException("Passed returnValueExpected (" + returnValueExpected + ") does not match platform-specific function code 0x"
					+ String.format("%04x", rawFunctionCode) + " return signature (" + returnsValue + ")");
	}

	@Override
	public void platformSpecificPostCheckExecute(FunctionData functionData, MachineState state, short rawFunctionCode) throws ExecutionException {
		switch (rawFunctionCode) {
			case 0x0501:
				System.out.println("Platform-specific function 0x0501 called with 0x" + String.format("%016x", functionData.value1));
				break;

			case 0x0502:
				System.out.println("Platform-specific function 0x0502 called!");
				functionData.returnValue = 0x0502L;
				break;

			default:
				// Unrecognised platform-specific function code
				throw new IllegalFunctionCodeException("Unrecognised platform-specific function code 0x" + String.format("%04x", rawFunctionCode));
		}
	}

}
