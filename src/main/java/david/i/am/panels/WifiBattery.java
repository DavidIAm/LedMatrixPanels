package david.i.am.panels;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
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
  public static final String POWER_SUPPLY_BAT_1_UEVENT = "/sys/class/power_supply/BAT1/uevent";
  public static final String PROC_NET_WIRELESS = "/proc/net/wireless";

  private static final Map<String, BiConsumer<Battery.BatteryBuilder, String>> BATTERY_MAPPINGS = new HashMap<>();

  static {
    BATTERY_MAPPINGS.put("POWER_SUPPLY_NAME", Battery.BatteryBuilder::name);
    BATTERY_MAPPINGS.put("POWER_SUPPLY_TYPE", Battery.BatteryBuilder::type);
    BATTERY_MAPPINGS.put("POWER_SUPPLY_STATUS", Battery.BatteryBuilder::status);
    BATTERY_MAPPINGS.put("POWER_SUPPLY_PRESENT", (b, v) -> b.present(v.equals("1")));
    BATTERY_MAPPINGS.put("POWER_SUPPLY_CYCLE_COUNT", (b, v) -> b.cycleCount(Integer.parseInt(v)));
    BATTERY_MAPPINGS.put("POWER_SUPPLY_VOLTAGE_MIN_DESIGN", (b, v) -> b.voltageMinDesign(Long.parseLong(v)));
    BATTERY_MAPPINGS.put("POWER_SUPPLY_VOLTAGE_NOW", (b, v) -> b.voltageNow(Long.parseLong(v)));
    BATTERY_MAPPINGS.put("POWER_SUPPLY_CURRENT_NOW", (b, v) -> b.currentNow(Long.parseLong(v)));
    BATTERY_MAPPINGS.put("POWER_SUPPLY_CHARGE_FULL_DESIGN", (b, v) -> b.chargeFullDesign(Long.parseLong(v)));
    BATTERY_MAPPINGS.put("POWER_SUPPLY_CHARGE_FULL", (b, v) -> b.chargeFull(Long.parseLong(v)));
    BATTERY_MAPPINGS.put("POWER_SUPPLY_CHARGE_NOW", (b, v) -> b.chargeNow(Long.parseLong(v)));
    BATTERY_MAPPINGS.put("POWER_SUPPLY_CAPACITY", (b, v) -> b.capacity(Integer.parseInt(v)));
    BATTERY_MAPPINGS.put("POWER_SUPPLY_CAPACITY_LEVEL", Battery.BatteryBuilder::capacityLevel);
    BATTERY_MAPPINGS.put("POWER_SUPPLY_MODEL_NAME", Battery.BatteryBuilder::modelName);
    BATTERY_MAPPINGS.put("POWER_SUPPLY_MANUFACTURER", Battery.BatteryBuilder::manufacturer);
    BATTERY_MAPPINGS.put("POWER_SUPPLY_SERIAL_NUMBER", Battery.BatteryBuilder::serialNumber);
  }

  private final CommunicationCreator left;
  private final CommunicationCreator right;

  public WifiBattery(@org.springframework.beans.factory.annotation.Qualifier("left") CommunicationCreator left,
                     @org.springframework.beans.factory.annotation.Qualifier("right") CommunicationCreator right) {
    this.left = left;
    this.right = right;
  }

  @PostConstruct
  void init() {
    left.setBrightness(0x20);
    right.setBrightness(0x20);
  }

  @PreDestroy
  void destroy() {
    // nothing needed here
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

    return BitUtils.packbits(integers);
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
    File ueventFile = new File(POWER_SUPPLY_BAT_1_UEVENT);
    Battery.BatteryBuilder builder = Battery.builder();

    try (Scanner scanner = new Scanner(ueventFile)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();

        // Parse the key-value pair
        if (line.contains("=")) {
          String[] keyValue = line.split("=", 2);
          String key = keyValue[0].trim();
          String value = keyValue[1].trim();

          // Map the key-value pair to Battery fields using lookup table
          BiConsumer<Battery.BatteryBuilder, String> mapping = BATTERY_MAPPINGS.get(key);
          if (mapping != null) {
            mapping.accept(builder, value);
          }
        }
      }
    } catch (Exception e) {
      throw new ParseError("Failed to parse uevent file: " + ueventFile.getAbsolutePath(), e);
    }

    // Build and return the Battery object
    return builder.build();
  }

  public Wifi wireless() {
    File wirelessFile = new File(PROC_NET_WIRELESS);
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
      throw new ParseError("Failed to parse wireless file: " + wirelessFile.getAbsolutePath(), e);
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

    return BitUtils.packbits(integers);
  }

  
}
