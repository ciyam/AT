package common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.ciyam.at.API;
import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionData;
import org.ciyam.at.IllegalFunctionCodeException;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.ciyam.at.Timestamp;

public class ACCTAPI extends API {

	private class Account {
		public String address;
		public long balance;

		public Account(String address, long amount) {
			this.address = address;
			this.balance = amount;
		}
	}

	private class Transaction {
		public int txType;
		public String creator;
		public String recipient;
		public long amount;
		public long[] message;
	}

	private class Block {
		public List<Transaction> transactions;

		public Block() {
			this.transactions = new ArrayList<Transaction>();
		}
	}

	//
	private List<Block> blockchain;
	private Map<String, Account> accounts;
	private long balanceAT;

	//
	public ACCTAPI() {
		// build blockchain
		this.blockchain = new ArrayList<Block>();

		Block genesisBlock = new Block();
		this.blockchain.add(genesisBlock);

		// generate accounts
		this.accounts = new HashMap<String, Account>();

		Account initiator = new Account("Initiator", 0);
		this.accounts.put(initiator.address, initiator);

		Account responder = new Account("Responder", 10000);
		this.accounts.put(responder.address, responder);

		Account bystander = new Account("Bystander", 999);
		this.accounts.put(bystander.address, bystander);

		Account creator = new Account("Creator", 0);
		this.accounts.put(creator.address, creator);

		this.balanceAT = 50000;
	}

	public void generateNextBlock(byte[] secret) {
		Random random = new Random();

		Block block = new Block();

		System.out.println("Block " + (this.blockchain.size() + 1));

		int transactionCount = random.nextInt(5);

		for (int i = 0; i < transactionCount; ++i) {
			Transaction transaction = new Transaction();

			transaction.txType = random.nextInt(2);

			switch (transaction.txType) {
				case 0: // payment
					transaction.amount = random.nextInt(1000);
					System.out.print("Payment Tx [" + transaction.amount + "]");
					break;

				case 1: // message
					System.out.print("Message Tx [");
					transaction.message = new long[4];

					if (random.nextInt(3) == 0) {
						// correct message
						transaction.message[0] = fromBytes(secret, 0);
						transaction.message[1] = fromBytes(secret, 8);
						transaction.message[2] = fromBytes(secret, 16);
						transaction.message[3] = fromBytes(secret, 24);
					} else {
						// incorrect message
						transaction.message[0] = 0xdeadbeefdeadbeefL;
						transaction.message[1] = 0xdeadbeefdeadbeefL;
						transaction.message[2] = 0xdeadbeefdeadbeefL;
						transaction.message[3] = 0xdeadbeefdeadbeefL;
					}
					System.out.print(String.format("%016x", transaction.message[0]));
					System.out.print(String.format("%016x", transaction.message[1]));
					System.out.print(String.format("%016x", transaction.message[2]));
					System.out.print(String.format("%016x", transaction.message[3]));
					System.out.print("]");
					break;
			}

			transaction.creator = getRandomAccount();
			transaction.recipient = getRandomAccount();
			System.out.println(" from " + transaction.creator + " to " + transaction.recipient);

			block.transactions.add(transaction);
		}

		this.blockchain.add(block);
	}

	/** Convert long to little-endian byte array */
	@SuppressWarnings("unused")
	private byte[] toByteArray(long value) {
		return new byte[] { (byte) (value), (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24), (byte) (value >> 32), (byte) (value >> 40),
				(byte) (value >> 48), (byte) (value >> 56) };
	}

	/** Convert part of little-endian byte[] to long */
	private long fromBytes(byte[] bytes, int start) {
		return (bytes[start] & 0xffL) | (bytes[start + 1] & 0xffL) << 8 | (bytes[start + 2] & 0xffL) << 16 | (bytes[start + 3] & 0xffL) << 24
				| (bytes[start + 4] & 0xffL) << 32 | (bytes[start + 5] & 0xffL) << 40 | (bytes[start + 6] & 0xffL) << 48 | (bytes[start + 7] & 0xffL) << 56;
	}

	private String getRandomAccount() {
		int numAccounts = this.accounts.size();
		int accountIndex = new Random().nextInt(numAccounts);

		List<Account> accounts = this.accounts.values().stream().collect(Collectors.toList());
		return accounts.get(accountIndex).address;
	}

	@Override
	public int getOpCodeSteps(OpCode opcode) {
		return 1;
	}

	@Override
	public long getFeePerStep() {
		return 1L;
	}

	@Override
	public int getCurrentBlockHeight() {
		return this.blockchain.size();
	}

	@Override
	public int getATCreationBlockHeight(MachineState state) {
		return 1;
	}

	@Override
	public void putPreviousBlockHashInA(MachineState state) {
		this.setA1(state, this.blockchain.size() - 1);
		this.setA2(state, state.getA1());
		this.setA3(state, state.getA1());
		this.setA4(state, state.getA1());
	}

	@Override
	public void putTransactionAfterTimestampInA(Timestamp timestamp, MachineState state) {
		int blockHeight = timestamp.blockHeight;
		int transactionSequence = timestamp.transactionSequence + 1;

		while (blockHeight <= this.blockchain.size()) {
			Block block = this.blockchain.get(blockHeight - 1);

			List<Transaction> transactions = block.transactions;

			if (transactionSequence > transactions.size() - 1) {
				// No more transactions at this height
				++blockHeight;
				transactionSequence = 0;
				continue;
			}

			Transaction transaction = transactions.get(transactionSequence);

			if (transaction.recipient.equals("Initiator")) {
				// Found a transaction
				System.out.println("Found transaction at height " + blockHeight + " sequence " + transactionSequence);

				// Generate pseudo-hash of transaction
				this.setA1(state, new Timestamp(blockHeight, transactionSequence).longValue());
				this.setA2(state, state.getA1());
				this.setA3(state, state.getA1());
				this.setA4(state, state.getA1());
				return;
			}

			++transactionSequence;
		}

		// Nothing found
		this.setA1(state, 0L);
		this.setA2(state, 0L);
		this.setA3(state, 0L);
		this.setA4(state, 0L);
	}

	@Override
	public long getTypeFromTransactionInA(MachineState state) {
		Timestamp timestamp = new Timestamp(state.getA1());
		Block block = this.blockchain.get(timestamp.blockHeight - 1);
		Transaction transaction = block.transactions.get(timestamp.transactionSequence);
		return transaction.txType;
	}

	@Override
	public long getAmountFromTransactionInA(MachineState state) {
		Timestamp timestamp = new Timestamp(state.getA1());
		Block block = this.blockchain.get(timestamp.blockHeight - 1);
		Transaction transaction = block.transactions.get(timestamp.transactionSequence);
		return transaction.amount;
	}

	@Override
	public long getTimestampFromTransactionInA(MachineState state) {
		// Transaction hash in A is actually just 4 copies of transaction's "timestamp"
		Timestamp timestamp = new Timestamp(state.getA1());
		return timestamp.longValue();
	}

	@Override
	public long generateRandomUsingTransactionInA(MachineState state) {
		// NOT USED
		return 0L;
	}

	@Override
	public void putMessageFromTransactionInAIntoB(MachineState state) {
		Timestamp timestamp = new Timestamp(state.getA1());
		Block block = this.blockchain.get(timestamp.blockHeight - 1);
		Transaction transaction = block.transactions.get(timestamp.transactionSequence);
		this.setB1(state, transaction.message[0]);
		this.setB2(state, transaction.message[1]);
		this.setB3(state, transaction.message[2]);
		this.setB4(state, transaction.message[3]);
	}

	@Override
	public void putAddressFromTransactionInAIntoB(MachineState state) {
		Timestamp timestamp = new Timestamp(state.getA1());
		Block block = this.blockchain.get(timestamp.blockHeight - 1);
		Transaction transaction = block.transactions.get(timestamp.transactionSequence);
		this.setB1(state, transaction.creator.charAt(0));
		this.setB2(state, state.getB1());
		this.setB3(state, state.getB1());
		this.setB4(state, state.getB1());
	}

	@Override
	public void putCreatorAddressIntoB(MachineState state) {
		// Dummy creator
		this.setB1(state, "C".charAt(0));
		this.setB2(state, state.getB1());
		this.setB3(state, state.getB1());
		this.setB4(state, state.getB1());
	}

	@Override
	public long getCurrentBalance(MachineState state) {
		return this.balanceAT;
	}

	public void setCurrentBalance(long balance) {
		this.balanceAT = balance;
		System.out.println("New AT balance: " + balance);
	}

	@Override
	public void payAmountToB(long amount, MachineState state) {
		// Determine recipient using first char in B1
		char firstChar = String.format("%c", (byte) state.getB1()).charAt(0);
		Account recipient = this.accounts.values().stream().filter((account) -> account.address.charAt(0) == firstChar).findFirst().get();

		// Simulate payment
		recipient.balance += amount;
		System.out.println("Paid " + amount + " to " + recipient.address + ", their balance now: " + recipient.balance);

		// For debugging, output our new balance
		long balance = state.getCurrentBalance() - amount;
		System.out.println("Our balance now: " + balance);
	}

	@Override
	public void messageAToB(MachineState state) {
		// NOT USED
	}

	@Override
	public long addMinutesToTimestamp(Timestamp timestamp, long minutes, MachineState state) {
		timestamp.blockHeight += (int) minutes;
		return timestamp.longValue();
	}

	@Override
	public void onFinished(long amount, MachineState state) {
		System.out.println("Finished - refunding remaining to creator");

		Account creator = this.accounts.get("Creator");
		creator.balance += amount;
		System.out.println("Paid " + amount + " to " + creator.address + ", their balance now: " + creator.balance);
	}

	@Override
	public void onFatalError(MachineState state, ExecutionException e) {
		System.out.println("Fatal error: " + e.getMessage());
		System.out.println("No error address set - will refund to creator and finish");
	}

	@Override
	public void platformSpecificPreExecuteCheck(short functionCodeValue, int paramCount, boolean returnValueExpected) throws IllegalFunctionCodeException {
		// NOT USED
	}

	@Override
	public void platformSpecificPostCheckExecute(short functionCodeValue, FunctionData functionData, MachineState state) throws ExecutionException {
		// NOT USED
	}

}
