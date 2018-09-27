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

public class BranchingOpCodeTests {

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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 1L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 1L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 1L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 1L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 1L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 2L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
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

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", 1L, state.dataByteBuffer.getLong(2 * MachineState.VALUE_SIZE));
	}

}
