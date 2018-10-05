import static org.junit.Assert.*;

import org.ciyam.at.ExecutionException;
import org.ciyam.at.FunctionCode;
import org.ciyam.at.OpCode;
import org.junit.Test;

import common.ExecutableTest;

public class MiscTests extends ExecutableTest {

	@Test
	public void testSimpleCode() throws ExecutionException {
		long testValue = 8888L;
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(testValue);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.ECHO.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.getIsFinished());
		assertFalse(state.getHadFatalError());
		assertEquals("Data does not match", testValue, getData(0));
	}

	@Test
	public void testInvalidOpCode() throws ExecutionException {
		codeByteBuffer.put((byte) 0xdd);

		execute(true);

		assertTrue(state.getIsFinished());
		assertTrue(state.getHadFatalError());
	}

}
