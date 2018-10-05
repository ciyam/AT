import static org.junit.Assert.*;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.OpCode;
import org.junit.Test;

import common.ExecutableTest;

public class BranchingOpCodeTests extends ExecutableTest {

	@Test
	public void testBZR_DATtrue() throws ExecutionException {
		int targetAddr = 0x21;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(0).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(1));
	}

	@Test
	public void testBZR_DATfalse() throws ExecutionException {
		int targetAddr = 0x21;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(9999L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(0).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(1));
	}

	@Test
	public void testBNZ_DATtrue() throws ExecutionException {
		int targetAddr = 0x21;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(9999L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNZ_DAT.value).putInt(0).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(1));
	}

	@Test
	public void testBNZ_DATfalse() throws ExecutionException {
		int targetAddr = 0x21;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNZ_DAT.value).putInt(0).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(1));
	}

	@Test
	public void testBGT_DATtrue() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BGT_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBGT_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BGT_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

	@Test
	public void testBLT_DATtrue() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLT_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBLT_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLT_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

	@Test
	public void testBGE_DATtrue1() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BGE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBGE_DATtrue2() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BGE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBGE_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BGE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

	@Test
	public void testBLE_DATtrue1() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBLE_DATtrue2() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBLE_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

	@Test
	public void testBEQ_DATtrue() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BEQ_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBEQ_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BEQ_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

	@Test
	public void testBNE_DATtrue() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1111L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 2L, getData(2));
	}

	@Test
	public void testBNE_DATfalse() throws ExecutionException {
		int targetAddr = 0x32;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(2222L);
		int tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNE_DAT.value).putInt(0).putInt(1).put((byte) (targetAddr - tempPC));
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", 1L, getData(2));
	}

}
