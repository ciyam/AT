package org.ciyam.at;

/**
 * CIYAM-AT "Timestamp"
 * <p>
 * With CIYAM-ATs, "timestamp" does not mean a real timestamp but instead is an artificial timestamp that includes three parts:
 * <p>
 * <ul>
 * <li>block height (32 bits)</li>
 * <li>blockchain ID (8 bits)</li>
 * <li>intra-block transaction sequence (24 bits)</li>
 * </ul>
 * This allows up to 256 different blockchains and up to ~16million transactions per block.
 * <p>
 * A blockchain ID of zero is assumed to be the 'native' blockchain.
 * <p>
 * Timestamp values are not directly manipulated by AT OpCodes so endianness isn't important here.
 * 
 * @see Timestamp#Timestamp(int, int, int)
 * @see Timestamp#Timestamp(long)
 * @see Timestamp#longValue()
 * @see Timestamp#toLong(int, int, int)
 *
 */
public class Timestamp {

	public static final int NATIVE_BLOCKCHAIN_ID = 0;

	public int blockHeight;
	public int blockchainId;
	public int transactionSequence;

	/**
	 * Constructs new CIYAM-AT "timestamp" using block height, blockchain ID and transaction sequence.
	 * 
	 * @param blockHeight
	 * @param blockchainId
	 * @param transactionSequence
	 */
	public Timestamp(int blockHeight, int blockchainId, int transactionSequence) {
		this.blockHeight = blockHeight;
		this.blockchainId = blockchainId;
		this.transactionSequence = transactionSequence;
	}

	/**
	 * Constructs new CIYAM-AT "timestamp" using only block height and transaction sequence.
	 * <p>
	 * Assumes native blockchain ID.
	 * 
	 * @param blockHeight
	 * @param transactionSequence
	 */
	public Timestamp(int blockHeight, int transactionSequence) {
		this(blockHeight, NATIVE_BLOCKCHAIN_ID, transactionSequence);
	}

	/**
	 * Constructs new CIYAM-AT "timestamp" using long packed with block height, blockchain ID and transaction sequence.
	 * 
	 * @param timestamp
	 */
	public Timestamp(long timestamp) {
		this.blockHeight = (int) (timestamp >> 32);
		this.blockchainId = (int) ((timestamp >> 24) & 0xffL);
		this.transactionSequence = (int) (timestamp & 0x00ffffffL);
	}

	/**
	 * Returns CIYAM-AT "timestamp" long representing block height, blockchain ID and transaction sequence.
	 * 
	 * @return CIYAM-AT "timestamp" as long
	 */
	public long longValue() {
		return Timestamp.toLong(this.blockHeight, this.blockchainId, this.transactionSequence);
	}

	/**
	 * Returns CIYAM-AT "timestamp" long representing block height, blockchain ID and transaction sequence.
	 * 
	 * @param blockHeight
	 * @param blockchainId
	 * @param transactionSequence
	 * @return CIYAM-AT "timestamp" as long
	 */
	public static long toLong(int blockHeight, int blockchainId, int transactionSequence) {
		long longValue = ((long) blockHeight) << 32;
		longValue |= ((long) blockchainId) << 24;
		longValue |= transactionSequence;
		return longValue;
	}

	/**
	 * Returns CIYAM-AT "timestamp" long representing block height, blockchain ID and transaction sequence.
	 * <p>
	 * Assumes native blockchain ID.
	 * 
	 * @param blockHeight
	 * @param transactionSequence
	 * @return CIYAM-AT "timestamp" as long
	 */
	public static long toLong(int blockHeight, int transactionSequence) {
		long longValue = ((long) blockHeight) << 32;
		// NOP: longValue |= ((long) NATIVE_BLOCKCHAIN_ID) << 24;
		longValue |= transactionSequence;
		return longValue;
	}

}
