package test.com.jera.vnc;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import com.jera.vnc.VNC;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: jbrewer
 * Date: Sep 10, 2003
 * Time: 10:33:46 PM
 * To change this template use Options | File Templates.
 */
public class VNC_test extends TestCase {
	byte[] challenge =
	        { (byte) 0xbd, (byte) 0xce, (byte) 0x51, (byte) 0xd6,
	          (byte) 0x8e, (byte) 0x41, (byte) 0x81, (byte) 0xe4,
	          (byte) 0xc3, (byte) 0xbe, (byte) 0xf5, (byte) 0x04,
	          (byte) 0x2f, (byte) 0x5d, (byte) 0xb8, (byte) 0x26 };

	byte[] expected_response =
	        { (byte) 0xc3, (byte) 0x92, (byte) 0x44, (byte) 0xb7,
	          (byte) 0x5e, (byte) 0xd1, (byte) 0xb3, (byte) 0x94,
	          (byte) 0x1f, (byte) 0xe2, (byte) 0x84, (byte) 0x15,
	          (byte) 0x42, (byte) 0x9b, (byte) 0x8e, (byte) 0x86 };

	public void test_authenticate() throws GeneralSecurityException, UnsupportedEncodingException {
		byte[] response = VNC.authenticate("vncpass", challenge);
		try {
			assertEquals(stringifyByteArray(expected_response), stringifyByteArray(response));
		}
		catch (AssertionFailedError e) {
			System.out.println();
			System.out.println(stringifyByteArray(expected_response));
			System.out.println(stringifyByteArray(response));
			throw e;
		}
	}

	public void test_getKeyBytes() throws UnsupportedEncodingException {
		byte[] expected_result = { (byte) 0xc2, (byte) 0xf2, (byte) 0xea, (byte) 0,
		                           (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
		byte[] result = VNC.getKeyBytes("COW");
		try {
			assertEquals("COW", stringifyByteArray(expected_result), stringifyByteArray(result));
		}
		catch (AssertionFailedError e) {
			System.out.println(stringifyByteArray(expected_result));
			System.out.println(stringifyByteArray(result));
			throw e;
		}
	}

	private String stringifyByteArray(byte[] theBytes) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("0x" + stringifyByte(theBytes[0]));

		for (int i=1; i < theBytes.length; i++) {
			buffer.append(", 0x" + stringifyByte(theBytes[i]));
		}

		return buffer.toString();
	}

	private String stringifyByte(byte theByte) {
		return Integer.toHexString(theByte & 0xff);
	}
}
