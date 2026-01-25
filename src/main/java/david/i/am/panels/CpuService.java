package david.i.am.panels;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("cpu")
public class CpuService {
  public static final String PROC_STAT = "/proc/stat";
  private final CommunicationCreator left;
  private final CommunicationCreator right;

  public CpuService(@org.springframework.beans.factory.annotation.Qualifier("left") CommunicationCreator left,
                    @org.springframework.beans.factory.annotation.Qualifier("right") CommunicationCreator right) {
    this.left = left;
    this.right = right;
  }

  // Field to store derivative CPU stats (derived percentages)
  private final AtomicReference<Cpu> cpuPercentageStats = new AtomicReference<>();

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
    left.sendDraw(cpuImageForRange("cpu0-7"));
  }

  @Scheduled(fixedRate = 2000)
  void showRight() {
    right.sendDraw(cpuImageForRange("cpu8-15"));
  }
  

  @Builder
  @Getter
  public static class Cpu {
    private Long totalUserTime;
    private Long totalNiceTime;
    private Long totalSystemTime;
    private Long totalIdleTime;
    private Long totalIowaitTime;
    private Long totalIrqTime;
    private Long totalSoftirqTime;

    private Long totalInterrupts;
    private Long totalContextSwitches;
    private Long bootTime;
    private Long totalProcesses;
    private Long runningProcesses;
    private Long blockedProcesses;

    @Singular
    private Map<String, CoreStats> coreStats;

    // Nested class for per-core statistics
    @Builder
    @Getter
    public static class CoreStats {
      private Long userTime;
      private Long niceTime;
      private Long systemTime;
      private Long idleTime;
      private Long iowaitTime;
      private Long irqTime;
      private Long softirqTime;
    }

  }

  public Cpu cpu() {
    File statsFile = new File(PROC_STAT);
    Cpu.CpuBuilder builder = Cpu.builder();

    try (Scanner scanner = new Scanner(statsFile)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();

        if (line.startsWith("cpu ")) {
          // Total CPU stats (summed across all CPUs)
          String[] parts = line.split("\\s+");
          builder.totalUserTime(Long.parseLong(parts[1]))
              .totalNiceTime(Long.parseLong(parts[2]))
              .totalSystemTime(Long.parseLong(parts[3]))
              .totalIdleTime(Long.parseLong(parts[4]))
              .totalIowaitTime(Long.parseLong(parts[5]))
              .totalIrqTime(Long.parseLong(parts[6]))
              .totalSoftirqTime(Long.parseLong(parts[7]));
        } else if (line.startsWith("cpu") && !line.startsWith("cpu ")) {
          // Per-core CPU stats (e.g., cpu0, cpu1, etc.)
          String[] parts = line.split("\\s+");
          Cpu.CoreStats coreStats = Cpu.CoreStats.builder()
              .userTime(Long.parseLong(parts[1]))
              .niceTime(Long.parseLong(parts[2]))
              .systemTime(Long.parseLong(parts[3]))
              .idleTime(Long.parseLong(parts[4]))
              .iowaitTime(Long.parseLong(parts[5]))
              .irqTime(Long.parseLong(parts[6]))
              .softirqTime(Long.parseLong(parts[7]))
              .build();
          builder.coreStat(parts[0], coreStats); // parts[0] = "cpu0", "cpu1", etc.
        } else if (line.startsWith("intr")) {
          // Total interrupts
          String[] parts = line.split("\\s+");
          builder.totalInterrupts(Long.parseLong(parts[1]));
        } else if (line.startsWith("ctxt")) {
          // Context switches
          builder.totalContextSwitches(Long.parseLong(line.split("\\s+")[1]));
        } else if (line.startsWith("btime")) {
          // Boot time
          builder.bootTime(Long.parseLong(line.split("\\s+")[1]));
        } else if (line.startsWith("processes")) {
          // Total number of processes
          builder.totalProcesses(Long.parseLong(line.split("\\s+")[1]));
        } else if (line.startsWith("procs_running")) {
          // Total number of running processes
          builder.runningProcesses(Long.parseLong(line.split("\\s+")[1]));
        } else if (line.startsWith("procs_blocked")) {
          // Total number of blocked processes
          builder.blockedProcesses(Long.parseLong(line.split("\\s+")[1]));
        }
      }
    } catch (Exception e) {
      throw new ParseError("Failed to parse stats file: " + statsFile.getAbsolutePath(), e);
    }

    // Build and return the Cpu object
    return builder.build();
  }
  byte[] cpuImageForRange(String range) {
    List<String> coreRange = switch (range.toLowerCase()) {
      case "cpu0-7" -> IntStream.rangeClosed(0, 7).mapToObj(i -> "cpu" + i).toList();
      case "cpu8-15" -> IntStream.rangeClosed(8, 15).mapToObj(i -> "cpu" + i).toList();
      default -> throw new IllegalArgumentException("Unsupported core range: " + range);
    };

    return cpuImage(coreRange);
  }
  byte[] cpuImage(List<String> coreRange) {
    Cpu cpu = cpuPercentageStats.get(); // Use precomputed percentages
    if (cpu == null) {
      return new byte[0]; // Fallback if percentage data is not yet ready
    }

    // Validate that the requested cores exist in the data
    List<String> filteredCores = coreRange.stream()
        .filter(core -> cpu.getCoreStats().containsKey(core))
        .toList();

    List<Integer> integers = Stream.concat(
            getFrameData().stream(),
            IntStream.rangeClosed(0, 33)
                .map(index -> calculateColumnBits(cpu, filteredCores, index))
                .boxed())
        .toList();

    return BitUtils.packbits(integers);
  }

  private List<Integer> getFrameData() {
    return List.of(
        0b111111111, // Top border
        0b100000001, // Frame
        0b100000001,
        0b101010101, // Decorative pattern
        0b111111111, // Bottom border
        0b000000000, // Space padding
        0b000000000  // Space padding
    );
  }

  private int calculateColumnBits(Cpu cpu, List<String> filteredCores, int index) {
    return filteredCores.stream()
        .mapToInt(core -> {
          Cpu.CoreStats stats = cpu.getCoreStats().get(core);
          if (stats == null) {
            return 0;
          }

          int userRows = scaleTo33(stats.getUserTime());
          int systemRows = scaleTo33(stats.getSystemTime());

          int userBit = (index < userRows) ? 1 : 0;
          int systemBit = (33 - index <= systemRows) ? 1 : 0;

          return userBit | systemBit;
        })
        .reduce(0, (accum, value) -> (accum << 1) | value);
  }

  private int scaleTo33(long time) {
    return Math.min(33, Math.max(0, (int) (time / 3)));
  }
  private final AtomicReference<Cpu> lastCpuStats = new AtomicReference<>(); // To store the previous CPU stats

  @Scheduled(fixedRate = 1000) // Runs every second
  void computeCpuPercentages() {
    Cpu currentCpuStats = cpu();
    Cpu last = lastCpuStats.get();
    if (last != null) {
      // Calculate the differences (percentages)
      Cpu.CpuBuilder percentageBuilder = Cpu.builder();

      long prevTotalTime = last.getTotalUserTime() +
          last.getTotalNiceTime() +
          last.getTotalSystemTime() +
          last.getTotalIdleTime() +
          last.getTotalIowaitTime() +
          last.getTotalIrqTime() +
          last.getTotalSoftirqTime();

      long currTotalTime = currentCpuStats.getTotalUserTime() +
          currentCpuStats.getTotalNiceTime() +
          currentCpuStats.getTotalSystemTime() +
          currentCpuStats.getTotalIdleTime() +
          currentCpuStats.getTotalIowaitTime() +
          currentCpuStats.getTotalIrqTime() +
          currentCpuStats.getTotalSoftirqTime();

      long totalDelta = currTotalTime - prevTotalTime;

      if (totalDelta > 0) {
        percentageBuilder
            .totalUserTime((currentCpuStats.getTotalUserTime() - last.getTotalUserTime()) * 100 / totalDelta)
            .totalNiceTime((currentCpuStats.getTotalNiceTime() - last.getTotalNiceTime()) * 100 / totalDelta)
            .totalSystemTime((currentCpuStats.getTotalSystemTime() - last.getTotalSystemTime()) * 100 / totalDelta)
            .totalIdleTime((currentCpuStats.getTotalIdleTime() - last.getTotalIdleTime()) * 100 / totalDelta)
            .totalIowaitTime((currentCpuStats.getTotalIowaitTime() - last.getTotalIowaitTime()) * 100 / totalDelta)
            .totalIrqTime((currentCpuStats.getTotalIrqTime() - last.getTotalIrqTime()) * 100 / totalDelta)
            .totalSoftirqTime((currentCpuStats.getTotalSoftirqTime() - last.getTotalSoftirqTime()) * 100 / totalDelta);

        // Per-core stats
        currentCpuStats.getCoreStats().forEach((core, coreStats) -> {
          Cpu.CoreStats lastCoreStats = last.getCoreStats().get(core);
          if (lastCoreStats != null) {
            long corePrevTotal = lastCoreStats.getUserTime() +
                lastCoreStats.getNiceTime() +
                lastCoreStats.getSystemTime() +
                lastCoreStats.getIdleTime() +
                lastCoreStats.getIowaitTime() +
                lastCoreStats.getIrqTime() +
                lastCoreStats.getSoftirqTime();

            long coreCurrentTotal = coreStats.getUserTime() +
                coreStats.getNiceTime() +
                coreStats.getSystemTime() +
                coreStats.getIdleTime() +
                coreStats.getIowaitTime() +
                coreStats.getIrqTime() +
                coreStats.getSoftirqTime();

            long coreTotalDelta = coreCurrentTotal - corePrevTotal;

            if (coreTotalDelta > 0) {
              percentageBuilder.coreStat(core, Cpu.CoreStats.builder()
                  .userTime((coreStats.getUserTime() - lastCoreStats.getUserTime()) * 100 / coreTotalDelta)
                  .niceTime((coreStats.getNiceTime() - lastCoreStats.getNiceTime()) * 100 / coreTotalDelta)
                  .systemTime((coreStats.getSystemTime() - lastCoreStats.getSystemTime()) * 100 / coreTotalDelta)
                  .idleTime((coreStats.getIdleTime() - lastCoreStats.getIdleTime()) * 100 / coreTotalDelta)
                  .iowaitTime((coreStats.getIowaitTime() - lastCoreStats.getIowaitTime()) * 100 / coreTotalDelta)
                  .irqTime((coreStats.getIrqTime() - lastCoreStats.getIrqTime()) * 100 / coreTotalDelta)
                  .softirqTime((coreStats.getSoftirqTime() - lastCoreStats.getSoftirqTime()) * 100 / coreTotalDelta)
                  .build());
            }
          }
        });
      }

      cpuPercentageStats.set(percentageBuilder.build());
    }
    // Update the last stats
    lastCpuStats.set(currentCpuStats);
  }
  
}
