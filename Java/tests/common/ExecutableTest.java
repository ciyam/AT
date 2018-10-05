package common;

import static common.TestUtils.hexToBytes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ciyam.at.MachineState;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class ExecutableTest {

	public static final int CODE_OFFSET = 6 * 2;
	public static final int DATA_OFFSET = CODE_OFFSET + 0x0200;
	public static final int CALL_STACK_OFFSET = DATA_OFFSET + 0x0020 * 8;

	public TestLogger logger;
	public TestAPI api;
	public MachineState state;
	public ByteBuffer codeByteBuffer;
	public ByteBuffer stateByteBuffer;
	public int callStackSize;
	public int userStackOffset;
	public int userStackSize;

	@BeforeClass
	public static void beforeClass() {
		Security.insertProviderAt(new BouncyCastleProvider(), 0);
	}

	@Before
	public void beforeTest() {
		logger = new TestLogger();
		api = new TestAPI();
		codeByteBuffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
		stateByteBuffer = null;
	}

	@After
	public void afterTest() {
		stateByteBuffer = null;
		codeByteBuffer = null;
		api = null;
		logger = null;
	}

	protected void execute(boolean onceOnly) {
		// version 0003, reserved 0000, code 0200 * 1, data 0020 * 8, call stack 0010 * 4, user stack 0010 * 8
		byte[] headerBytes = hexToBytes("0300" + "0000" + "0002" + "2000" + "1000" + "1000");
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = new byte[0];

		state = new MachineState(api, logger, headerBytes, codeBytes, dataBytes);

		do {
			System.out.println("Starting execution:");
			System.out.println("Current block height: " + api.getCurrentBlockHeight());

			// Actual execution
			state.execute();

			System.out.println("After execution:");
			System.out.println("Steps: " + state.getSteps());
			System.out.println("Program Counter: " + String.format("%04x", state.getProgramCounter()));
			System.out.println("Stop Address: " + String.format("%04x", state.getOnStopAddress()));
			System.out.println("Error Address: " + (state.getOnErrorAddress() == null ? "not set" : String.format("%04x", state.getOnErrorAddress())));

			if (state.getIsSleeping())
				System.out.println("Sleeping until current block height (" + state.getCurrentBlockHeight() + ") reaches " + state.getSleepUntilHeight());
			else
				System.out.println("Sleeping: " + state.getIsSleeping());

			System.out.println("Stopped: " + state.getIsStopped());
			System.out.println("Finished: " + state.getIsFinished());

			if (state.getHadFatalError())
				System.out.println("Finished due to fatal error!");

			System.out.println("Frozen: " + state.getIsFrozen());

			// Bump block height
			api.bumpCurrentBlockHeight();
		} while (!onceOnly && !state.getIsFinished());

		// Ready for diagnosis
		byte[] stateBytes = state.toBytes();

		// We know how the state will be serialized so we can extract values
		// header(6) + code(0x0200) + data(0x0020 * 8) + callStack length(4) + callStack + userStack length(4) + userStack

		stateByteBuffer = ByteBuffer.wrap(stateBytes).order(ByteOrder.LITTLE_ENDIAN);
		callStackSize = stateByteBuffer.getInt(CALL_STACK_OFFSET);
		userStackOffset = CALL_STACK_OFFSET + 4 + callStackSize;
		userStackSize = stateByteBuffer.getInt(userStackOffset);
	}

	protected long getData(int address) {
		int index = DATA_OFFSET + address * MachineState.VALUE_SIZE;
		return stateByteBuffer.getLong(index);
	}

	protected int getCallStackPosition() {
		return 0x0010 * MachineState.ADDRESS_SIZE - callStackSize;
	}

	protected int getCallStackEntry(int address) {
		int index = CALL_STACK_OFFSET + 4 + address - 0x0010 * MachineState.ADDRESS_SIZE + callStackSize;
		return stateByteBuffer.getInt(index);
	}

	protected int getUserStackPosition() {
		return 0x0010 * MachineState.VALUE_SIZE - userStackSize;
	}

	protected long getUserStackEntry(int address) {
		int index = userStackOffset + 4 + address - 0x0010 * MachineState.VALUE_SIZE + userStackSize;
		return stateByteBuffer.getLong(index);
	}

}
