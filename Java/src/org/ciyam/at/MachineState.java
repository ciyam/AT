package org.ciyam.at;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MachineState {

	/** Header bytes length */
	public static final int HEADER_LENGTH = 2 + 2 + 2 + 2 + 2 + 2; // version reserved code data call-stack user-stack

	/** Size of value stored in data segment - typically 8 bytes (long) */
	public static final int VALUE_SIZE = 8;

	/** Size of code-address - typically 4 bytes (int) */
	public static final int ADDRESS_SIZE = 4;

	/** Maximum value for an address in the code segment */
	public static final int MAX_CODE_ADDRESS = 0x1fffffff;

	/** Bytes per code page */
	public static final int CODE_PAGE_SIZE = 1;

	/** Bytes per data page */
	public static final int DATA_PAGE_SIZE = VALUE_SIZE;

	/** Bytes per call stack page */
	public static final int CALL_STACK_PAGE_SIZE = ADDRESS_SIZE;

	/** Bytes per user stack page */
	public static final int USER_STACK_PAGE_SIZE = VALUE_SIZE;

	/** Program Counter: offset into code to point of current execution */
	public int programCounter;

	/** Initial program counter value to use on next block after current block's execution has stopped. 0 by default */
	public int onStopAddress;

	/** Program counter value to use if an error occurs during execution. If null upon error, refund all funds to creator and finish */
	public Integer onErrorAddress;

	/** Execution for current block has stopped. Continue at current program counter on next/specific block */
	public boolean isSleeping;

	/** Block height required to wake from sleeping, or null if not in use */
	public Integer sleepUntilHeight;

	/** Execution for current block has stopped. Restart at onStopAddress on next block */
	public boolean isStopped;

	/** Execution stopped due to lack of funds for processing. Restart at onStopAddress if frozenBalance increases */
	public boolean isFrozen;

	/** Balance at which there were not enough funds, or null if not in use */
	public Long frozenBalance;

	/** Execution permanently stopped */
	public boolean isFinished;

	/** Execution permanently stopped due to fatal error */
	public boolean hadFatalError;

	// 256-bit pseudo-registers
	public long a1;
	public long a2;
	public long a3;
	public long a4;

	public long b1;
	public long b2;
	public long b3;
	public long b4;

	public int currentBlockHeight;

	/** Number of opcodes processed this execution */
	public int steps;

	public API api;
	LoggerInterface logger;

	public short version;
	public short reserved;
	public short numCodePages;
	public short numDataPages;
	public short numCallStackPages;
	public short numUserStackPages;

	public byte[] headerBytes;

	public ByteBuffer codeByteBuffer;
	public ByteBuffer dataByteBuffer;
	public ByteBuffer callStackByteBuffer;
	public ByteBuffer userStackByteBuffer;

	private class Flags {
		private int flags;

		public Flags() {
			flags = 0;
		}

		public Flags(int value) {
			this.flags = value;
		}

		public void push(boolean flag) {
			flags <<= 1;
			flags |= flag ? 1 : 0;
		}

		public boolean pop() {
			boolean result = (flags & 1) != 0;
			flags >>>= 1;
			return result;
		}

		public int intValue() {
			return flags;
		}
	}

	/** For internal use when recreating a machine state */
	private MachineState(API api, LoggerInterface logger, byte[] headerBytes) {
		if (headerBytes.length != HEADER_LENGTH)
			throw new IllegalArgumentException("headerBytes length " + headerBytes.length + " incorrect, expected " + HEADER_LENGTH);

		this.headerBytes = headerBytes;
		parseHeader();

		this.codeByteBuffer = ByteBuffer.allocate(this.numCodePages * CODE_PAGE_SIZE).order(ByteOrder.LITTLE_ENDIAN);

		this.dataByteBuffer = ByteBuffer.allocate(this.numDataPages * DATA_PAGE_SIZE).order(ByteOrder.LITTLE_ENDIAN);

		this.callStackByteBuffer = ByteBuffer.allocate(this.numCallStackPages * CALL_STACK_PAGE_SIZE).order(ByteOrder.LITTLE_ENDIAN);
		this.callStackByteBuffer.position(this.callStackByteBuffer.limit()); // Downward-growing stack, so start at the end

		this.userStackByteBuffer = ByteBuffer.allocate(this.numUserStackPages * USER_STACK_PAGE_SIZE).order(ByteOrder.LITTLE_ENDIAN);
		this.userStackByteBuffer.position(this.userStackByteBuffer.limit()); // Downward-growing stack, so start at the end

		this.api = api;
		this.currentBlockHeight = api.getCurrentBlockHeight();
		this.steps = 0;
		this.logger = logger;
	}

	/** For creating a new machine state */
	public MachineState(API api, LoggerInterface logger, byte[] headerBytes, byte[] codeBytes, byte[] dataBytes) {
		this(api, logger, headerBytes);

		// XXX: Why don't we simply ByteBuffer.wrap(codeBytes) as they're read-only?
		// This would do away with the need to specify numCodePages, save space and provide automatic end-of-code detection during execution thanks to
		// ByteBuffer's BufferUnderflowException

		if (codeBytes.length > this.numCodePages * CODE_PAGE_SIZE)
			throw new IllegalArgumentException("Number of code pages too small to hold code bytes");

		if (dataBytes.length > this.numDataPages * DATA_PAGE_SIZE)
			throw new IllegalArgumentException("Number of data pages too small to hold data bytes");

		System.arraycopy(codeBytes, 0, this.codeByteBuffer.array(), 0, codeBytes.length);

		System.arraycopy(dataBytes, 0, this.dataByteBuffer.array(), 0, dataBytes.length);

		this.programCounter = 0;
		this.onStopAddress = 0;
		this.onErrorAddress = null;
		this.isSleeping = false;
		this.sleepUntilHeight = null;
		this.isStopped = false;
		this.isFinished = false;
		this.hadFatalError = false;
		this.isFrozen = false;
		this.frozenBalance = null;
	}

	/** For serializing a machine state */
	public byte[] toBytes() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		try {
			// Header first
			bytes.write(this.headerBytes);

			// Code
			bytes.write(this.codeByteBuffer.array());

			// Data
			bytes.write(this.dataByteBuffer.array());

			// Call stack length (32bit unsigned int)
			int callStackLength = this.callStackByteBuffer.limit() - this.callStackByteBuffer.position();
			bytes.write(toByteArray(callStackLength));
			// Call stack
			bytes.write(this.callStackByteBuffer.array(), this.callStackByteBuffer.position(), callStackLength);

			// User stack length (32bit unsigned int)
			int userStackLength = this.userStackByteBuffer.limit() - this.userStackByteBuffer.position();
			bytes.write(toByteArray(userStackLength));
			// User stack
			bytes.write(this.userStackByteBuffer.array(), this.userStackByteBuffer.position(), userStackLength);

			// Actual state
			bytes.write(toByteArray(this.programCounter));
			bytes.write(toByteArray(this.onStopAddress));

			// Various flags
			Flags flags = new Flags();
			flags.push(this.isSleeping);
			flags.push(this.isStopped);
			flags.push(this.isFinished);
			flags.push(this.hadFatalError);
			flags.push(this.isFrozen);

			flags.push(this.onErrorAddress != null); // has onErrorAddress?
			flags.push(this.sleepUntilHeight != null); // has sleepUntilHeight?
			flags.push(this.frozenBalance != null); // has frozenBalance?

			boolean hasNonZeroA = this.a1 != 0 || this.a2 != 0 || this.a3 != 0 || this.a4 != 0;
			flags.push(hasNonZeroA);

			boolean hasNonZeroB = this.b1 != 0 || this.b2 != 0 || this.b3 != 0 || this.b4 != 0;
			flags.push(hasNonZeroB);

			bytes.write(toByteArray(flags.intValue()));

			// Optional flag-indicated extra info in same order as above
			if (this.onErrorAddress != null)
				bytes.write(toByteArray(this.onErrorAddress));

			if (this.sleepUntilHeight != null)
				bytes.write(toByteArray(this.sleepUntilHeight));

			if (this.frozenBalance != null)
				bytes.write(toByteArray(this.frozenBalance));

			if (hasNonZeroA) {
				bytes.write(toByteArray(this.a1));
				bytes.write(toByteArray(this.a2));
				bytes.write(toByteArray(this.a3));
				bytes.write(toByteArray(this.a4));
			}

			if (hasNonZeroB) {
				bytes.write(toByteArray(this.b1));
				bytes.write(toByteArray(this.b2));
				bytes.write(toByteArray(this.b3));
				bytes.write(toByteArray(this.b4));
			}
		} catch (IOException e) {
			return null;
		}

		return bytes.toByteArray();
	}

	/** For restoring a previously serialized machine state */
	public static MachineState fromBytes(API api, LoggerInterface logger, byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

		byte[] headerBytes = new byte[HEADER_LENGTH];
		byteBuffer.get(headerBytes);

		MachineState state = new MachineState(api, logger, headerBytes);

		byte[] codeBytes = new byte[state.codeByteBuffer.capacity()];
		byteBuffer.get(codeBytes);
		System.arraycopy(codeBytes, 0, state.codeByteBuffer.array(), 0, codeBytes.length);

		byte[] dataBytes = new byte[state.dataByteBuffer.capacity()];
		byteBuffer.get(dataBytes);
		System.arraycopy(dataBytes, 0, state.dataByteBuffer.array(), 0, dataBytes.length);

		int callStackLength = byteBuffer.getInt();
		byte[] callStackBytes = new byte[callStackLength];
		byteBuffer.get(callStackBytes);
		// Restore call stack pointer, and useful for copy below
		state.callStackByteBuffer.position(state.callStackByteBuffer.limit() - callStackLength);
		// Call stack grows downwards so copy to end
		System.arraycopy(callStackBytes, 0, state.callStackByteBuffer.array(), state.callStackByteBuffer.position(), callStackLength);

		int userStackLength = byteBuffer.getInt();
		byte[] userStackBytes = new byte[userStackLength];
		byteBuffer.get(userStackBytes);
		// Restore user stack pointer, and useful for copy below
		state.userStackByteBuffer.position(state.userStackByteBuffer.limit() - userStackLength);
		// User stack grows downwards so copy to end
		System.arraycopy(userStackBytes, 0, state.userStackByteBuffer.array(), state.userStackByteBuffer.position(), userStackLength);

		// Actual state
		state.programCounter = byteBuffer.getInt();
		state.onStopAddress = byteBuffer.getInt();

		// Various flags (reverse order to toBytes)
		Flags flags = state.new Flags(byteBuffer.getInt());
		boolean hasNonZeroB = flags.pop();
		boolean hasNonZeroA = flags.pop();
		boolean hasFrozenBalance = flags.pop();
		boolean hasSleepUntilHeight = flags.pop();
		boolean hasOnErrorAddress = flags.pop();

		state.isFrozen = flags.pop();
		state.hadFatalError = flags.pop();
		state.isFinished = flags.pop();
		state.isStopped = flags.pop();
		state.isSleeping = flags.pop();

		// Optional extras (same order as toBytes)
		if (hasOnErrorAddress)
			state.onErrorAddress = byteBuffer.getInt();

		if (hasSleepUntilHeight)
			state.sleepUntilHeight = byteBuffer.getInt();

		if (hasFrozenBalance)
			state.frozenBalance = byteBuffer.getLong();

		if (hasNonZeroA) {
			state.a1 = byteBuffer.getLong();
			state.a2 = byteBuffer.getLong();
			state.a3 = byteBuffer.getLong();
			state.a4 = byteBuffer.getLong();
		}

		if (hasNonZeroB) {
			state.b1 = byteBuffer.getLong();
			state.b2 = byteBuffer.getLong();
			state.b3 = byteBuffer.getLong();
			state.b4 = byteBuffer.getLong();
		}

		return state;
	}

	/** Convert int to little-endian byte array */
	private byte[] toByteArray(int value) {
		return new byte[] { (byte) (value), (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24) };
	}

	/** Convert long to little-endian byte array */
	private byte[] toByteArray(long value) {
		return new byte[] { (byte) (value), (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24), (byte) (value >> 32), (byte) (value >> 40),
				(byte) (value >> 48), (byte) (value >> 56) };
	}

	private void parseHeader() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(this.headerBytes);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		this.version = byteBuffer.getShort();
		if (this.version < 1)
			throw new IllegalArgumentException("Version must be >= 0");

		this.reserved = byteBuffer.getShort();

		this.numCodePages = byteBuffer.getShort();
		if (this.numCodePages < 1)
			throw new IllegalArgumentException("Number of code pages must be > 0");

		this.numDataPages = byteBuffer.getShort();
		if (this.numDataPages < 1)
			throw new IllegalArgumentException("Number of data pages must be > 0");

		this.numCallStackPages = byteBuffer.getShort();
		if (this.numCallStackPages < 0)
			throw new IllegalArgumentException("Number of call stack pages must be >= 0");

		this.numUserStackPages = byteBuffer.getShort();
		if (this.numUserStackPages < 0)
			throw new IllegalArgumentException("Number of user stack pages must be >= 0");
	}

	public void execute() {
		// Set byte buffer position using program counter
		codeByteBuffer.position(this.programCounter);

		// Reset for this round of execution
		this.isSleeping = false;
		this.sleepUntilHeight = null;
		this.isStopped = false;
		this.isFrozen = false;
		this.frozenBalance = null;
		this.steps = 0;

		while (!this.isSleeping && !this.isStopped && !this.isFinished && !this.isFrozen) {
			byte rawOpCode = codeByteBuffer.get();
			OpCode nextOpCode = OpCode.valueOf(rawOpCode);

			try {
				if (nextOpCode == null)
					throw new IllegalOperationException("OpCode 0x" + String.format("%02x", rawOpCode) + " not recognised");

				this.logger.debug("[PC: " + String.format("%04x", this.programCounter) + "] " + nextOpCode.name());

				nextOpCode.execute(codeByteBuffer, dataByteBuffer, userStackByteBuffer, callStackByteBuffer, this);
				this.programCounter = codeByteBuffer.position();
			} catch (ExecutionException e) {
				this.logger.debug("Error at PC " + String.format("%04x", this.programCounter) + ": " + e.getMessage());

				if (this.onErrorAddress == null) {
					this.isFinished = true;
					this.hadFatalError = true;
					this.api.onFatalError(this, e);
					break;
				}

				this.programCounter = this.onErrorAddress;
				codeByteBuffer.position(this.programCounter);
			}

			++this.steps;
		}

		if (this.isStopped) {
			this.logger.debug("Setting program counter to stop address: " + String.format("%04x", this.onStopAddress));
			this.programCounter = this.onStopAddress;
		}
	}

	// public String disassemble(List<String> dataLabels, Map<Integer, String> codeLabels) {
	public String disassemble() throws ExecutionException {
		String output = "";

		codeByteBuffer.position(0);

		while (codeByteBuffer.hasRemaining()) {
			byte rawOpCode = codeByteBuffer.get();
			if (rawOpCode == 0)
				continue;

			OpCode nextOpCode = OpCode.valueOf(rawOpCode);
			if (nextOpCode == null)
				throw new IllegalOperationException("OpCode 0x" + String.format("%02x", rawOpCode) + " not recognised");

			if (!output.isEmpty())
				output += "\n";

			output += "[PC: " + String.format("%04x", codeByteBuffer.position() - 1) + "] " + nextOpCode.disassemble(codeByteBuffer, dataByteBuffer);
		}

		return output;
	}

}
