import static common.TestUtils.hexToBytes;
import static org.junit.Assert.*;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.OpCode;
import org.junit.Test;

import common.ExecutableTest;

public class OpCodeTests extends ExecutableTest {

	@Test
	public void testNOP() throws ExecutionException {
		codeByteBuffer.put(OpCode.NOP.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());

		// Check data unchanged
		for (int i = 0; i < 0x0020; ++i)
			assertEquals(0L, getData(i));
	}

	@Test
	public void testJMP_ADR() throws ExecutionException {
		int targetAddr = 0x12;

		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(targetAddr);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1L);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(0));
	}

	@Test
	public void testSLP_DAT() throws ExecutionException {
		int blockHeight = 12345;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(blockHeight);
		codeByteBuffer.put(OpCode.SLP_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsSleeping());
		assertFalse(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Sleep-until block height incorrect", blockHeight, getData(0));
	}

	@Test
	public void testFIZ_DATtrue() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		codeByteBuffer.put(OpCode.FIZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.SLP_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getIsSleeping());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testFIZ_DATfalse() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.FIZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.SLP_IMD.value);

		execute(true);

		assertFalse(state.getIsFinished());
		assertTrue(state.getIsSleeping());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testSTZ_DATtrue() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int stopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.STZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsStopped());
		assertFalse(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Program counter incorrect", stopAddress, state.getProgramCounter());
	}

	@Test
	public void testSTZ_DATfalse() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_PCS.value);
		codeByteBuffer.put(OpCode.STZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertFalse(state.getIsStopped());
		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testFIN_IMD() throws ExecutionException {
		codeByteBuffer.put(OpCode.FIN_IMD.value);
		codeByteBuffer.put(OpCode.STP_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getIsStopped());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testSTP_IMD() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int stopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.NOP.value);
		codeByteBuffer.put(OpCode.STP_IMD.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsStopped());
		assertFalse(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Program counter incorrect", stopAddress, state.getProgramCounter());
	}

	@Test
	public void testSLP_IMD() throws ExecutionException {
		codeByteBuffer.put(OpCode.SLP_IMD.value);
		int nextAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsSleeping());
		assertFalse(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Program counter incorrect", nextAddress, state.getProgramCounter());
	}

	@Test
	public void testERR_ADR() throws ExecutionException {
		// Note: non-fatal error because error handler IS set

		int errorAddr = 0x29;

		codeByteBuffer.put(OpCode.ERR_ADR.value).putInt(errorAddr);

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(12345L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(0L);
		codeByteBuffer.put(OpCode.DIV_DAT.value).putInt(0).putInt(1); // divide by zero
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// errorAddr:
		assertEquals(errorAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Error flag not set", 1L, getData(2));
	}

	@Test
	public void testPCS() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000011111111"));
		codeByteBuffer.put(OpCode.SET_PCS.value);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000022222222"));
		codeByteBuffer.put(OpCode.SET_PCS.value);
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int expectedStopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertEquals(expectedStopAddress, state.getOnStopAddress());
		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

	@Test
	public void testPCS2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000011111111"));
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int expectedStopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000022222222"));
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertEquals(expectedStopAddress, state.getOnStopAddress());
		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
	}

}
