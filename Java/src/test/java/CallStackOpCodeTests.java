import static common.TestUtils.*;
import static org.junit.Assert.*;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.junit.Test;

import common.ExecutableTest;

public class CallStackOpCodeTests extends ExecutableTest {

	@Test
	public void testJMP_SUB() throws ExecutionException {
		int subAddr = 0x06;

		codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(subAddr);
		int returnAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// subAddr:
		assertEquals(subAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());

		int expectedCallStackPosition = (state.numCallStackPages - 1) * CALL_STACK_PAGE_SIZE;
		assertEquals("Call stack pointer incorrect", expectedCallStackPosition, getCallStackPosition());

		assertEquals("Return address does not match", returnAddress, getCallStackEntry(expectedCallStackPosition));

		assertEquals("Data does not match", 4444L, getData(0));
	}

	@Test
	public void testJMP_SUB2() throws ExecutionException {
		int subAddr1 = 0x06;
		int subAddr2 = 0x19;

		codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(subAddr1);
		int returnAddress1 = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// subAddr1:
		assertEquals(subAddr1, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(subAddr2);
		int returnAddress2 = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// subAddr2:
		assertEquals(subAddr2, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(5555L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());

		int expectedCallStackPosition = (state.numCallStackPages - 1 - 1) * CALL_STACK_PAGE_SIZE;
		assertEquals("Call stack pointer incorrect", expectedCallStackPosition, getCallStackPosition());

		assertEquals("Return address does not match", returnAddress2, getCallStackEntry(expectedCallStackPosition));
		assertEquals("Return address does not match", returnAddress1, getCallStackEntry(expectedCallStackPosition + MachineState.ADDRESS_SIZE));

		assertEquals("Data does not match", 4444L, getData(0));
		assertEquals("Data does not match", 5555L, getData(1));
	}

	@Test
	public void testJMP_SUBoverflow() throws ExecutionException {
		// Call stack is 0x0010 entries in size, so exceed this to test overflow
		for (int i = 0; i < 20; ++i) {
			// sub address is next opcode!
			codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(i * (1 + 4));
		}
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testRET_SUB() throws ExecutionException {
		int subAddr = 0x13;

		codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(subAddr);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(7777L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// subAddr:
		assertEquals(subAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.RET_SUB.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value); // not reached!

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());

		int expectedCallStackPosition = (state.numCallStackPages - 1 + 1) * CALL_STACK_PAGE_SIZE;
		assertEquals("Call stack pointer incorrect", expectedCallStackPosition, getCallStackPosition());

		assertEquals("Return address not cleared", 0L, getCallStackEntry(expectedCallStackPosition - MachineState.ADDRESS_SIZE));

		assertEquals("Data does not match", 4444L, getData(0));
		assertEquals("Data does not match", 7777L, getData(1));
	}

	@Test
	public void testRET_SUB2() throws ExecutionException {
		int subAddr1 = 0x13;
		int subAddr2 = 0x34;

		codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(subAddr1);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(7777L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// subAddr1:
		assertEquals(subAddr1, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(subAddr2);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.RET_SUB.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value); // not reached!

		// subAddr2:
		assertEquals(subAddr2, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.RET_SUB.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value); // not reached!

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());

		int expectedCallStackPosition = (state.numCallStackPages - 1 - 1 + 1 + 1) * CALL_STACK_PAGE_SIZE;
		assertEquals("Call stack pointer incorrect", expectedCallStackPosition, getCallStackPosition());

		assertEquals("Return address not cleared", 0L, getCallStackEntry(expectedCallStackPosition - MachineState.ADDRESS_SIZE));

		assertEquals("Data does not match", 4444L, getData(0));
		assertEquals("Data does not match", 7777L, getData(1));
		assertEquals("Data does not match", 2222L, getData(2));
		assertEquals("Data does not match", 3333L, getData(3));
	}

	@Test
	public void testRET_SUBoverflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.RET_SUB.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

	@Test
	public void testRET_SUBoverflow2() throws ExecutionException {
		// sub address is next opcode!
		codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(1 + 4);
		// this is return address too
		codeByteBuffer.put(OpCode.RET_SUB.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

}
