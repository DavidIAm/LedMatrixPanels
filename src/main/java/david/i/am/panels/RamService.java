package david.i.am.panels;

import java.io.File;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class RamService implements PanelService {

  public static final String PROC_MEMINFO = "/proc/meminfo";

  private final ProfileState profileState;

  public RamService(ProfileState profileState) {
    this.profileState = profileState;
  }

  @Override
  public String getProfileName() {
    return "ram";
  }

  @Override
  public void showLeft(CommunicationCreator left) {
    if (!isActive(profileState)) {
      return;
    }
    left.sendDraw(ramImage());
  }

  @Override
  public void showRight(CommunicationCreator right) {
    if (!isActive(profileState)) {
      return;
    }
    right.sendDraw(ramImage());
  }

  @Builder
  @Getter
  public static class Ram {
    private Integer total;       // MemTotal
    private Integer free;        // MemFree
    private Integer available;   // MemAvailable
    private Integer buffers;     // Buffers
    private Integer cached;      // Cached
    private Integer swapTotal;   // SwapTotal
    private Integer swapFree;    // SwapFree
  }
  public Ram ram() {
    File ueventFile = new File(PROC_MEMINFO);
    Ram.RamBuilder builder = Ram.builder();

    try (Scanner scanner = new Scanner(ueventFile)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.startsWith("MemTotal:")) {
          builder.total(Integer.parseInt(line.replaceAll("\\D+", ""))); // Extract digits
        } else if (line.startsWith("MemFree:")) {
          builder.free(Integer.parseInt(line.replaceAll("\\D+", "")));
        } else if (line.startsWith("MemAvailable:")) {
          builder.available(Integer.parseInt(line.replaceAll("\\D+", "")));
        } else if (line.startsWith("Buffers:")) {
          builder.buffers(Integer.parseInt(line.replaceAll("\\D+", "")));
        } else if (line.startsWith("Cached:")) {
          builder.cached(Integer.parseInt(line.replaceAll("\\D+", "")));
        } else if (line.startsWith("SwapTotal:")) {
          builder.swapTotal(Integer.parseInt(line.replaceAll("\\D+", "")));
        } else if (line.startsWith("SwapFree:")) {
          builder.swapFree(Integer.parseInt(line.replaceAll("\\D+", "")));
        }
      }
    } catch (Exception e) {
      throw new ParseError(
          "Failed to parse meminfo file: " + ueventFile.getAbsolutePath(), e);
    }

    return builder.build();
  }

  public byte[] ramImage() {
    Ram ram = ram();
    List<Integer> integers = Stream.concat(Stream.of(
                0b110010101,
                0b100010001,
                0b110011011,
                0b101010101,
                0b100110001,
                0b000000000),
            IntStream.rangeClosed(0, 33)
                .map(index -> (100 - ram.getTotal()) / 3 < index ? 0b111_111_111 : 0b000_000_000)
                .boxed())
        .toList();

    return BitUtils.packbits(integers);
  }
}