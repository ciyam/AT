package org.ciyam.at;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * API for CIYAM AT "Function Codes" for blockchain-specific interactions.
 * <p>
 * For more information, see the specification document at:<br>
 * <a href="http://ciyam.org/at/at_api.html">Automated Transactions API Specification</a>
 * <p>
 * Note that "timestamp" does not mean a real timestamp but instead is an artificial timestamp that includes two parts. The first part is a block height (32
 * bits) with the second part being the number of the transaction if applicable (also 32 bits and zero if not applicable).
 *
 */
public abstract class API {

	/** Suggested transaction types to be used by the AT sub-system */
	public enum ATTransactionType {
		PAYMENT(0),
		MESSAGE(1);

		public final long value;

		private static final Map<Long, ATTransactionType> map = stream(ATTransactionType.values()).collect(toMap(type -> type.value, type -> type));

		ATTransactionType(long value) {
			this.value = value;
		}

		public static ATTransactionType valueOf(long value) {
			return map.get(value);
		}
	}

	/** Returns maximum number of permitted steps per execution round */
	public abstract int getMaxStepsPerRound();

	/** Returns fee for executing opcode in terms of execution "steps" */
	public abstract int getOpCodeSteps(OpCode opcode);

	/** Returns fee per execution "step" */
	public abstract long getFeePerStep();

	/** Returns current blockchain's height */
	public abstract int getCurrentBlockHeight();

	/** Returns block height where AT was created */
	public abstract int getATCreationBlockHeight(MachineState state);

	/** Returns previous block's height */
	public int getPreviousBlockHeight() {
		return getCurrentBlockHeight() - 1;
	}

	/** Put previous block's signature hash in A */
	public abstract void putPreviousBlockHashInA(MachineState state);

	/** Put next transaction to AT after timestamp in A, or zero A if no more transactions */
	public abstract void putTransactionAfterTimestampInA(Timestamp timestamp, MachineState state);

	/** Return type from transaction in A, or 0xffffffffffffffff if A not valid transaction */
	public abstract long getTypeFromTransactionInA(MachineState state);

	/** Return amount from transaction in A, after transaction fees have been deducted, or 0xffffffffffffffff if A not valid transaction */
	public abstract long getAmountFromTransactionInA(MachineState state);

	/** Return timestamp from transaction in A, or 0xffffffffffffffff if A not valid transaction */
	public abstract long getTimestampFromTransactionInA(MachineState state);

	/**
	 * Generate pseudo-random number using transaction in A.
	 * <p>
	 * AT should sleep so it can use next block as source of entropy.
	 * <p>
	 * Set <tt>state.isSleeping = true</tt> before exit on first call.<br>
	 * <tt>state.steps</tt> will be zero on second call after wake-up.
	 * <p>
	 * Returns 0xffffffffffffffff if A not valid transaction.
	 */
	public abstract long generateRandomUsingTransactionInA(MachineState state);

	/** Put 'message' from transaction in A into B, or zero B if not a message transaction */
	public abstract void putMessageFromTransactionInAIntoB(MachineState state);

	/** Put sender/creator address from transaction in A into B */
	public abstract void putAddressFromTransactionInAIntoB(MachineState state);

	/** Put AT's creator's address into B */
	public abstract void putCreatorAddressIntoB(MachineState state);

	/** Return AT's current balance */
	public abstract long getCurrentBalance(MachineState state);

	/** Pay passed amount, or current balance if necessary, (fee inclusive) to address in B */
	public abstract void payAmountToB(long amount, MachineState state);

	/** Send 'message' in A to address in B */
	public abstract void messageAToB(MachineState state);

	/**
	 * Returns <tt>minutes</tt> of blocks added to 'timestamp'
	 * <p>
	 * <tt>minutes</tt> is converted to rough number of blocks and added to 'timestamp' to create return value.
	 */
	public abstract long addMinutesToTimestamp(Timestamp timestamp, long minutes, MachineState state);

	/**
	 * AT has finished. Return remaining funds to creator.
	 * 
	 * @param amount
	 *            - final balance to be returned to creator
	 * @param state
	 */
	public abstract void onFinished(long amount, MachineState state);

	/** AT has encountered fatal error */
	public abstract void onFatalError(MachineState state, ExecutionException e);

	/** Pre-execute checking of param requirements for platform-specific functions */
	public abstract void platformSpecificPreExecuteCheck(int paramCount, boolean returnValueExpected, MachineState state, short rawFunctionCode)
			throws IllegalFunctionCodeException;

	/**
	 * Platform-specific function execution after checking correct calling OpCode
	 * 
	 * @throws ExecutionException
	 */
	public abstract void platformSpecificPostCheckExecute(FunctionData functionData, MachineState state, short rawFunctionCode) throws ExecutionException;

	/** Convenience method to allow subclasses to access package-scoped MachineState.setIsSleeping */
	protected void setIsSleeping(MachineState state, boolean isSleeping) {
		state.setIsSleeping(isSleeping);
	}

	/** Convenience method to allow subclasses to test package-scoped MachineState.isFirstOpCodeAfterSleeping */
	protected boolean isFirstOpCodeAfterSleeping(MachineState state) {
		return state.isFirstOpCodeAfterSleeping();
	}

	/** Convenience method to allow subclasses to access MachineState.rewindCodePosition */
	protected void rewindCodePosition(MachineState state, int offset) {
		state.rewindCodePosition(offset);
	}

	protected void setSleepUntilHeight(MachineState state, int height) {
		state.setSleepUntilHeight(height);
	}

	/* Convenience methods to allow subclasses to access package-scoped a1-a4, b1-b4 variables */

	public void zeroA(MachineState state) {
		state.a1 = 0L;
		state.a2 = 0L;
		state.a3 = 0L;
		state.a4 = 0L;
	}

	public void zeroB(MachineState state) {
		state.b1 = 0L;
		state.b2 = 0L;
		state.b3 = 0L;
		state.b4 = 0L;
	}

	public void setAToMaxValue(MachineState state) {
		state.a1 = 0xffffffffffffffffL;
		state.a2 = 0xffffffffffffffffL;
		state.a3 = 0xffffffffffffffffL;
		state.a4 = 0xffffffffffffffffL;
	}

	public void setA1(MachineState state, long value) {
		state.a1 = value;
	}

	public void setA2(MachineState state, long value) {
		state.a2 = value;
	}

	public void setA3(MachineState state, long value) {
		state.a3 = value;
	}

	public void setA4(MachineState state, long value) {
		state.a4 = value;
	}

	public void setA(MachineState state, byte[] bytes) {
		// Enforce endian
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		state.a1 = byteBuffer.getLong();
		state.a2 = byteBuffer.getLong();
		state.a3 = byteBuffer.getLong();
		state.a4 = byteBuffer.getLong();
	}

	public void setB1(MachineState state, long value) {
		state.b1 = value;
	}

	public void setB2(MachineState state, long value) {
		state.b2 = value;
	}

	public void setB3(MachineState state, long value) {
		state.b3 = value;
	}

	public void setB4(MachineState state, long value) {
		state.b4 = value;
	}

	public void setB(MachineState state, byte[] bytes) {
		// Enforce endian
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		state.b1 = byteBuffer.getLong();
		state.b2 = byteBuffer.getLong();
		state.b3 = byteBuffer.getLong();
		state.b4 = byteBuffer.getLong();
	}

}
