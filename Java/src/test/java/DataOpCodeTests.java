import static org.junit.Assert.*;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.OpCode;
import org.junit.Test;

import common.ExecutableTest;

public class DataOpCodeTests extends ExecutableTest {

	@Test
	public void testSET_VAL() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2222L, getData(2));
	}

	@Test
	public void testSET_VALunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(9999).putLong(2222L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testSET_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_DAT.value).putInt(1).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2222L, getData(1));
	}

	@Test
	public void testSET_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_DAT.value).putInt(9999).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testSET_DATunbounded2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_DAT.value).putInt(1).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testCLR_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.CLR_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());

		// Check data all zero
		for (int i = 0; i < 0x0020; ++i)
			assertEquals(0L, getData(i));
	}

	@Test
	public void testCLR_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.CLR_DAT.value).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testINC_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.INC_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2222L + 1L, getData(2));
	}

	@Test
	public void testINC_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.INC_DAT.value).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testINC_DAToverflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(0xffffffffffffffffL);
		codeByteBuffer.put(OpCode.INC_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 0L, getData(2));
	}

	@Test
	public void testDEC_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.DEC_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2222L - 1L, getData(2));
	}

	@Test
	public void testDEC_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.DEC_DAT.value).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);
		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testDEC_DATunderflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(0L);
		codeByteBuffer.put(OpCode.DEC_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 0xffffffffffffffffL, getData(2));
	}

	@Test
	public void testADD_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.ADD_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2222L + 3333L, getData(2));
	}

	@Test
	public void testADD_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.ADD_DAT.value).putInt(9999).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testADD_DATunbounded2() throws ExecutionException {
		codeByteBuffer.put(OpCode.ADD_DAT.value).putInt(2).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testADD_DAToverflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(0x7fffffffffffffffL);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(0x8000000000000099L);
		codeByteBuffer.put(OpCode.ADD_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 0x0000000000000098L, getData(2));
	}

	@Test
	public void testSUB_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SUB_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 3333L - 2222L, getData(3));
	}

	@Test
	public void testMUL_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.MUL_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", (3333L * 2222L), getData(3));
	}

	@Test
	public void testDIV_DAT() throws ExecutionException {
		// Note: fatal error because error handler not set

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.DIV_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", (3333L / 2222L), getData(3));
	}

	@Test
	public void testDIV_DATzeroWithOnError() throws ExecutionException {
		// Note: non-fatal error because error handler IS set

		int errorAddr = 0x29;

		codeByteBuffer.put(OpCode.ERR_ADR.value).putInt(errorAddr);

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.DIV_DAT.value).putInt(3).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// errorAddr:
		assertEquals(errorAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Error flag not set", 1L, getData(1));
	}

	@Test
	public void testBOR_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.BOR_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", (3333L | 2222L), getData(3));
	}

	@Test
	public void testAND_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.AND_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", (3333L & 2222L), getData(3));
	}

	@Test
	public void testXOR_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.XOR_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", (3333L ^ 2222L), getData(3));
	}

	@Test
	public void testNOT_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.NOT_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", ~2222L, getData(2));
	}

	@Test
	public void testSET_IND() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(3L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		// @(6) = $($0) aka $(3) aka 3333
		codeByteBuffer.put(OpCode.SET_IND.value).putInt(6).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 3333L, getData(6));
	}

	@Test
	public void testSET_INDunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(3L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		// @(6) = $($9999) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.SET_IND.value).putInt(6).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testSET_INDunbounded2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(9999L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		// @(6) = $($0) aka $(9999) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.SET_IND.value).putInt(6).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testSET_IDX() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(6).putLong(1L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(7).putLong(3L);
		// @(0) = $($6 + $7) aka $(1 + 3) aka $(4) aka 4444
		codeByteBuffer.put(OpCode.SET_IDX.value).putInt(0).putInt(6).putInt(7);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 4444L, getData(0));
	}

	@Test
	public void testSET_IDXunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(6).putLong(1L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(7).putLong(3L);
		// @(0) = $($9999 + $7) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.SET_IDX.value).putInt(0).putInt(9999).putInt(7);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testSET_IDXunbounded2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(6).putLong(9999L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(7).putLong(3L);
		// @(0) = $($6 + $7) aka $(9999 + 1) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.SET_IDX.value).putInt(0).putInt(6).putInt(7);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testSET_IDXunbounded3() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(6).putLong(1L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(7).putLong(9999L);
		// @(0) = $($6 + $7) aka $(1 + 9999) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.SET_IDX.value).putInt(0).putInt(6).putInt(7);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testSET_IDXunbounded4() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(6).putLong(1L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(7).putLong(3L);
		// @(0) = $($6 + $9999) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.SET_IDX.value).putInt(0).putInt(6).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testIND_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(3L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		// @($0) aka @(3) = $(5) = 5555
		codeByteBuffer.put(OpCode.IND_DAT.value).putInt(0).putInt(5);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 5555L, getData(3));
	}

	@Test
	public void testIND_DATDunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(3L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		// @($9999) = $(5) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.SET_IND.value).putInt(9999).putInt(5);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testIND_DATDunbounded2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(9999L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		// @($0) aka @(9999) = $(5) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.SET_IND.value).putInt(0).putInt(5);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testIDX_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(6).putLong(1L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(7).putLong(3L);
		// @($6 + $7) aka @(1 + 3) aka @(4) = $(5) aka 5555
		codeByteBuffer.put(OpCode.IDX_DAT.value).putInt(6).putInt(7).putInt(5);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 5555L, getData(4));
	}

	@Test
	public void testIDX_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(6).putLong(1L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(7).putLong(3L);
		// @($9999 + $7) = $(5) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.IDX_DAT.value).putInt(9999).putInt(7).putInt(5);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testIDX_DATunbounded2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(6).putLong(9999L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(7).putLong(3L);
		// @($6 + $7) aka @(9999 + 3) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.IDX_DAT.value).putInt(6).putInt(7).putInt(5);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testIDX_DATunbounded3() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(6).putLong(1L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(7).putLong(9999L);
		// @($6 + $7) aka @(1 + 9999) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.IDX_DAT.value).putInt(6).putInt(7).putInt(5);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testIDX_DATunbounded4() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(4).putLong(4444L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(5).putLong(5555L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(6).putLong(1L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(7).putLong(3L);
		// @($6 + $9999) = $(5) but data address 9999 is out of bounds
		codeByteBuffer.put(OpCode.IDX_DAT.value).putInt(6).putInt(9999).putInt(5);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testMOD_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.MOD_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2222L % 3333L, getData(2));
	}

	@Test
	public void testMOD_DATzeroWithOnError() throws ExecutionException {
		// Note: non-fatal error because error handler IS set

		int errorAddr = 0x29;

		codeByteBuffer.put(OpCode.ERR_ADR.value).putInt(errorAddr);

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.MOD_DAT.value).putInt(3).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// errorAddr:
		assertEquals(errorAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Error flag not set", 1L, getData(1));
	}

	@Test
	public void testSHL_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3L);
		codeByteBuffer.put(OpCode.SHL_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2222L << 3, getData(2));
	}

	@Test
	public void testSHL_DATexcess() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SHL_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 0L, getData(2));
	}

	@Test
	public void testSHR_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3L);
		codeByteBuffer.put(OpCode.SHR_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2222L >> 3, getData(2));
	}

	@Test
	public void testSHR_DATexcess() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SHR_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 0L, getData(2));
	}

}
