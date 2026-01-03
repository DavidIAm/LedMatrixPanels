package david.i.am.panels;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("wifibattery")
public class WifiBattery {
  private CommunicationCreator left;
  private CommunicationCreator right;

  @PostConstruct
  void init() {
    left = new CommunicationCreator("/dev/ttyACM0", 115200);
    left.setBrightness(0x20);
    right = new CommunicationCreator("/dev/ttyACM1", 115200);
    right.setBrightness(0x20);
  }

  @PreDestroy
  void destroy() {
    left.close();
    right.close();
  }

  @Scheduled(initialDelay = 1000, fixedRate = 2000)
  void showLeft() {
    left.sendDraw(wirelessImage());
  }

  @Scheduled(fixedRate = 2000)
  void showRight() {
    right.sendDraw(batteryImage());
  }
  
  byte[] batteryImage() {
    Battery battery = battery();
    List<Integer> integers = Stream.concat(Stream.of(
        0b011111111,
        0b110100001,
        0b110100001,
        0b011111111,
        0b000000000,
        0b111111111),
        IntStream.rangeClosed(0, 33)
            .map(index -> (100 - battery.getCapacity()) / 3 < index ? 0b111_111_111 : 0b000_000_000 ).boxed())
        .toList();

//    System.out.println("----------------------");
//    integers.stream().map(Integer::toBinaryString).forEach(n -> System.out.println(new StringBuilder(String.format("%9s", n).replace(' ', '0')).reverse().toString()));
    return packbits(integers);
  }

  private static byte[] packbits(List<Integer> integers) {
    final int totalBits = integers.size() * 9; // Total bits needed
    final int bufferSize = (int) Math.ceil(totalBits / 8.0); // Calculate number of bytes needed
    byte[] buffer = new byte[bufferSize];

    // Process and pack the 9-bit integers into the byte array
    integers.stream()
        .map(i -> i & 0x1FF) // Mask each integer to ensure it's 9 bits
        .reduce(new int[]{0, 0}, // Accumulator: [current bit offset, byte index]
            (acc, value) -> {
              int bitOffset = acc[0];
              int byteIndex = acc[1];

              // Write the value into the current byte

              buffer[byteIndex] |= (value << bitOffset % 8) & 0xFF;

              // Handle spillover bits
              if (bitOffset > 7 - 9) {
                buffer[byteIndex + 1] |= (value >> (8 - bitOffset % 8));
              }

              // Update the bit offset and byte index
              bitOffset += 9;
              byteIndex = bitOffset / 8;

              return new int[]{bitOffset, byteIndex};
            },
            (acc1, acc2) -> acc1 // Combiner (not needed here as we're dealing with sequential stream)
        );
    return buffer;
  }

  @Builder
  @Getter
  public static class Wifi {
    private Integer link;
    private Integer level;
    private Integer noise;
  }

  @Builder
  @Getter
  public static class Battery {
    private String devtype;
    private String name;
    private String type;
    private String status;
    private boolean present;
    private String technology;
    private long cycleCount;
    private long voltageMinDesign;
    private long voltageNow;
    private long currentNow;
    private long chargeFullDesign;
    private long chargeFull;
    private long chargeNow;
    private long capacity;
    private String capacityLevel;
    private String modelName;
    private String manufacturer;
    private String serialNumber;
  }

  public Battery battery() {
    File ueventFile = new File("/sys/class/power_supply/BAT1/uevent");
    Battery.BatteryBuilder builder = Battery.builder();

    try (Scanner scanner = new Scanner(ueventFile)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();

        // Parse the key-value pair
        if (line.contains("=")) {
          String[] keyValue = line.split("=", 2);
          String key = keyValue[0].trim();
          String value = keyValue[1].trim();

          // Map the key-value pair to Battery fields
          switch (key) {
            case "POWER_SUPPLY_NAME":
              builder.name(value);
              break;
            case "POWER_SUPPLY_TYPE":
              builder.type(value);
              break;
            case "POWER_SUPPLY_STATUS":
              builder.status(value);
              break;
            case "POWER_SUPPLY_PRESENT":
              builder.present(value.equals("1"));
              break;
            case "POWER_SUPPLY_CYCLE_COUNT":
              builder.cycleCount(Integer.parseInt(value));
              break;
            case "POWER_SUPPLY_VOLTAGE_MIN_DESIGN":
              builder.voltageMinDesign(Long.parseLong(value));
              break;
            case "POWER_SUPPLY_VOLTAGE_NOW":
              builder.voltageNow(Long.parseLong(value));
              break;
            case "POWER_SUPPLY_CURRENT_NOW":
              builder.currentNow(Long.parseLong(value));
              break;
            case "POWER_SUPPLY_CHARGE_FULL_DESIGN":
              builder.chargeFullDesign(Long.parseLong(value));
              break;
            case "POWER_SUPPLY_CHARGE_FULL":
              builder.chargeFull(Long.parseLong(value));
              break;
            case "POWER_SUPPLY_CHARGE_NOW":
              builder.chargeNow(Long.parseLong(value));
              break;
            case "POWER_SUPPLY_CAPACITY":
              builder.capacity(Integer.parseInt(value));
              break;
            case "POWER_SUPPLY_CAPACITY_LEVEL":
              builder.capacityLevel(value);
              break;
            case "POWER_SUPPLY_MODEL_NAME":
              builder.modelName(value);
              break;
            case "POWER_SUPPLY_MANUFACTURER":
              builder.manufacturer(value);
              break;
            case "POWER_SUPPLY_SERIAL_NUMBER":
              builder.serialNumber(value);
              break;
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse uevent file: " + ueventFile.getAbsolutePath(), e);
    }

    // Build and return the Battery object
    return builder.build();
  }

  public Wifi wireless() {
    File wirelessFile = new File("/proc/net/wireless");
    try (Scanner scanner = new Scanner(wirelessFile)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.contains("wlp1s0:")) { // Identify the WiFi interface
          // Extract link, level, and noise based on character ranges
          int link = Integer.parseInt(line.substring(12, 17).trim());
          int level = Integer.parseInt(line.substring(19, 23).trim());
          int noise = Integer.parseInt(line.substring(25, 30).trim());

          // Build and return the Wifi object
          return Wifi.builder().link(link).level(level).noise(noise).build();
        }
      }
    } catch (Exception e) {
      e.printStackTrace(); // Handle exception (e.g., file not found or parse error)
    }
    return null; // Return null if no data is found
  }

  byte[] wirelessImage() {
    Wifi wifi = wireless();
    List<Integer> integers = Stream.concat(Stream.of(
                0b001010100,
                0b000101010,
                0b110010101,
                0b110010101,
                0b000101010,
                0b001010100,
                0b000000000),
            IntStream.rangeClosed(0, 33)
                .map(index ->
                    (((100-wifi.link)/3 < index ? 0b111_111_111 : 0b000_0000_000) & 0b111_000_000) |
                    (((90+(wifi.level + 30)) / 2 < index ?  0b111_111_111 : 0b000_0000_000) & 0b000_110_000) |
                    (((-1 * (wifi.noise + 30)) / 6 < index ?  0b111_111_111 : 0b000_0000_000) & 0b000_001_100) |
                    (((200-(wifi.level - wifi.noise)) / 6 < index ?  0b111_111_111 : 0b000_0000_000) & 0b000_000_011)
                ).boxed())
        .toList();

//System.out.println("----------------------");
    //System.out.println(String.format("link %d ; %d ; %d", wifi.link, 100 - wifi.link, ((100 - wifi.link) / 3)));
    //System.out.println(String.format("level %d ; %d ; %d ; %d", wifi.level, wifi.level + 30, (90 + (wifi.level + 30)), (90 + (wifi.level + 30)) / 2));
    //System.out.println(String.format("noise %d ; %d ; %d ", wifi.noise, wifi.noise + 30, -1 * (wifi.noise + 30) / 6));
    //System.out.println(String.format("SNR %d ; %d ; %d", wifi.level - wifi.noise, (wifi.level - wifi.noise)-100, (wifi.level - wifi.noise)-100 / 2));
    //System.out.println("----------------------");
  //integers.stream().map(Integer::toBinaryString).forEach(n -> System.out.println(new StringBuilder(String.format("%9s", n).replace(' ', '0')).reverse().toString()));
    return packbits(integers);
  }

  
}
