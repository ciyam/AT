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

public class UserStackOpCodeTests {

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
	public void testPSH_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);

		int expectedUserStackPosition = (state.numUserStackPages - 1) * MachineState.USER_STACK_PAGE_SIZE;
		assertEquals("User stack pointer incorrect", expectedUserStackPosition, state.userStackByteBuffer.position());
		assertEquals("Data does not match", 4444L, state.userStackByteBuffer.getLong(expectedUserStackPosition));
	}

	@Test
	public void testPSH_DAT2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(3333L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(1);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);

		int expectedUserStackPosition = (state.numUserStackPages - 2) * MachineState.USER_STACK_PAGE_SIZE;
		assertEquals("User stack pointer incorrect", expectedUserStackPosition, state.userStackByteBuffer.position());
		assertEquals("Data does not match", 3333L, state.userStackByteBuffer.getLong(expectedUserStackPosition));
	}

	@Test
	public void testPSH_DAToverflow() throws ExecutionException {
		// User stack is 0x0010 entries in size, so exceed this to test overflow
		for (int i = 0; i < 20; ++i) {
			codeByteBuffer.put(OpCode.SET_VAL.value).putInt(i).putLong(1000L * i);
			codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(i);
		}
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Error flag not set", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testPOP_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.POP_DAT.value).putInt(1);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);

		int expectedUserStackPosition = (state.numUserStackPages - 1 + 1) * MachineState.USER_STACK_PAGE_SIZE;
		assertEquals("User stack pointer incorrect", expectedUserStackPosition, state.userStackByteBuffer.position());
		assertEquals("Data does not match", 4444L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
		assertEquals("Stack entry not cleared", 0L, state.userStackByteBuffer.getLong(expectedUserStackPosition - MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);

		int expectedUserStackPosition = (state.numUserStackPages - 1 - 1 + 1 + 1) * MachineState.USER_STACK_PAGE_SIZE;
		assertEquals("User stack pointer incorrect", expectedUserStackPosition, state.userStackByteBuffer.position());
		assertEquals("Data does not match", 3333L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
		assertEquals("Data does not match", 4444L, state.dataByteBuffer.getLong(3 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testPOP_DAToverflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.POP_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testPOP_DAToverflow2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(4444L);
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.POP_DAT.value).putInt(1);
		codeByteBuffer.put(OpCode.POP_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

}
