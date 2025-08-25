package megamiku.rpmprinter;

import org.junit.Test;

import megamiku.rpmprinter.datafeed.ObdPid;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
	@Test
	public void stringhexToNumber() {
		assertEquals(0, valueFromHexChar('0'));
		assertEquals(1, valueFromHexChar('1'));
		assertEquals(2, valueFromHexChar('2'));
		assertEquals(3, valueFromHexChar('3'));
		assertEquals(4, valueFromHexChar('4'));
		assertEquals(5, valueFromHexChar('5'));
		assertEquals(6, valueFromHexChar('6'));
		assertEquals(7, valueFromHexChar('7'));
		assertEquals(8, valueFromHexChar('8'));
		assertEquals(9, valueFromHexChar('9'));
		assertEquals(10, valueFromHexChar('A'));
		assertEquals(11, valueFromHexChar('B'));
		assertEquals(12, valueFromHexChar('C'));
		assertEquals(13, valueFromHexChar('D'));
		assertEquals(14, valueFromHexChar('E'));
		assertEquals(15, valueFromHexChar('F'));
	}

	private int valueFromHexChar(char c) {
		if (c >= '0' && c <= '9')
			return c-'0';
		if (c >= 'A' && c <= 'F')
			return c-'A'+10;
		throw new RuntimeException("Bad character sent : " + c);
	}

	@Test
	public void testDecodeString() {
		assertEquals(6904, getRpmNumber("41 0C 1A F8"));
	}

	private int getRpmNumber(String str) {
		int value = (valueFromHexChar(str.charAt(6)) << 12)
				+ (valueFromHexChar(str.charAt(7)) << 8)
				+ (valueFromHexChar(str.charAt(9)) << 4)
				+ (valueFromHexChar(str.charAt(10)));
		return value;
	}

	@Test
	public void writeCommands() throws InterruptedException {
		System.out.println("Hue");
		System.out.println(new String(ObdPid.ENGINE_RPM.getELMCommand()));
		System.out.println(new String(ObdPid.VEHICLE_SPEED.getELMCommand()));
		System.out.println(new String(ObdPid.ENGINE_COOLANT_TEMPERATURE.getELMCommand()));
	}
}