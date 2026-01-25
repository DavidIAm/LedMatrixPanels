package david.i.am.panels;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BitUtilsTest {

    @Test
    void testSingleValue() {
        List<Integer> input = List.of(0b101010101);
        // value = 0x155 (9 bits)
        // bitOffset = 0, bitInByte = 0, byteIndex = 0
        // buffer[0] |= (0x155 << 0) & 0xFF -> 0x55
        // buffer[1] |= (0x155 >> 8) & 0xFF -> 0x01
        // buffer = [0x55, 0x01]
        byte[] expected = {(byte) 0x55, (byte) 0x01};
        assertArrayEquals(expected, BitUtils.packbits(input));
    }

    @Test
    void testTwoValues() {
        List<Integer> input = Arrays.asList(0b111111111, 0b000000000);
        // Value 1: 0x1FF
        // bitOffset = 0, byteIndex = 0, bitInByte = 0
        // buffer[0] |= (0x1FF << 0) & 0xFF -> 0xFF
        // buffer[1] |= (0x1FF >> 8) & 0xFF -> 0x01
        // bitOffset = 9
        // Value 2: 0x000
        // bitOffset = 9, byteIndex = 1, bitInByte = 1
        // buffer[1] |= (0x000 << 1) & 0xFF -> remains 0x01
        // buffer[2] |= (0x000 >> 7) & 0xFF -> 0x00
        // buffer = [0xFF, 0x01, 0x00]
        byte[] expected = {(byte) 0xFF, (byte) 0x01, (byte) 0x00};
        assertArrayEquals(expected, BitUtils.packbits(input));
    }

    @Test
    void testValuesSpanningBytes() {
        List<Integer> input = Arrays.asList(0b111111111, 0b000000000, 0b111111111);
        // After first two values: [0xFF, 0x01, 0x00]
        // bitOffset = 18
        // Value 3: 0x1FF
        // bitOffset = 18, byteIndex = 2, bitInByte = 2
        // buffer[2] |= (0x1FF << 2) & 0xFF -> 0xFC
        // buffer[3] |= (0x1FF >> 6) & 0xFF -> 0x07
        // buffer = [0xFF, 0x01, 0xFC, 0x07]
        byte[] expected = {(byte) 0xFF, (byte) 0x01, (byte) 0xFC, (byte) 0x07};
        assertArrayEquals(expected, BitUtils.packbits(input));
    }
}
