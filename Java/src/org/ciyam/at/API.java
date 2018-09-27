package org.ciyam.at;

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
public interface API {

	/** Returns current blockchain's height */
	public int getCurrentBlockHeight();

	/** Returns block height where AT was created */
	public int getATCreationBlockHeight(MachineState state);

	/** Returns previous block's height */
	default public int getPreviousBlockHeight() {
		return getCurrentBlockHeight() - 1;
	}

	/** Put previous block's signature hash in A */
	public void putPreviousBlockHashInA(MachineState state);

	/** Put next transaction to AT after timestamp in A */
	public void putTransactionAfterTimestampInA(Timestamp timestamp, MachineState state);

	/** Return type from transaction in A, or 0xffffffffffffffff if A not valid transaction */
	public long getTypeFromTransactionInA(MachineState state);

	/** Return amount from transaction in A, after transaction fees have been deducted, or 0xffffffffffffffff if A not valid transaction */
	public long getAmountFromTransactionInA(MachineState state);

	/** Return timestamp from transaction in A, or 0xffffffffffffffff if A not valid transaction */
	public long getTimestampFromTransactionInA(MachineState state);

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
	public long generateRandomUsingTransactionInA(MachineState state);

	/** Put 'message' from transaction in A into B */
	public void putMessageFromTransactionInAIntoB(MachineState state);

	/** Put sender/creator address from transaction in A into B */
	public void putAddressFromTransactionInAIntoB(MachineState state);

	/** Put AT's creator's address into B */
	public void putCreatorAddressIntoB(MachineState state);

	/** Return AT's current balance */
	public long getCurrentBalance(MachineState state);

	/** Return AT's previous balance at end of last execution round. Does not include any amounts sent to AT since */
	public long getPreviousBalance(MachineState state);

	/** Pay passed amount, or current balance if necessary, (fee inclusive) to address in B */
	public void payAmountToB(long value1, MachineState state);

	/** Pay AT's current balance to address in B */
	public void payCurrentBalanceToB(MachineState state);

	/** Pay AT's previous balance to address in B */
	public void payPreviousBalanceToB(MachineState state);

	/** Send 'message' in A to address in B */
	public void messageAToB(MachineState state);

	/**
	 * Returns <tt>minutes</tt> of blocks added to 'timestamp'
	 * <p>
	 * <tt>minutes</tt> is converted to rough number of blocks and added to 'timestamp' to create return value.
	 */
	public long addMinutesToTimestamp(Timestamp timestamp, long minutes, MachineState state);

	/** AT has encountered fatal error. Return remaining funds to creator */
	public void onFatalError(MachineState state, ExecutionException e);

	/** Pre-execute checking of param requirements for platform-specific functions */
	public void platformSpecificPreExecuteCheck(short functionCodeValue, int paramCount, boolean returnValueExpected) throws IllegalFunctionCodeException;

	/**
	 * Platform-specific function execution
	 * 
	 * @throws ExecutionException
	 */
	public void platformSpecificPostCheckExecute(short functionCodeValue, FunctionData functionData, MachineState state) throws ExecutionException;

}
