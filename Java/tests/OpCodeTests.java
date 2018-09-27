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

public class OpCodeTests {

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
		} while (!state.isFinished && !state.isFrozen && !state.isSleeping && !state.isStopped);

	}

	@Test
	public void testNOP() throws ExecutionException {
		codeByteBuffer.put(OpCode.NOP.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);

		// Check data unchanged
		state.dataByteBuffer.position(0);
		while (state.dataByteBuffer.hasRemaining())
			assertEquals((byte) 0, state.dataByteBuffer.get());
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(0 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testSLP_DAT() throws ExecutionException {
		int blockHeight = 12345;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(blockHeight);
		codeByteBuffer.put(OpCode.SLP_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isSleeping);
		assertFalse(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Sleep-until block height incorrect", blockHeight, state.dataByteBuffer.getLong(0 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testFIZ_DATtrue() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		codeByteBuffer.put(OpCode.FIZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.SLP_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.isSleeping);
		assertFalse(state.hadFatalError);
	}

	@Test
	public void testFIZ_DATfalse() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.FIZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.SLP_IMD.value);

		simulate();

		assertFalse(state.isFinished);
		assertTrue(state.isSleeping);
		assertFalse(state.hadFatalError);
	}

	@Test
	public void testSTZ_DATtrue() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int stopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.STZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isStopped);
		assertFalse(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Program counter incorrect", stopAddress, state.programCounter);
	}

	@Test
	public void testSTZ_DATfalse() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_PCS.value);
		codeByteBuffer.put(OpCode.STZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertFalse(state.isStopped);
		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
	}

	@Test
	public void testFIN_IMD() throws ExecutionException {
		codeByteBuffer.put(OpCode.FIN_IMD.value);
		codeByteBuffer.put(OpCode.STP_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.isStopped);
		assertFalse(state.hadFatalError);
	}

	@Test
	public void testSTP_IMD() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int stopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.NOP.value);
		codeByteBuffer.put(OpCode.STP_IMD.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isStopped);
		assertFalse(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Program counter incorrect", stopAddress, state.programCounter);
	}

	@Test
	public void testSLP_IMD() throws ExecutionException {
		codeByteBuffer.put(OpCode.SLP_IMD.value);
		int nextAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isSleeping);
		assertFalse(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Program counter incorrect", nextAddress, state.programCounter);
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Error flag not set", 1L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertEquals(expectedStopAddress, state.onStopAddress);
		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
	}

	@Test
	public void testPCS2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000011111111"));
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int expectedStopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000022222222"));
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertEquals(expectedStopAddress, state.onStopAddress);
		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
	}

}
