import static common.TestUtils.*;
import static org.junit.Assert.*;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.OpCode;
import org.junit.Test;

import common.ExecutableTest;

public class UserStackOpCodeTests extends ExecutableTest {

	@Test
	public void testPSH_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());

		int expectedUserStackPosition = (state.numUserStackPages - 1) * USER_STACK_PAGE_SIZE;
		assertEquals("User stack pointer incorrect", expectedUserStackPosition, getUserStackPosition());
		assertEquals("Data does not match", 4444L, getUserStackEntry(expectedUserStackPosition));
	}

	@Test
	public void testPSH_DAT2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(3333L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(1);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());

		int expectedUserStackPosition = (state.numUserStackPages - 2) * USER_STACK_PAGE_SIZE;
		assertEquals("User stack pointer incorrect", expectedUserStackPosition, getUserStackPosition());
		assertEquals("Data does not match", 3333L, getUserStackEntry(expectedUserStackPosition));
	}

	@Test
	public void testPSH_DAToverflow() throws ExecutionException {
		// User stack is 0x0010 entries in size, so exceed this to test overflow
		for (int i = 0; i < 20; ++i) {
			codeByteBuffer.put(OpCode.SET_VAL.value).putInt(i).putLong(1000L * i);
			codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(i);
		}
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testPSH_DAToverflowWithOnError() throws ExecutionException {
		int errorAddr = 0x16e;

		codeByteBuffer.put(OpCode.ERR_ADR.value).putInt(errorAddr);

		// User stack is 0x0010 entries in size, so exceed this to test overflow
		for (int i = 0; i < 20; ++i) {
			codeByteBuffer.put(OpCode.SET_VAL.value).putInt(i).putLong(1000L * i);
			codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(i);
		}
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
	public void testPOP_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.POP_DAT.value).putInt(1);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());

		int expectedUserStackPosition = (state.numUserStackPages - 1 + 1) * USER_STACK_PAGE_SIZE;
		assertEquals("User stack pointer incorrect", expectedUserStackPosition, getUserStackPosition());
		assertEquals("Data does not match", 4444L, getData(1));
		// Following test is not applicable when using serialized state:
		// assertEquals("Stack entry not cleared", 0L, getUserStackEntry(expectedUserStackPosition - MachineState.VALUE_SIZE));
	}

	@Test
	public void testPOP_DAT2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(3333L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(1);
		codeByteBuffer.put(OpCode.POP_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.POP_DAT.value).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());

		int expectedUserStackPosition = (state.numUserStackPages - 1 - 1 + 1 + 1) * USER_STACK_PAGE_SIZE;
		assertEquals("User stack pointer incorrect", expectedUserStackPosition, getUserStackPosition());
		assertEquals("Data does not match", 3333L, getData(2));
		assertEquals("Data does not match", 4444L, getData(3));
	}

	@Test
	public void testPOP_DAToverflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.POP_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testPOP_DAToverflow2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.POP_DAT.value).putInt(1);
		codeByteBuffer.put(OpCode.POP_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

}
