package org.ciyam.at;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class Utils {

	/**
	 * Returns immediate function code enum from code bytes at current position.
	 * <p>
	 * Initial position is <tt>codeByteBuffer.position()</tt> but on return is incremented by 2.
	 * 
	 * @param codeByteBuffer
	 * @return FunctionCode enum
	 * @throws CodeSegmentException
	 * @throws InvalidAddressException
	 */
	public static FunctionCode getFunctionCode(ByteBuffer codeByteBuffer) throws CodeSegmentException, IllegalFunctionCodeException {
		try {
			int rawFunctionCode = codeByteBuffer.getShort();

			FunctionCode functionCode = FunctionCode.valueOf(rawFunctionCode);

			if (functionCode == null)
				throw new IllegalFunctionCodeException("Unknown function code");

			return functionCode;
		} catch (BufferUnderflowException e) {
			throw new CodeSegmentException("No code bytes left to get function code", e);
		}
	}

	/**
	 * Returns code address from code bytes at current position.
	 * <p>
	 * Initial position is <tt>codeByteBuffer.position()</tt> but on return is incremented by 4.
	 * <p>
	 * <b>Note:</b> address is not scaled by <tt>Constants.VALUE_SIZE</tt> unlike other methods in this class.
	 * 
	 * @param codeByteBuffer
	 * @return int address into code bytes
	 * @throws CodeSegmentException
	 * @throws InvalidAddressException
	 */
	public static int getCodeAddress(ByteBuffer codeByteBuffer) throws CodeSegmentException, InvalidAddressException {
		try {
			int address = codeByteBuffer.getInt();

			if (address < 0 || address > MachineState.MAX_CODE_ADDRESS || address >= codeByteBuffer.limit())
				throw new InvalidAddressException("Code address out of bounds");

			return address;
		} catch (BufferUnderflowException e) {
			throw new CodeSegmentException("No code bytes left to get code address", e);
		}
	}

	/**
	 * Returns data address from code bytes at current position.
	 * <p>
	 * Initial position is <tt>codeByteBuffer.position()</tt> but on return is incremented by 4.
	 * <p>
	 * <b>Note:</b> address is returned scaled by <tt>Constants.VALUE_SIZE</tt>.
	 * 
	 * @param codeByteBuffer
	 * @return int address into data bytes
	 * @throws CodeSegmentException
	 * @throws InvalidAddressException
	 */
	public static int getDataAddress(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws CodeSegmentException, InvalidAddressException {
		try {
			int address = codeByteBuffer.getInt() * MachineState.VALUE_SIZE;

			if (address < 0 || address + MachineState.VALUE_SIZE >= dataByteBuffer.limit())
				throw new InvalidAddressException("Data address out of bounds");

			return address;
		} catch (BufferUnderflowException e) {
			throw new CodeSegmentException("No code bytes left to get data address", e);
		}
	}

	/**
	 * Returns byte offset from code bytes at current position.
	 * <p>
	 * Initial position is <tt>codeByteBuffer.position()</tt> but on return is incremented by 1.
	 * <p>
	 * <b>Note:</b> offset is not scaled by <tt>Constants.VALUE_SIZE</tt> unlike other methods in this class.
	 * 
	 * @param codeByteBuffer
	 * @return byte offset
	 * @throws CodeSegmentException
	 * @throws InvalidAddressException
	 */
	public static byte getCodeOffset(ByteBuffer codeByteBuffer) throws CodeSegmentException, InvalidAddressException {
		try {
			byte offset = codeByteBuffer.get();

			if (codeByteBuffer.position() + offset < 0 || codeByteBuffer.position() + offset >= codeByteBuffer.limit())
				throw new InvalidAddressException("Code offset out of bounds");

			return offset;
		} catch (BufferUnderflowException e) {
			throw new CodeSegmentException("No code bytes left to get code offset", e);
		}
	}

	/**
	 * Returns long immediate value from code bytes at current position.
	 * <p>
	 * Initial position is <tt>codeByteBuffer.position()</tt> but on return is incremented by 8.
	 * 
	 * @param codeByteBuffer
	 * @return long value
	 * @throws CodeSegmentException
	 * @throws InvalidAddressException
	 */
	public static long getCodeValue(ByteBuffer codeByteBuffer) throws CodeSegmentException, InvalidAddressException {
		try {
			return codeByteBuffer.getLong();
		} catch (BufferUnderflowException e) {
			throw new CodeSegmentException("No code bytes left to get immediate value", e);
		}
	}

}
