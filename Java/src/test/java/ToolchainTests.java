import static common.TestUtils.hexToBytes;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class ToolchainTests {

	@Test
	public void testHexToBytes() {
		assertTrue(Arrays.equals(new byte[] { 0x12 }, hexToBytes("12")));
		assertTrue(Arrays.equals(new byte[] { 0x00, 0x00, 0x12 }, hexToBytes("000012")));
		assertTrue(Arrays.equals(new byte[] { (byte) 0xff }, hexToBytes("ff")));
		assertTrue(Arrays.equals(new byte[] { 0x00, 0x00, (byte) 0xee }, hexToBytes("0000ee")));
	}

}
