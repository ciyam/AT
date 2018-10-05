package org.ciyam.at;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This enum contains op codes for the CIYAM AT machine.
 * <p>
 * Op codes are represented by a single byte and maybe be followed by additional arguments like data addresses, offset, immediate values, etc.
 * <p>
 * OpCode instances can be obtained via the default <tt>OpCode.valueOf(String)</tt> or the additional <tt>OpCode.valueOf(int)</tt>.
 * <p>
 * Use the <tt>OpCode.execute</tt> method to perform the operation.
 * <p>
 * In the documentation for each OpCode:
 * <p>
 * <tt>@addr</tt> means "store at <tt>addr</tt>"
 * <p>
 * <tt>$addr</tt> means "fetch from <tt>addr</tt>"
 * <p>
 * <tt>@($addr)</tt> means "store at address fetched from <tt>addr</tt>", i.e. indirect
 * <p>
 * <tt>$($addr1 + $addr2)</tt> means "fetch from address fetched from <tt>addr1</tt> plus offset fetched from <tt>addr2</tt>", i.e. indirect indexed
 * 
 * @see OpCode#valueOf(int)
 * @see OpCode#executeWithParams(MachineState, Object...)
 */
public enum OpCode {

	/**
	 * <b>N</b>o <b>OP</b>eration<br>
	 * <tt>0x7f</tt><br>
	 * (Does nothing)
	 */
	NOP(0x7f) {
		@Override
		public void executeWithParams(MachineState state, Object... args) {
			// Do nothing
		}
	},
	/**
	 * <b>SET</b> <b>VAL</b>ue<br>
	 * <tt>0x01 addr value</tt><br>
	 * <tt>@addr = value</tt>
	 */
	SET_VAL(0x01, OpCodeParam.DEST_ADDR, OpCodeParam.VALUE) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];
			long value = (long) args[1];

			state.dataByteBuffer.putLong(address, value);
		}
	},
	/**
	 * <b>SET</b> <b>DAT</b>a<br>
	 * <tt>0x02 addr1 addr2</tt><br>
	 * <tt>@addr1 = $addr2</tt>
	 */
	SET_DAT(0x02, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address1 = (int) args[0];
			int address2 = (int) args[1];

			long value = state.dataByteBuffer.getLong(address2);
			state.dataByteBuffer.putLong(address1, value);
		}
	},
	/**
	 * <b>CL</b>ea<b>R</b> <b>DAT</b>a<br>
	 * <tt>0x03 addr</tt><br>
	 * <tt>@addr = 0</tt>
	 */
	CLR_DAT(0x03, OpCodeParam.DEST_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			state.dataByteBuffer.putLong(address, 0L);
		}
	},
	/**
	 * <b>INC</b>rement <b>DAT</b>a<br>
	 * <tt>0x04 addr</tt><br>
	 * <tt>@addr += 1</tt>
	 */
	INC_DAT(0x04, OpCodeParam.DEST_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			long value = state.dataByteBuffer.getLong(address);
			state.dataByteBuffer.putLong(address, value + 1);
		}
	},
	/**
	 * <b>DEC</b>rement <b>DAT</b>a<br>
	 * <tt>0x05 addr</tt><br>
	 * <tt>@addr -= 1</tt>
	 */
	DEC_DAT(0x05, OpCodeParam.DEST_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			long value = state.dataByteBuffer.getLong(address);
			state.dataByteBuffer.putLong(address, value - 1);
		}
	},
	/**
	 * <b>ADD</b> <b>DAT</b>a<br>
	 * <tt>0x06 addr1 addr2</tt><br>
	 * <tt>@addr1 += $addr2</tt>
	 */
	ADD_DAT(0x06, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeDataOperation(state, (a, b) -> a + b, args);
		}
	},
	/**
	 * <b>SUB</b>tract <b>DAT</b>a<br>
	 * <tt>0x07 addr1 addr2</tt><br>
	 * <tt>@addr1 -= $addr2</tt>
	 */
	SUB_DAT(0x07, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeDataOperation(state, (a, b) -> a - b, args);
		}
	},
	/**
	 * <b>MUL</b>tiply <b>DAT</b>a<br>
	 * <tt>0x08 addr1 addr2</tt><br>
	 * <tt>@addr1 *= $addr2</tt>
	 */
	MUL_DAT(0x08, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeDataOperation(state, (a, b) -> a * b, args);
		}
	},
	/**
	 * <b>DIV</b>ide <b>DAT</b>a<br>
	 * <tt>0x09 addr1 addr2</tt><br>
	 * <tt>@addr1 /= $addr2</tt><br>
	 * Can also throw <tt>IllegealOperationException</tt> if divide-by-zero attempted.
	 */
	DIV_DAT(0x09, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			try {
				executeDataOperation(state, (a, b) -> a / b, args);
			} catch (ArithmeticException e) {
				throw new IllegalOperationException("Divide by zero", e);
			}
		}
	},
	/**
	 * <b>B</b>inary-<b>OR</b> <b>DAT</b>a<br>
	 * <tt>0x0a addr1 addr2</tt><br>
	 * <tt>@addr1 |= $addr2</tt>
	 */
	BOR_DAT(0x0a, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeDataOperation(state, (a, b) -> a | b, args);
		}
	},
	/**
	 * Binary-<b>AND</b> <b>DAT</b>a<br>
	 * <tt>0x0b addr1 addr2</tt><br>
	 * <tt>@addr1 &= $addr2</tt>
	 */
	AND_DAT(0x0b, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeDataOperation(state, (a, b) -> a & b, args);
		}
	},
	/**
	 * E<b>X</b>clusive <b>OR</b> <b>DAT</b>a<br>
	 * <tt>0x0c addr1 addr2</tt><br>
	 * <tt>@addr1 ^= $addr2</tt>
	 */
	XOR_DAT(0x0c, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeDataOperation(state, (a, b) -> a ^ b, args);
		}
	},
	/**
	 * Bitwise-<b>NOT</b> <b>DAT</b>a<br>
	 * <tt>0x0d addr</tt><br>
	 * <tt>@addr = ~$addr</tt>
	 */
	NOT_DAT(0x0d, OpCodeParam.DEST_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			long value = state.dataByteBuffer.getLong(address);
			state.dataByteBuffer.putLong(address, ~value);
		}
	},
	/**
	 * <b>SET</b> using <b>IND</b>irect data<br>
	 * <tt>0x0e addr1 addr2</tt><br>
	 * <tt>@addr1 = $($addr2)</tt>
	 */
	SET_IND(0x0e, OpCodeParam.DEST_ADDR, OpCodeParam.INDIRECT_SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address1 = (int) args[0];
			int address2 = (int) args[1];

			long address3 = state.dataByteBuffer.getLong(address2) * MachineState.VALUE_SIZE;

			if (address3 < 0 || address3 + MachineState.VALUE_SIZE >= state.dataByteBuffer.limit())
				throw new InvalidAddressException("Data address out of bounds");

			long value = state.dataByteBuffer.getLong((int) address3);
			state.dataByteBuffer.putLong(address1, value);
		}
	},
	/**
	 * <b>SET</b> using indirect <b>I</b>n<b>D</b>e<b>X</b>ed data<br>
	 * <tt>0x0f addr1 addr2 addr3</tt><br>
	 * <tt>@addr1 = $($addr2 + $addr3)</tt>
	 */
	SET_IDX(0x0f, OpCodeParam.DEST_ADDR, OpCodeParam.INDIRECT_SRC_ADDR_WITH_INDEX, OpCodeParam.INDEX) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address1 = (int) args[0];
			int address2 = (int) args[1];
			int address3 = (int) args[2];

			long baseAddress = state.dataByteBuffer.getLong(address2) * MachineState.VALUE_SIZE;
			long offset = state.dataByteBuffer.getLong(address3) * MachineState.VALUE_SIZE;

			long newAddress = baseAddress + offset;

			if (newAddress < 0 || newAddress + MachineState.VALUE_SIZE >= state.dataByteBuffer.limit())
				throw new InvalidAddressException("Data address out of bounds");

			long value = state.dataByteBuffer.getLong((int) newAddress);
			state.dataByteBuffer.putLong(address1, value);
		}
	},
	/**
	 * <b>P</b>u<b>SH</b> <b>DAT</b>a onto user stack<br>
	 * <tt>0x10 addr</tt><br>
	 * <tt>@--user_stack = $addr</tt><br>
	 * Can also throw <tt>StackBoundsException</tt> if user stack exhausted.
	 */
	PSH_DAT(0x10, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			long value = state.dataByteBuffer.getLong(address);

			try {
				// Simulate backwards-walking stack
				int newPosition = state.userStackByteBuffer.position() - MachineState.VALUE_SIZE;
				state.userStackByteBuffer.putLong(newPosition, value);
				state.userStackByteBuffer.position(newPosition);
			} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
				throw new StackBoundsException("No room on user stack to push data", e);
			}
		}
	},
	/**
	 * <b>POP</b> <b>DAT</b>a from user stack<br>
	 * <tt>0x11 addr</tt><br>
	 * <tt>@addr = $user_stack++</tt><br>
	 * Can also throw <tt>StackBoundsException</tt> if user stack empty.
	 */
	POP_DAT(0x11, OpCodeParam.DEST_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			try {
				long value = state.userStackByteBuffer.getLong();

				// Clear old stack entry
				state.userStackByteBuffer.putLong(state.userStackByteBuffer.position() - MachineState.VALUE_SIZE, 0L);

				// Put popped value into data address
				state.dataByteBuffer.putLong(address, value);
			} catch (BufferUnderflowException e) {
				throw new StackBoundsException("Empty user stack from which to pop data", e);
			}
		}
	},
	/**
	 * <b>J</b>u<b>MP</b> into <b>SUB</b>routine<br>
	 * <tt>0x12 addr</tt><br>
	 * <tt>@--call_stack = PC after opcode & args</tt>, <tt>PC = addr</tt><br>
	 * Can also throw <tt>StackBoundsException</tt> if call stack exhausted.
	 */
	JMP_SUB(0x12, OpCodeParam.CODE_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			try {
				// Simulate backwards-walking stack
				int newPosition = state.callStackByteBuffer.position() - MachineState.ADDRESS_SIZE;
				state.callStackByteBuffer.putInt(newPosition, state.codeByteBuffer.position());
				state.callStackByteBuffer.position(newPosition);
			} catch (IndexOutOfBoundsException | IllegalArgumentException e) {
				throw new StackBoundsException("No room on call stack to call subroutine", e);
			}

			state.codeByteBuffer.position(address);
		}
	},
	/**
	 * <b>RET</b>urn from <b>SUB</b>routine<br>
	 * <tt>0x13<br>
	 * <tt>PC = $call_stack++</tt><br>
	 * Can also throw <tt>StackBoundsException</tt> if call stack empty.
	 */
	RET_SUB(0x13) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			try {
				int returnAddress = state.callStackByteBuffer.getInt();

				// Clear old stack entry
				state.callStackByteBuffer.putInt(state.callStackByteBuffer.position() - MachineState.ADDRESS_SIZE, 0);

				state.codeByteBuffer.position(returnAddress);
			} catch (BufferUnderflowException e) {
				throw new StackBoundsException("Empty call stack missing return address from subroutine", e);
			}
		}
	},
	/**
	 * Store <b>IND</b>irect <b>DAT</b>a<br>
	 * <tt>0x14 addr1 addr2<br>
	 * <tt>@($addr1) = $addr2</tt>
	 */
	IND_DAT(0x14, OpCodeParam.INDIRECT_DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address1 = (int) args[0];
			int address2 = (int) args[1];

			long address3 = state.dataByteBuffer.getLong(address1) * MachineState.VALUE_SIZE;

			if (address3 < 0 || address3 + MachineState.VALUE_SIZE >= state.dataByteBuffer.limit())
				throw new InvalidAddressException("Data address out of bounds");

			long value = state.dataByteBuffer.getLong(address2);
			state.dataByteBuffer.putLong((int) address3, value);
		}
	},
	/**
	 * Store indirect <b>I</b>n<b>D</b>e<b>X</b>ed <b>DAT</b>a<br>
	 * <tt>0x15 addr1 addr2<br>
	 * <tt>@($addr1 + $addr2) = $addr3</tt>
	 */
	IDX_DAT(0x15, OpCodeParam.INDIRECT_DEST_ADDR_WITH_INDEX, OpCodeParam.INDEX, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address1 = (int) args[0];
			int address2 = (int) args[1];
			int address3 = (int) args[2];

			long baseAddress = state.dataByteBuffer.getLong(address1) * MachineState.VALUE_SIZE;
			long offset = state.dataByteBuffer.getLong(address2) * MachineState.VALUE_SIZE;

			long newAddress = baseAddress + offset;

			if (newAddress < 0 || newAddress + MachineState.VALUE_SIZE >= state.dataByteBuffer.limit())
				throw new InvalidAddressException("Data address out of bounds");

			long value = state.dataByteBuffer.getLong(address3);
			state.dataByteBuffer.putLong((int) newAddress, value);
		}
	},
	/**
	 * <b>MOD</b>ulo <b>DAT</b>a<br>
	 * <tt>0x16 addr1 addr2</tt><br>
	 * <tt>@addr1 %= $addr2</tt>
	 */
	MOD_DAT(0x16, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			try {
				executeDataOperation(state, (a, b) -> a % b, args);
			} catch (ArithmeticException e) {
				throw new IllegalOperationException("Divide by zero", e);
			}
		}
	},
	/**
	 * <b>SH</b>ift <b>L</b>eft <b>DAT</b>a<br>
	 * <tt>0x17 addr1 addr2</tt><br>
	 * <tt>@addr1 <<= $addr2</tt>
	 */
	SHL_DAT(0x17, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		private static final long MAX_SHIFT = MachineState.VALUE_SIZE * 8;

		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			// If 2nd arg is more than value size (in bits) then return 0 to simulate all bits being shifted out of existence
			executeDataOperation(state, (a, b) -> b >= MAX_SHIFT ? 0 : a << b, args);
		}
	},
	/**
	 * <b>SH</b>ift <b>R</b>ight <b>DAT</b>a<br>
	 * <tt>0x18 addr1 addr2</tt><br>
	 * <tt>@addr1 >>= $addr2</tt><br>
	 * Note: new MSB bit will be zero
	 */
	SHR_DAT(0x18, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		private static final long MAX_SHIFT = MachineState.VALUE_SIZE * 8;

		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			// If 2nd arg is more than value size (in bits) then return 0 to simulate all bits being shifted out of existence
			executeDataOperation(state, (a, b) -> b >= MAX_SHIFT ? 0 : a >>> b, args);
		}
	},
	/**
	 * <b>J</b>u<b>MP</b> to <b>AD</b>d<b>R</b>ess<br>
	 * <tt>0x1a addr</tt><br>
	 * <tt>PC = addr</tt>
	 */
	JMP_ADR(0x1a, OpCodeParam.CODE_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			state.codeByteBuffer.position(address);
		}
	},
	/**
	 * <b>B</b>ranch if <b>Z</b>e<b>R</b>o<br>
	 * <tt>0x1b addr offset</tt><br>
	 * <tt>if ($addr == 0) PC += offset</tt><br>
	 * Note: <tt>PC</tt> is considered to be immediately before opcode byte.
	 */
	BZR_DAT(0x1b, OpCodeParam.SRC_ADDR, OpCodeParam.OFFSET) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];
			byte offset = (byte) args[1];

			int branchTarget = state.getProgramCounter() + offset;

			if (branchTarget < 0 || branchTarget >= state.codeByteBuffer.limit())
				throw new InvalidAddressException("branch target out of bounds");

			long value = state.dataByteBuffer.getLong(address);

			if (value == 0)
				state.codeByteBuffer.position(branchTarget);
		}
	},
	/**
	 * <b>B</b>ranch if <b>N</b>ot <b>Z</b>ero<br>
	 * <tt>0x1e addr offset</tt><br>
	 * <tt>if ($addr != 0) PC += offset</tt><br>
	 * Note: <tt>PC</tt> is considered to be immediately before opcode byte.
	 */
	BNZ_DAT(0x1e, OpCodeParam.SRC_ADDR, OpCodeParam.OFFSET) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];
			byte offset = (byte) args[1];

			int branchTarget = state.getProgramCounter() + offset;

			if (branchTarget < 0 || branchTarget >= state.codeByteBuffer.limit())
				throw new InvalidAddressException("branch target out of bounds");

			long value = state.dataByteBuffer.getLong(address);

			if (value != 0)
				state.codeByteBuffer.position(branchTarget);
		}
	},
	/**
	 * <b>B</b>ranch if <b>G</b>reater-<b>T</b>han <b>DAT</b>a<br>
	 * <tt>0x1f addr1 addr2 offset</tt><br>
	 * <tt>if ($addr1 > $addr2) PC += offset</tt><br>
	 * Note: <tt>PC</tt> is considered to be immediately before opcode byte.
	 */
	BGT_DAT(0x1f, OpCodeParam.SRC_ADDR, OpCodeParam.SRC_ADDR, OpCodeParam.OFFSET) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeBranchConditional(state, (a, b) -> a > b, args);
		}
	},
	/**
	 * <b>B</b>ranch if <b>L</b>ess-<b>T</b>han <b>DAT</b>a<br>
	 * <tt>0x20 addr1 addr2 offset</tt><br>
	 * <tt>if ($addr1 < $addr2) PC += offset</tt><br>
	 * Note: <tt>PC</tt> is considered to be immediately before opcode byte.
	 */
	BLT_DAT(0x20, OpCodeParam.SRC_ADDR, OpCodeParam.SRC_ADDR, OpCodeParam.OFFSET) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeBranchConditional(state, (a, b) -> a < b, args);
		}
	},
	/**
	 * <b>B</b>ranch if <b>G</b>reater-or-<b>E</b>qual <b>DAT</b>a<br>
	 * <tt>0x21 addr1 addr2 offset</tt><br>
	 * <tt>if ($addr1 >= $addr2) PC += offset</tt><br>
	 * Note: <tt>PC</tt> is considered to be immediately before opcode byte.
	 */
	BGE_DAT(0x21, OpCodeParam.SRC_ADDR, OpCodeParam.SRC_ADDR, OpCodeParam.OFFSET) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeBranchConditional(state, (a, b) -> a >= b, args);
		}
	},
	/**
	 * <b>B</b>ranch if <b>L</b>ess-or-<b>E</b>qual <b>DAT</b>a<br>
	 * <tt>0x22 addr1 addr2 offset</tt><br>
	 * <tt>if ($addr1 <= $addr2) PC += offset</tt><br>
	 * Note: <tt>PC</tt> is considered to be immediately before opcode byte.
	 */
	BLE_DAT(0x22, OpCodeParam.SRC_ADDR, OpCodeParam.SRC_ADDR, OpCodeParam.OFFSET) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeBranchConditional(state, (a, b) -> a <= b, args);
		}
	},
	/**
	 * <b>B</b>ranch if <b>EQ</b>ual <b>DAT</b>a<br>
	 * <tt>0x23 addr1 addr2 offset</tt><br>
	 * <tt>if ($addr1 == $addr2) PC += offset</tt><br>
	 * Note: <tt>PC</tt> is considered to be immediately before opcode byte.
	 */
	BEQ_DAT(0x23, OpCodeParam.SRC_ADDR, OpCodeParam.SRC_ADDR, OpCodeParam.OFFSET) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeBranchConditional(state, (a, b) -> a == b, args);
		}
	},
	/**
	 * <b>B</b>ranch if <b>N</b>ot-<b>E</b>qual <b>DAT</b>a<br>
	 * <tt>0x24 addr1 addr2 offset</tt><br>
	 * <tt>if ($addr1 != $addr2) PC += offset</tt><br>
	 * Note: <tt>PC</tt> is considered to be immediately before opcode byte.
	 */
	BNE_DAT(0x24, OpCodeParam.SRC_ADDR, OpCodeParam.SRC_ADDR, OpCodeParam.OFFSET) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			executeBranchConditional(state, (a, b) -> a != b, args);
		}
	},
	/**
	 * <b>SL</b>ee<b>P</b> until <b>DAT</b>a<br>
	 * <tt>0x25 addr</tt><br>
	 * <tt>sleep until $addr, then carry on from current PC</tt><br>
	 * Note: The value from <tt>$addr</tt> is considered to be a block height.
	 */
	SLP_DAT(0x25, OpCodeParam.BLOCK_HEIGHT) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			long value = state.codeByteBuffer.getLong(address);

			state.setSleepUntilHeight((int) value);
			state.setIsSleeping(true);
		}
	},
	/**
	 * <b>FI</b>nish if <b>Z</b>ero <b>DAT</b>a<br>
	 * <tt>0x26 addr</tt><br>
	 * <tt>if ($addr == 0) permanently stop</tt>
	 */
	FIZ_DAT(0x26, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			long value = state.dataByteBuffer.getLong(address);

			if (value == 0)
				state.setIsFinished(true);
		}
	},
	/**
	 * <b>ST</b>op if <b>Z</b>ero <b>DAT</b>a<br>
	 * <tt>0x27 addr</tt><br>
	 * <tt>if ($addr == 0) PC = PCS and stop</tt>
	 */
	STZ_DAT(0x27, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			long value = state.dataByteBuffer.getLong(address);

			if (value == 0) {
				state.codeByteBuffer.position(state.getOnStopAddress());
				state.setIsStopped(true);
			}
		}
	},
	/**
	 * <b>FIN</b>ish <b>IM</b>me<b>D</b>iately<br>
	 * <tt>0x28</tt><br>
	 * <tt>permanently stop</tt>
	 */
	FIN_IMD(0x28) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			state.setIsFinished(true);
		}
	},
	/**
	 * <b>ST</b>o<b>P</b> <b>IM</b>me<b>D</b>iately<br>
	 * <tt>0x29</tt><br>
	 * <tt>stop</tt>
	 */
	STP_IMD(0x29) {
		@Override
		public void executeWithParams(MachineState state, Object... args) {
			state.setIsStopped(true);
		}
	},
	/**
	 * <b>SL</b>ee<b>P</b> <b>IM</b>me<b>D</b>iately<br>
	 * <tt>0x2a</tt><br>
	 * <tt>sleep until next block, then carry on from current PC</tt>
	 */
	SLP_IMD(0x2a) {
		@Override
		public void executeWithParams(MachineState state, Object... args) {
			state.setSleepUntilHeight(state.getCurrentBlockHeight() + 1);
			state.setIsSleeping(true);
		}
	},
	/**
	 * Set <b>ERR</b>or <b>AD</b>d<b>R</b>ess<br>
	 * <tt>0x2b addr</tt><br>
	 * <tt>PCE = addr</tt>
	 */
	ERR_ADR(0x2b, OpCodeParam.CODE_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			int address = (int) args[0];

			state.setOnErrorAddress(address);
		}
	},
	/**
	 * <b>SET</b> <b>PCS</b> (stop address)<br>
	 * <tt>0x30</tt><br>
	 * <tt>PCS = PC</tt><br>
	 * Note: <tt>PC</tt> is considered to be immediately after this opcode byte.
	 */
	SET_PCS(0x30) {
		@Override
		public void executeWithParams(MachineState state, Object... args) {
			state.setOnStopAddress(state.codeByteBuffer.position());
		}
	},
	/**
	 * Call <b>EXT</b>ernal <b>FUN</b>ction<br>
	 * <tt>0x32 func</tt><br>
	 * <tt>func()</tt>
	 */
	EXT_FUN(0x32, OpCodeParam.FUNC) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			short rawFunctionCode = (short) args[0];

			FunctionCode functionCode = FunctionCode.valueOf(rawFunctionCode);

			if (functionCode == null)
				throw new IllegalFunctionCodeException("Unknown function code 0x" + String.format("%04x", rawFunctionCode) + " encountered at EXT_FUN");

			functionCode.preExecuteCheck(0, false, state, rawFunctionCode);

			FunctionData functionData = new FunctionData(false);

			functionCode.execute(functionData, state, rawFunctionCode);
		}
	},
	/**
	 * Call <b>EXT</b>ernal <b>FUN</b>ction with <b>DAT</b>a<br>
	 * <tt>0x33 func addr</tt><br>
	 * <tt>func($addr)</tt>
	 */
	EXT_FUN_DAT(0x33, OpCodeParam.FUNC, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			short rawFunctionCode = (short) args[0];
			int address = (int) args[1];

			FunctionCode functionCode = FunctionCode.valueOf(rawFunctionCode);

			if (functionCode == null)
				throw new IllegalFunctionCodeException("Unknown function code 0x" + String.format("%04x", rawFunctionCode) + " encountered at EXT_FUN_DAT");

			functionCode.preExecuteCheck(1, false, state, rawFunctionCode);

			long value = state.dataByteBuffer.getLong(address);

			FunctionData functionData = new FunctionData(value, false);

			functionCode.execute(functionData, state, rawFunctionCode);
		}
	},
	/**
	 * Call <b>EXT</b>ernal <b>FUN</b>ction with <b>DAT</b>a x<b>2</b><br>
	 * <tt>0x34 func addr1 addr2</tt><br>
	 * <tt>func($addr1, $addr2)</tt>
	 */
	EXT_FUN_DAT_2(0x34, OpCodeParam.FUNC, OpCodeParam.SRC_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			short rawFunctionCode = (short) args[0];
			int address1 = (int) args[1];
			int address2 = (int) args[2];

			FunctionCode functionCode = FunctionCode.valueOf(rawFunctionCode);

			if (functionCode == null)
				throw new IllegalFunctionCodeException("Unknown function code 0x" + String.format("%04x", rawFunctionCode) + " encountered at EXT_FUN_DAT_2");

			functionCode.preExecuteCheck(2, false, state, rawFunctionCode);

			long value1 = state.dataByteBuffer.getLong(address1);
			long value2 = state.dataByteBuffer.getLong(address2);

			FunctionData functionData = new FunctionData(value1, value2, false);

			functionCode.execute(functionData, state, rawFunctionCode);
		}
	},
	/**
	 * Call <b>EXT</b>ernal <b>FUN</b>ction expecting <b>RET</b>urn value<br>
	 * <tt>0x35 func addr</tt><br>
	 * <tt>@addr = func()</tt>
	 */
	EXT_FUN_RET(0x35, OpCodeParam.FUNC, OpCodeParam.DEST_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			short rawFunctionCode = (short) args[0];
			int address = (int) args[1];

			FunctionCode functionCode = FunctionCode.valueOf(rawFunctionCode);

			if (functionCode == null)
				throw new IllegalFunctionCodeException("Unknown function code 0x" + String.format("%04x", rawFunctionCode) + " encountered at EXT_FUN_RET");

			functionCode.preExecuteCheck(0, true, state, rawFunctionCode);

			FunctionData functionData = new FunctionData(true);

			functionCode.execute(functionData, state, rawFunctionCode);

			if (functionData.returnValue == null)
				throw new ExecutionException("Function failed to return a value as expected of EXT_FUN_RET");

			state.dataByteBuffer.putLong(address, functionData.returnValue);
		}
	},
	/**
	 * Call <b>EXT</b>ernal <b>FUN</b>ction expecting <b>RET</b>urn value with <b>DAT</b>a<br>
	 * <tt>0x36 func addr1 addr2</tt><br>
	 * <tt>@addr1 = func($addr2)</tt>
	 */
	EXT_FUN_RET_DAT(0x36, OpCodeParam.FUNC, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			short rawFunctionCode = (short) args[0];
			int address1 = (int) args[1];
			int address2 = (int) args[2];

			FunctionCode functionCode = FunctionCode.valueOf(rawFunctionCode);

			if (functionCode == null)
				throw new IllegalFunctionCodeException("Unknown function code 0x" + String.format("%04x", rawFunctionCode) + " encountered at EXT_FUN_RET_DAT");

			functionCode.preExecuteCheck(1, true, state, rawFunctionCode);

			long value = state.dataByteBuffer.getLong(address2);

			FunctionData functionData = new FunctionData(value, true);

			functionCode.execute(functionData, state, rawFunctionCode);

			if (functionData.returnValue == null)
				throw new ExecutionException("Function failed to return a value as expected of EXT_FUN_RET_DAT");

			state.dataByteBuffer.putLong(address1, functionData.returnValue);
		}
	},
	/**
	 * Call <b>EXT</b>ernal <b>FUN</b>ction expecting <b>RET</b>urn value with <b>DAT</b>a x<b>2</b><br>
	 * <tt>0x37 func addr1 addr2 addr3</tt><br>
	 * <tt>@addr1 = func($addr2, $addr3)</tt>
	 */
	EXT_FUN_RET_DAT_2(0x37, OpCodeParam.FUNC, OpCodeParam.DEST_ADDR, OpCodeParam.SRC_ADDR, OpCodeParam.SRC_ADDR) {
		@Override
		public void executeWithParams(MachineState state, Object... args) throws ExecutionException {
			short rawFunctionCode = (short) args[0];
			int address1 = (int) args[1];
			int address2 = (int) args[2];
			int address3 = (int) args[3];

			FunctionCode functionCode = FunctionCode.valueOf(rawFunctionCode);

			if (functionCode == null)
				throw new IllegalFunctionCodeException(
						"Unknown function code 0x" + String.format("%04x", rawFunctionCode) + " encountered at EXT_FUN_RET_DAT_2");

			functionCode.preExecuteCheck(2, true, state, rawFunctionCode);

			long value1 = state.dataByteBuffer.getLong(address2);
			long value2 = state.dataByteBuffer.getLong(address3);

			FunctionData functionData = new FunctionData(value1, value2, true);

			functionCode.execute(functionData, state, rawFunctionCode);

			if (functionData.returnValue == null)
				throw new ExecutionException("Function failed to return a value as expected of EXT_FUN_RET_DAT_2");

			state.dataByteBuffer.putLong(address1, functionData.returnValue);
		}
	};

	public final byte value;
	public final OpCodeParam[] params;

	// Create a map of opcode values to OpCode
	private final static Map<Byte, OpCode> map = Arrays.stream(OpCode.values()).collect(Collectors.toMap(opcode -> opcode.value, opcode -> opcode));

	private OpCode(int value, OpCodeParam... params) {
		this.value = (byte) value;
		this.params = params;
	}

	public static OpCode valueOf(int value) {
		return map.get((byte) value);
	}

	/**
	 * Execute OpCode with args fetched from code bytes
	 * <p>
	 * Assumes <tt>codeByteBuffer.position()</tt> is already placed immediately after opcode and params.<br>
	 * <tt>state.getProgramCounter()</tt> is available to return position immediately before opcode and params.
	 * <p>
	 * OpCode execution can modify <tt>codeByteBuffer.position()</tt> in cases like jumps, branches, etc.
	 * <p>
	 * Can also modify <tt>userStackByteBuffer</tt> and various fields of <tt>state</tt>.
	 * <p>
	 * Throws a subclass of <tt>ExecutionException</tt> on error, e.g. <tt>InvalidAddressException</tt>.
	 * 
	 * @param state
	 * @param args
	 * 
	 * @throws ExecutionException
	 */
	public abstract void executeWithParams(MachineState state, Object... args) throws ExecutionException;

	public void execute(MachineState state) throws ExecutionException {
		List<Object> args = new ArrayList<Object>();

		for (OpCodeParam param : this.params)
			args.add(param.fetch(state.codeByteBuffer, state.dataByteBuffer));

		this.executeWithParams(state, args.toArray());
	}

	/**
	 * Returns string representing disassembled OpCode and parameters
	 * 
	 * @param codeByteBuffer
	 * @param dataByteBuffer
	 * @return String
	 * @throws ExecutionException
	 */
	public String disassemble(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
		String output = this.name();

		int postOpcodeProgramCounter = codeByteBuffer.position();

		for (OpCodeParam param : this.params)
			output += " " + param.disassemble(codeByteBuffer, dataByteBuffer, postOpcodeProgramCounter);

		return output;
	}

	/**
	 * Common code for ADD_DAT/SUB_DAT/MUL_DAT/DIV_DAT/MOD_DAT/SHL_DAT/SHR_DAT
	 * 
	 * @param codeByteBuffer
	 * @param dataByteBuffer
	 * @param operator
	 *            - typically a lambda operating on two <tt>long</tt> params, e.g. <tt>(a, b) -> a + b</tt>
	 * @throws ExecutionException
	 */
	private static void executeDataOperation(MachineState state, TwoValueOperator operator, Object... args) throws ExecutionException {
		int address1 = (int) args[0];
		int address2 = (int) args[1];

		long value1 = state.dataByteBuffer.getLong(address1);
		long value2 = state.dataByteBuffer.getLong(address2);

		long newValue = operator.apply(value1, value2);

		state.dataByteBuffer.putLong(address1, newValue);
	}

	/**
	 * Common code for BGT/BLT/BGE/BLE/BEQ/BNE
	 * 
	 * @param codeByteBuffer
	 * @param dataByteBuffer
	 * @param state
	 * @param comparator
	 *            - typically a lambda comparing two <tt>long</tt> params, e.g. <tt>(a, b) -> a == b</tt>
	 * @throws ExecutionException
	 */
	private static void executeBranchConditional(MachineState state, TwoValueComparator comparator, Object... args) throws ExecutionException {
		int address1 = (int) args[0];
		int address2 = (int) args[1];
		byte offset = (byte) args[2];

		int branchTarget = state.getProgramCounter() + offset;

		long value1 = state.dataByteBuffer.getLong(address1);
		long value2 = state.dataByteBuffer.getLong(address2);

		if (comparator.compare(value1, value2))
			state.codeByteBuffer.position(branchTarget);
	}

}
