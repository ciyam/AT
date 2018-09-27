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

public class DataOpCodeTests {

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
	public void testSET_VAL() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2222L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testSET_VALunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(9999).putLong(2222L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testSET_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_DAT.value).putInt(1).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2222L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testSET_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_DAT.value).putInt(9999).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testSET_DATunbounded2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_DAT.value).putInt(1).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testCLR_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.CLR_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);

		// Check data all zero
		state.dataByteBuffer.position(0);
		while (state.dataByteBuffer.hasRemaining())
			assertEquals((byte) 0, state.dataByteBuffer.get());
	}

	@Test
	public void testCLR_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.CLR_DAT.value).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testINC_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.INC_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2222L + 1L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testINC_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.INC_DAT.value).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testINC_DAToverflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(0xffffffffffffffffL);
		codeByteBuffer.put(OpCode.INC_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 0L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testDEC_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.DEC_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2222L - 1L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testDEC_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.DEC_DAT.value).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();
		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testDEC_DATunderflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(0L);
		codeByteBuffer.put(OpCode.DEC_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 0xffffffffffffffffL, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testADD_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.ADD_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2222L + 3333L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testADD_DATunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.ADD_DAT.value).putInt(9999).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testADD_DATunbounded2() throws ExecutionException {
		codeByteBuffer.put(OpCode.ADD_DAT.value).putInt(2).putInt(9999);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testADD_DAToverflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(0x7fffffffffffffffL);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(0x8000000000000099L);
		codeByteBuffer.put(OpCode.ADD_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 0x0000000000000098L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testSUB_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SUB_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 3333L - 2222L, state.dataByteBuffer.getLong(3 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testMUL_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.MUL_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", (3333L * 2222L), state.dataByteBuffer.getLong(3 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testDIV_DAT() throws ExecutionException {
		// Note: fatal error because error handler not set

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.DIV_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", (3333L / 2222L), state.dataByteBuffer.getLong(3 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Error flag not set", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testBOR_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.BOR_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", (3333L | 2222L), state.dataByteBuffer.getLong(3 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testAND_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.AND_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", (3333L & 2222L), state.dataByteBuffer.getLong(3 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testXOR_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.XOR_DAT.value).putInt(3).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", (3333L ^ 2222L), state.dataByteBuffer.getLong(3 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testNOT_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.NOT_DAT.value).putInt(2);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", ~2222L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 3333L, state.dataByteBuffer.getLong(6 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 4444L, state.dataByteBuffer.getLong(0 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 5555L, state.dataByteBuffer.getLong(3 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 5555L, state.dataByteBuffer.getLong(4 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
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

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testMOD_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.MOD_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2222L % 3333L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Error flag not set", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testSHL_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3L);
		codeByteBuffer.put(OpCode.SHL_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2222L << 3, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testSHL_DATexcess() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SHL_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 0L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testSHR_DAT() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3L);
		codeByteBuffer.put(OpCode.SHR_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2222L >> 3, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testSHR_DATexcess() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SHR_DAT.value).putInt(2).putInt(3);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 0L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

}
