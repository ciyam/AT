package org.ciyam.at;

/**
 * CIYAM-AT "Timestamp"
 * <p>
 * With CIYAM-ATs, "timestamp" does not mean a real timestamp but instead is an artificial timestamp that includes two parts. The first part is a block height
 * (32 bits) with the second part being the number of the transaction if applicable (also 32 bits and zero if not applicable). Timestamps can thus be
 * represented as a 64 bit long.
 * <p>
 * 
 * @see Timestamp#Timestamp(int, int)
 * @see Timestamp#Timestamp(long)
 * @see Timestamp#longValue()
 * @see Timestamp#toLong(int, int)
 *
 */
public class Timestamp {

	public int blockHeight;
	public int transactionSequence;

	/**
	 * Constructs new CIYAM-AT "timestamp" using block height and transaction sequence.
	 * 
	 * @param blockHeight
	 * @param transactionSequence
	 */
	public Timestamp(int blockHeight, int transactionSequence) {
		this.blockHeight = blockHeight;
		this.transactionSequence = transactionSequence;
	}

	/**
	 * Constructs new CIYAM-AT "timestamp" using long packed with block height and transaction sequence.
	 * 
	 * @param timestamp
	 */
	public Timestamp(long timestamp) {
		this.blockHeight = (int) (timestamp >> 32);
		this.transactionSequence = (int) (timestamp & 0xffffff);
	}

	/**
	 * Returns CIYAM-AT "timestamp" long representing block height and transaction sequence.
	 * 
	 * @return CIYAM-AT "timestamp" as long
	 */
	public long longValue() {
		return Timestamp.toLong(this.blockHeight, this.transactionSequence);
	}

	/**
	 * Returns CIYAM-AT "timestamp" long representing block height and transaction sequence.
	 * 
	 * @param blockHeight
	 * @param transactionSequence
	 * @return CIYAM-AT "timestamp" as long
	 */
	public static long toLong(int blockHeight, int transactionSequence) {
		long longValue = blockHeight;
		longValue <<= 32;
		longValue |= transactionSequence;
		return longValue;
	}

}
