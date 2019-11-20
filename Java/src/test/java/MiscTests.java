import static common.TestUtils.hexToBytes;
import static org.junit.Assert.*;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.junit.Test;

import common.ExecutableTest;

public class MiscTests extends ExecutableTest {

	@Test
	public void testSimpleCode() throws ExecutionException {
		long testValue = 8888L;
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(testValue);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.ECHO.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", testValue, getData(0));
	}

	@Test
	public void testInvalidOpCode() throws ExecutionException {
		codeByteBuffer.put((byte) 0xdd);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testFreeze() throws ExecutionException {
		// Infinite loop
		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(0);

		// If starting balance is 1234 then should take about 3 rounds as 500 steps max each round.
		for (int i = 0; i < 3; ++i)
			execute(true);

		assertTrue(state.getIsFrozen());

		Long frozenBalance = state.getFrozenBalance();
		assertNotNull(frozenBalance);
	}

	@Test
	public void testMinActivation() throws ExecutionException {
		long minActivation = 12345L; // 0x0000000000003039

		// version 0002, reserved 0000, code 0200 * 1, data 0020 * 8, call stack 0010 * 4, user stack 0010 * 4, minActivation = 12345L
		byte[] headerBytes = hexToBytes("0200" + "0000" + "0002" + "2000" + "1000" + "1000" + "3930000000000000");
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = new byte[0];

		state = new MachineState(api, logger, headerBytes, codeBytes, dataBytes);

		assertTrue(state.getIsFrozen());
		assertEquals((Long) (minActivation - 1L), state.getFrozenBalance());
	}

}
