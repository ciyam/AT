import static common.TestUtils.hexToBytes;

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

public class DisassemblyTests {

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

	@Test
	public void testMD160disassembly() throws ExecutionException {
		// MD160 of ffffffffffffffffffffffffffffffffffffffffffffffff is 90e735014ea23aa89190121b229c06d58fc71e83
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("ffffffffffffffff"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A1.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A2.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A3.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A4.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("90e735014ea23aa8"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B1.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("9190121b229c06d5"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B2.value).putInt(0);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("8fc71e8300000000"));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B3.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_HASH160_A_WITH_B.value).putInt(1);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.ECHO.value).putInt(1);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// version 0003, reserved 0000, code 0200 * 1, data 0020 * 8, call stack 0010 * 4, user stack 0010 * 4
		byte[] headerBytes = hexToBytes("0300" + "0000" + "0002" + "2000" + "1000" + "1000");
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = new byte[0];

		state = new MachineState(api, logger, headerBytes, codeBytes, dataBytes);

		System.out.println(state.disassemble());
	}

	@Test
	public void testACCTdisassembly() throws ExecutionException {
		codeByteBuffer.put(hexToBytes("3501030900000006040000000900000029302009000000040000000f1ab4000000330403090000003525010a000000260a00"));
		codeByteBuffer.put(hexToBytes("0000320903350703090000003526010a0000001b0a000000cd32280133160100000000331701010000003318010200000033"));
		codeByteBuffer.put(hexToBytes("1901030000003505020a0000001b0a000000a1320b033205041e050000001833000509000000320a033203041ab400000033"));
		codeByteBuffer.put(hexToBytes("160105000000331701060000003318010700000033190108000000320304320b033203041ab7000000000000000000000000"));
		codeByteBuffer.put(hexToBytes("0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"));
		codeByteBuffer.put(hexToBytes("000000000000"));

		// version 0003, reserved 0000, code 0200 * 1, data 0020 * 8, call stack 0010 * 4, user stack 0010 * 4
		byte[] headerBytes = hexToBytes("0300" + "0000" + "0002" + "2000" + "1000" + "1000");
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = new byte[0];

		state = new MachineState(api, logger, headerBytes, codeBytes, dataBytes);

		System.out.println(state.disassemble());
	}

}
