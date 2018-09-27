package org.ciyam.at;

import java.nio.ByteBuffer;

public enum OpCodeParam {

	VALUE {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Long(Utils.getCodeValue(codeByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("#%016x", (Long) value);
		}
	},
	DEST_ADDR {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Integer(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("@%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	INDIRECT_DEST_ADDR {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Integer(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("@($%08x)", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	INDIRECT_DEST_ADDR_WITH_INDEX {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Integer(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("@($%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	SRC_ADDR {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Integer(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("$%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	INDIRECT_SRC_ADDR {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Integer(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("$($%08x)", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	INDIRECT_SRC_ADDR_WITH_INDEX {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Integer(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("$($%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	INDEX {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Integer(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("+ $%08x)", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	CODE_ADDR {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Integer(Utils.getCodeAddress(codeByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("[%04x]", (Integer) value);
		}
	},
	OFFSET {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Byte(Utils.getCodeOffset(codeByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("PC+%02x=[%04x]", (int) ((Byte) value), postOpcodeProgramCounter - 1 + (Byte) value);
		}
	},
	FUNC {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Short(codeByteBuffer.getShort());
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			FunctionCode functionCode = FunctionCode.valueOf((Short) value);

			// generic/unknown form
			if (functionCode == null)
				return String.format("FN(%04x)", (Short) value);

			// API pass-through
			if (functionCode == FunctionCode.API_PASSTHROUGH)
				return String.format("API-FN(%04x)", (Short) value);

			return "\"" + functionCode.name() + "\"" + String.format("{%04x}", (Short) value);
		}
	},
	BLOCK_HEIGHT {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return new Integer(codeByteBuffer.getInt());
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("height $%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	};

	public abstract Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException;

	public String disassemble(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer, int postOpcodeProgramCounter) throws ExecutionException {
		Object value = fetch(codeByteBuffer, dataByteBuffer);

		return this.toString(value, postOpcodeProgramCounter);
	}

	protected abstract String toString(Object value, int postOpcodeProgramCounter);

}
