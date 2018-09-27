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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import common.TestAPI;
import common.TestLogger;

public class MiscTests {

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
	public void testSimpleCode() throws ExecutionException {
		long testValue = 8888L;
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(testValue);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.ECHO.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		simulate();

		assertTrue(state.isFinished);
		assertFalse(state.hadFatalError);
		assertEquals("Data does not match", testValue, state.dataByteBuffer.getLong(0 * MachineState.VALUE_SIZE));
	}

	@Test
	public void testInvalidOpCode() throws ExecutionException {
		codeByteBuffer.put((byte) 0xdd);

		simulate();

		assertTrue(state.isFinished);
		assertTrue(state.hadFatalError);
	}

}
