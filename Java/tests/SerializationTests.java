import static common.TestUtils.hexToBytes;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.ciyam.at.API;
import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import common.TestAPI;
import common.TestLogger;

public class SerializationTests {

	public TestLogger logger;
	public API api;
	public MachineState state;
	public ByteBuffer codeByteBuffer;

	@Before
	public void beforeTest() {
		logger = new TestLogger();
		api = new TestAPI();
		codeByteBuffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
	}

	@After
	public void afterTest() {
		codeByteBuffer = null;
		api = null;
		logger = null;
	}

	private byte[] simulate() {
		// version 0003, reserved 0000, code 0100 * 1, data 0020 * 8, call stack 0010 * 4, user stack 0010 * 4
		byte[] headerBytes = hexToBytes("0300" + "0000" + "0001" + "2000" + "1000" + "1000");
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = new byte[0];

		state = new MachineState(api, logger, headerBytes, codeBytes, dataBytes);

		return executeAndCheck(state);
	}

	private byte[] continueSimulation(byte[] savedState) {
		state = MachineState.fromBytes(api, logger, savedState);

		// Pretend we're on next block
		state.currentBlockHeight++;

		return executeAndCheck(state);
	}

	private byte[] executeAndCheck(MachineState state) {
		state.execute();

		byte[] stateBytes = state.toBytes();
		MachineState restoredState = MachineState.fromBytes(api, logger, stateBytes);
		byte[] restoredStateBytes = restoredState.toBytes();

		assertTrue("Serialization->Deserialization->Reserialization error", Arrays.equals(stateBytes, restoredStateBytes));

		return stateBytes;
	}

	@Test
	public void testPCS2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000011111111"));
		codeByteBuffer.put(OpCode.SET_PCS.value);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000022222222"));
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertEquals(0x0e, (int) state.onStopAddress);
		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
	}

	@Test
	public void testStopWithStacks() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(100); // 0000
		codeByteBuffer.put(OpCode.SET_PCS.value); // 000d
		codeByteBuffer.put(OpCode.JMP_SUB.value).putInt(0x002a); // 000e
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(10); // 0013
		codeByteBuffer.put(OpCode.ADD_DAT.value).putInt(0).putInt(1); // 0020
		codeByteBuffer.put(OpCode.STP_IMD.value); // 0029
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.ECHO.value).putInt(0); // 002a
		codeByteBuffer.put(OpCode.PSH_DAT.value).putInt(0); // 0031
		codeByteBuffer.put(OpCode.RET_SUB.value); // 0036

		byte[] savedState = simulate();

		assertEquals(0x0e, (int) state.onStopAddress);
		assertTrue(state.isStopped);
		assertFalse(state.hadFatalError);

		savedState = continueSimulation(savedState);
		savedState = continueSimulation(savedState);
	}

}
