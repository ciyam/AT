import static common.TestUtils.hexToBytes;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ciyam.at.API;
import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.ciyam.at.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import common.TestAPI;
import common.TestLogger;

public class FunctionCodeTests {

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
	public void testMD5() throws ExecutionException {
		// MD5 of ffffffffffffffffffffffffffffffff is 8d79cbc9a4ecdde112fc91ba625b13c2
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("ffffffffffffffff"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);
		// A3 unused
		// A4 unused

		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.MD5_A_TO_B.value);

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("8d79cbc9a4ecdde1"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("12fc91ba625b13c2"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000000000000"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A3.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000000000000"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A4.value).putInt(0);

		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_A_EQUALS_B.value).putInt(1);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("MD5 hashes do not match", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testCHECK_MD5() throws ExecutionException {
		// MD5 of ffffffffffffffffffffffffffffffff is 8d79cbc9a4ecdde112fc91ba625b13c2
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("ffffffffffffffff"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);
		// A3 unused
		// A4 unused

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("8d79cbc9a4ecdde1"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B1.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("12fc91ba625b13c2"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B2.value).putInt(0);

		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_MD5_A_WITH_B.value).putInt(1);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("MD5 hashes do not match", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testHASH160() throws ExecutionException {
		// RIPEMD160 of ffffffffffffffffffffffffffffffffffffffffffffffff is 90e735014ea23aa89190121b229c06d58fc71e83
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("ffffffffffffffff"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A3.value).putInt(0);
		// A4 unused

		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.HASH160_A_TO_B.value);

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("90e735014ea23aa8"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("9190121b229c06d5"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("8fc71e8300000000"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A3.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000000000000"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A4.value).putInt(0);

		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_A_EQUALS_B.value).putInt(1);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("RIPEMD160 hashes do not match", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testCHECK_HASH160() throws ExecutionException {
		// RIPEMD160 of ffffffffffffffffffffffffffffffffffffffffffffffff is 90e735014ea23aa89190121b229c06d58fc71e83
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("ffffffffffffffff"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A3.value).putInt(0);
		// A4 unused

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("90e735014ea23aa8"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B1.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("9190121b229c06d5"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B2.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("8fc71e8300000000"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B3.value).putInt(0);

		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_HASH160_A_WITH_B.value).putInt(1);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertEquals("RIPEMD160 hashes do not match", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
	}

	@Test
	public void testSHA256() throws ExecutionException {
		// SHA256 of ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff is af9613760f72635fbdb44a5a0a63c39f12af30f950a6ee5c971be188e89c4051
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("ffffffffffffffff"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A3.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A4.value).putInt(0);

		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.SHA256_A_TO_B.value);

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("af9613760f72635f"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("bdb44a5a0a63c39f"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("12af30f950a6ee5c"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A3.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("971be188e89c4051"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A4.value).putInt(0);

		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_A_EQUALS_B.value).putInt(1);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("RIPEMD160 hashes do not match", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testCHECK_SHA256() throws ExecutionException {
		// SHA256 of ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff is af9613760f72635fbdb44a5a0a63c39f12af30f950a6ee5c971be188e89c4051
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("ffffffffffffffff"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A3.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A4.value).putInt(0);

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("af9613760f72635f"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B1.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("bdb44a5a0a63c39f"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B2.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("12af30f950a6ee5c"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B3.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("971be188e89c4051"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B4.value).putInt(0);

		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_SHA256_A_WITH_B.value).putInt(1);

		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertEquals("RIPEMD160 hashes do not match", 1L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
	}

	@Test
	public void testRandom() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(Timestamp.toLong(api.getCurrentBlockHeight(), 0));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_IN_A.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GENERATE_RANDOM_USING_TX_IN_A.value).putInt(1);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertNotEquals("Random wasn't generated", 0L, state.dataByteBuffer.getLong(1 * MachineState.VALUE_SIZE));
		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
	}

	@Test
	public void testInvalidFunctionCode() throws ExecutionException {
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort((short) 0xaaaa);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

	@Test
	public void testPlatformSpecific0501() {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(Timestamp.toLong(api.getCurrentBlockHeight(), 0));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort((short) 0x0501).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
	}

	@Test
	public void testPlatformSpecific0501Error() {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(Timestamp.toLong(api.getCurrentBlockHeight(), 0));
		codeByteBuffer.put(OpCode.EXT_FUN_RET_DAT_2.value).putShort((short) 0x0501).putInt(0).putInt(0); // Wrong OPCODE for function
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

}
