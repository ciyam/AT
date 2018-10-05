import static common.TestUtils.hexToBytes;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.MachineState;
import org.ciyam.at.OpCode;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import common.ACCTAPI;
import common.TestLogger;

public class TestACCT {

	public TestLogger logger;
	public ACCTAPI api;
	public MachineState state;
	public ByteBuffer codeByteBuffer;
	public ByteBuffer dataByteBuffer;

	@BeforeClass
	public static void beforeClass() {
		Security.insertProviderAt(new BouncyCastleProvider(), 0);
	}

	@Before
	public void beforeTest() {
		logger = new TestLogger();
		api = new ACCTAPI();
		codeByteBuffer = ByteBuffer.allocate(0x0200 * 1).order(ByteOrder.LITTLE_ENDIAN);
		dataByteBuffer = ByteBuffer.allocate(0x0020 * 8).order(ByteOrder.LITTLE_ENDIAN);
	}

	@After
	public void afterTest() {
		dataByteBuffer = null;
		codeByteBuffer = null;
		api = null;
		logger = null;
	}

	private byte[] simulate() {
		// version 0003, reserved 0000, code 0200 * 1, data 0020 * 8, call stack 0010 * 4, user stack 0010 * 4
		byte[] headerBytes = hexToBytes("0300" + "0000" + "0002" + "2000" + "1000" + "1000");
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = dataByteBuffer.array();

		state = new MachineState(api, logger, headerBytes, codeBytes, dataBytes);

		return executeAndCheck(state);
	}

	private byte[] continueSimulation(byte[] savedState) {
		state = MachineState.fromBytes(api, logger, savedState);

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
	public void testACCT() throws ExecutionException {
		// DATA
		final int addrHashPart1 = 0x0;
		final int addrHashPart2 = 0x1;
		final int addrHashPart3 = 0x2;
		final int addrHashPart4 = 0x3;
		final int addrAddressPart1 = 0x4;
		final int addrAddressPart2 = 0x5;
		final int addrAddressPart3 = 0x6;
		final int addrAddressPart4 = 0x7;
		final int addrRefundMinutes = 0x8;
		final int addrRefundTimestamp = 0x9;
		final int addrLastTimestamp = 0xa;
		final int addrBlockTimestamp = 0xb;
		final int addrTxType = 0xc;
		final int addrComparator = 0xd;
		final int addrAddressTemp1 = 0xe;
		final int addrAddressTemp2 = 0xf;
		final int addrAddressTemp3 = 0x10;
		final int addrAddressTemp4 = 0x11;

		byte[] secret = new byte[32];
		new SecureRandom().nextBytes(secret);

		try {
			MessageDigest digester = MessageDigest.getInstance("SHA-256");
			byte[] digest = digester.digest(secret);

			dataByteBuffer.put(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new ExecutionException("No SHA-256 message digest service available", e);
		}

		// Destination address (based on "R" for "Responder", where "R" is 0x52)
		dataByteBuffer.put(hexToBytes("5200000000000000520000000000000052000000000000005200000000000000"));

		// Expiry in minutes (but actually blocks in this test case)
		dataByteBuffer.putLong(8L);

		// Code labels
		final int addrTxLoop = 0x36;
		final int addrCheckTx = 0x4b;
		final int addrCheckSender = 0x64;
		final int addrCheckMessage = 0xab;
		final int addrPayout = 0xdf;
		final int addrRefund = 0x102;

		int tempPC;

		// init:
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_CREATION_TIMESTAMP.value).putInt(addrRefundTimestamp);
		codeByteBuffer.put(OpCode.SET_DAT.value).putInt(addrLastTimestamp).putInt(addrRefundTimestamp);
		codeByteBuffer.put(OpCode.EXT_FUN_RET_DAT_2.value).putShort(FunctionCode.ADD_MINUTES_TO_TIMESTAMP.value).putInt(addrRefundTimestamp)
				.putInt(addrRefundTimestamp).putInt(addrRefundMinutes);
		codeByteBuffer.put(OpCode.SET_PCS.value);

		// loop:
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_BLOCK_TIMESTAMP.value).putInt(addrBlockTimestamp);
		tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BLT_DAT.value).putInt(addrBlockTimestamp).putInt(addrRefundTimestamp).put((byte) (addrTxLoop - tempPC));
		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(addrRefund);

		// txloop:
		assertEquals(addrTxLoop, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_IN_A.value).putInt(addrLastTimestamp);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_A_IS_ZERO.value).putInt(addrComparator);
		tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(addrComparator).put((byte) (addrCheckTx - tempPC));
		codeByteBuffer.put(OpCode.STP_IMD.value);

		// checkTx:
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_TIMESTAMP_FROM_TX_IN_A.value).putInt(addrLastTimestamp);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_TYPE_FROM_TX_IN_A.value).putInt(addrTxType);
		tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNZ_DAT.value).putInt(addrTxType).put((byte) (addrCheckSender - tempPC));
		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(addrTxLoop);

		// checkSender
		assertEquals(addrCheckSender, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PUT_ADDRESS_FROM_TX_IN_A_INTO_B.value);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_B1.value).putInt(addrAddressTemp1);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_B2.value).putInt(addrAddressTemp2);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_B3.value).putInt(addrAddressTemp3);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_B4.value).putInt(addrAddressTemp4);
		tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNE_DAT.value).putInt(addrAddressTemp1).putInt(addrAddressPart1).put((byte) (addrTxLoop - tempPC));
		tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNE_DAT.value).putInt(addrAddressTemp2).putInt(addrAddressPart2).put((byte) (addrTxLoop - tempPC));
		tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNE_DAT.value).putInt(addrAddressTemp3).putInt(addrAddressPart3).put((byte) (addrTxLoop - tempPC));
		tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNE_DAT.value).putInt(addrAddressTemp4).putInt(addrAddressPart4).put((byte) (addrTxLoop - tempPC));

		// checkMessage:
		assertEquals(addrCheckMessage, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PUT_MESSAGE_FROM_TX_IN_A_INTO_B.value);
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.SWAP_A_AND_B.value);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B1.value).putInt(addrHashPart1);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B2.value).putInt(addrHashPart2);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B3.value).putInt(addrHashPart3);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B4.value).putInt(addrHashPart4);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_SHA256_A_WITH_B.value).putInt(addrComparator);
		tempPC = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BNZ_DAT.value).putInt(addrComparator).put((byte) (addrPayout - tempPC));
		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(addrTxLoop);

		// payout:
		assertEquals(addrPayout, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B1.value).putInt(addrAddressPart1);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B2.value).putInt(addrAddressPart2);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B3.value).putInt(addrAddressPart3);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B4.value).putInt(addrAddressPart4);
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.MESSAGE_A_TO_ADDRESS_IN_B.value);
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PAY_ALL_TO_ADDRESS_IN_B.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// refund:
		assertEquals(addrRefund, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PUT_CREATOR_INTO_B.value);
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PAY_ALL_TO_ADDRESS_IN_B.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		byte[] savedState = simulate();

		while (!state.getIsFinished()) {
			((ACCTAPI) state.getAPI()).generateNextBlock(secret);

			savedState = continueSimulation(savedState);
		}
	}

}
