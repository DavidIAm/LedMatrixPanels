package david.i.am.panels;

import java.util.List;

public class BitUtils {

    private BitUtils(){}
    /**
     * Packs a list of integers into a byte array, using 9 bits per integer.
     * This is a specific packing format used for drawing on panels.
     *
     * @param integers The list of integers to pack.
     * @return A byte array containing the packed bits.
     */
    public static byte[] packbits(List<Integer> integers) {
        final int totalBits = integers.size() * 9; // Total bits needed
        final int bufferSize = (int) Math.ceil(totalBits / 8.0); // Calculate number of bytes needed
        byte[] buffer = new byte[bufferSize];

        // Process and pack the 9-bit integers into the byte array
        int bitOffset = 0;
        for (Integer integer : integers) {
            int value = integer & 0x1FF; // Mask each integer to ensure it's 9 bits
            int byteIndex = bitOffset / 8;
            int bitInByte = bitOffset % 8;

            // Write the value into the current byte
            buffer[byteIndex] |= (byte) ((value << bitInByte) & 0xFF);

            // Handle spillover bits
            if (byteIndex + 1 < buffer.length) {
                buffer[byteIndex + 1] |= (byte) ((value >> (8 - bitInByte)) & 0xFF);
            }

            bitOffset += 9;
        }
        return buffer;
    }
}
