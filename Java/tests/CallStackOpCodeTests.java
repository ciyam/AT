import static common.TestUtils.hexToBytes;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ciyam.at.API;
import org.ciyam.at.ExecutionException;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import common.TestAPI;
import common.TestLogger;

public class CallStackOpCodeTests {

	public TestLogger logger;
	public API api;
	public MachineState state;
	public ByteBuffer codeByteBuffer;

	@BeforeClass
	public static void beforeClass() {
		Security.insertProviderAt(new BouncyCastleProvider(), 0);
	}

	@Before
	public void beforeTest() {
		logger = new TestLogger();
		api = new TestAPI();
		codeByteBuffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
	}

	@After
	public void afterTest() {
		codeByteBuffer = null;
		api = null;
		logger = null;
	}

	private void execute() {
		System.out.println("Starting execution:");
		System.out.println("Current block height: " + state.currentBlockHeight);

		state.execute();

		System.out.println("After execution:");
		System.out.println("Steps: " + state.steps);
		System.out.println("Program Counter: " + String.format("%04x", state.programCounter));
		System.out.println("Stop Address: " + String.format("%04x", state.onStopAddress));
		System.out.println("Error Address: " + (state.onErrorAddress == null ? "not set" : String.format("%04x", state.onErrorAddress)));
		if (state.isSleeping)
			System.out.println("Sleeping until current block height (" + state.currentBlockHeight + ") reaches " + state.sleepUntilHeight);
		else
			System.out.println("Sleeping: " + state.isSleeping);
		System.out.println("Stopped: " + state.isStopped);
		System.out.println("Finished: " + state.isFinished);
		if (state.hadFatalError)
			System.out.println("Finished due to fatal error!");
		System.out.println("Frozen: " + state.isFrozen);
	}

	private void simulate() {
		// version 0003, reserved 0000, code 0200 * 1, data 0020 * 8, call stack 0010 * 4, user stack 0010 * 8
		byte[] headerBytes = hexToBytes("0300" + "0000" + "0002" + "2000" + "1000" + "1000");
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = new byte[0];

		state = new MachineState(api, logger, headerBytes, codeBytes, dataBytes);

		do {
			execute();

			// Bump block height
			state.currentBlockHeight++;
		} while (!state.isFinished);

	}

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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);

		int expectedCallStackPosition = (state.numCallStackPages - 1) * MachineState.CALL_STACK_PAGE_SIZE;
		assertEquals("Call stack pointer incorrect", expectedCallStackPosition, state.callStackByteBuffer.position());

		assertEquals("Return address does not match", returnAddress, state.callStackByteBuffer.getInt(expectedCallStackPosition));

		assertEquals("Data does not match", 4444L, state.dataByteBuffer.getLong(0 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);

		int expectedCallStackPosition = (state.numCallStackPages - 1 - 1) * MachineState.CALL_STACK_PAGE_SIZE;
		assertEquals("Call stack pointer incorrect", expectedCallStackPosition, state.callStackByteBuffer.position());

		assertEquals("Return address does not match", returnAddress2, state.callStackByteBuffer.getInt(expectedCallStackPosition));
		assertEquals("Return address does not match", returnAddress1, state.callStackByteBuffer.getInt(expectedCallStackPosition + MachineState.ADDRESS_SIZE));

		assertEquals("Data does not match", 4444L, state.dataByteBuffer.getLong(0 * MachineState.VALUE_SIZE));
		assertEquals("Data does not match", 5555L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testJMP_SUBoverflow() throws ExecutionException {
		// Call stack is 0x0010 entries in size, so exceed this to test overflow
		for (int i = 0; i < 20; ++i) {
			// sub address is next opcode!
			codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(i * (1 + 4));
		}
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);

		int expectedCallStackPosition = (state.numCallStackPages - 1 + 1) * MachineState.CALL_STACK_PAGE_SIZE;
		assertEquals("Call stack pointer incorrect", expectedCallStackPosition, state.callStackByteBuffer.position());

		assertEquals("Return address not cleared", 0L, state.callStackByteBuffer.getInt(expectedCallStackPosition - MachineState.ADDRESS_SIZE));

		assertEquals("Data does not match", 4444L, state.dataByteBuffer.getLong(0 * MachineState.VALUE_SIZE));
		assertEquals("Data does not match", 7777L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);

		int expectedCallStackPosition = (state.numCallStackPages - 1 - 1 + 1 + 1) * MachineState.CALL_STACK_PAGE_SIZE;
		assertEquals("Call stack pointer incorrect", expectedCallStackPosition, state.callStackByteBuffer.position());

		assertEquals("Return address not cleared", 0L, state.callStackByteBuffer.getInt(expectedCallStackPosition - MachineState.ADDRESS_SIZE));

		assertEquals("Data does not match", 4444L, state.dataByteBuffer.getLong(0 * MachineState.VALUE_SIZE));
		assertEquals("Data does not match", 7777L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
		assertEquals("Data does not match", 2222L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
		assertEquals("Data does not match", 3333L, state.dataByteBuffer.getLong(3 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testRET_SUBoverflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.RET_SUB.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testRET_SUBoverflow2() throws ExecutionException {
		// sub address is next opcode!
		codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(1 + 4);
		// this is return address too
		codeByteBuffer.put(OpCode.RET_SUB.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

}
