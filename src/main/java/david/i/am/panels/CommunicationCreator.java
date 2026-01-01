package david.i.am.panels;

import com.fazecast.jSerialComm.SerialPort;

public class CommunicationCreator {

    private static final byte[] HEADER = {(byte) 0x32, (byte) 0xAC}; // Fixed header for all commands
    private final SerialPort serialPort;

    public CommunicationCreator(String portName, int baudRate) {
        // Opens and configures the serial port
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (!serialPort.openPort()) {
            throw new IllegalStateException("Failed to open port: " + portName);
        }
        System.out.println("Serial Port " + portName + " opened at baud rate " + baudRate);
    }

    /**
     * Sends a command packet over the serial connection.
     *
     * @param command The Command Enum value (1 byte).
     * @param payload   The payload (variable length, can be null).
     */
    public void sendCommand(CommandVals command, byte[] payload) {
        byte commandId = command.getValue();
        // Calculate payload length
        byte payloadLength = (byte) (payload != null ? payload.length : 0);

        // Build the packet
        byte[] packet = new byte[3 + payloadLength]; // Header (2 bytes) + Command ID + Payload Length + Payload
        packet[0] = HEADER[0]; // Header (byte 1)
        packet[1] = HEADER[1]; // Header (byte 2)
        packet[2] = commandId; // Command ID
        //packet[3] = payloadLength; // Payload Length

        // Append payload data if available
        if (payload != null) {
            System.arraycopy(payload, 0, packet, 3, payloadLength);
        }

        // Write the packet to the serial port
        serialPort.writeBytes(packet, packet.length);
    }

    /**
     * Sends a brightness command (Command ID: 0x01).
     *
     * @param brightness Brightness value (0–255).
     */
    public void setBrightness(int brightness) {
        sendCommand(CommandVals.BRIGHTNESS, new byte[]{(byte) brightness});
    }

    public void setDisplayOn() {
        sendCommand(CommandVals.DISPLAY_ON, null);
    }

    public void sendDraw(byte[] drawData) {
        sendCommand(CommandVals.DRAW, drawData);
    }

    /// CommandVals enum translated to Java
    public enum CommandVals {
        BRIGHTNESS((byte) 0x00),
        PATTERN((byte) 0x01),
        BOOTLOADER_RESET((byte) 0x02),
        SLEEP((byte) 0x03),
        ANIMATE((byte) 0x04),
        PANIC((byte) 0x05),
        DRAW((byte) 0x06),
        STAGE_GREY_COL((byte) 0x07),
        DRAW_GREY_COL_BUFFER((byte) 0x08),
        SET_TEXT((byte) 0x09),
        START_GAME((byte) 0x10),
        GAME_CONTROL((byte) 0x11),
        GAME_STATUS((byte) 0x12),
        SET_COLOR((byte) 0x13),
        DISPLAY_ON((byte) 0x14),
        INVERT_SCREEN((byte) 0x15),
        SET_PIXEL_COLUMN((byte) 0x16),
        FLUSH_FRAMEBUFFER((byte) 0x17),
        CLEAR_RAM((byte) 0x18),
        SCREEN_SAVER((byte) 0x19),
        SET_FPS((byte) 0x1A),
        SET_POWER_MODE((byte) 0x1B),
        ANIMATION_PERIOD((byte) 0x1C),
        PWM_FREQ((byte) 0x1E),
        DEBUG_MODE((byte) 0x1F),
        VERSION((byte) 0x20);

        private final byte value;

        CommandVals(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    /**
     * Closes the serial connection.
     */
    public void close() {
        if (serialPort.closePort()) {
            System.out.println("Serial port closed successfully.");
        } else {
            System.out.println("Failed to close the serial port.");
        }
    }

    /**
     * Helper function to convert a byte array to a Hex String (for debugging).
     *
     * @param bytes Byte array to convert.
     * @return Hexadecimal representation of the byte array.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Requests raw frame data (using Command ID: 0x05) and reads the response.
     *
     * @return A 32-byte array containing the raw frame data.
     * @throws IllegalStateException If the response is invalid or not 306 bytes.
     */
    public byte[] read() {
        byte[] frameData = new byte[32];
        int bytesRead = serialPort.readBytes(frameData, frameData.length);
        System.out.println("Raw frame data received: " + bytesRead + " - " + bytesToHex(frameData));
        while (frameData[0] != 0x00 && frameData[31] != 0x00) {
            frameData = new byte[32];
            bytesRead = serialPort.readBytes(frameData, frameData.length);
            System.out.println("Raw frame data received: " + bytesRead + " - " + bytesToHex(frameData));
        }
        return frameData;
    }

}