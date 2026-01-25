package david.i.am.panels;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Profile("shimmer")
@Slf4j
@Service
@Data
public class ShimmerService {
  private static final int FRAME_RATE = 5; // frames per second
  private static final long FRAME_TIME_MS = 1000 / FRAME_RATE;
  private static final int INTER_COLUMN_DELAY_MS = 2;
  private static final int MATRIX_WIDTH = 9;
  private static final int MATRIX_HEIGHT = 34;

  private final AtomicInteger position = new AtomicInteger(0);
  private final List<Integer> gradientPattern;
  private final AtomicInteger frameCount = new AtomicInteger(0);
  private final long startTime = System.currentTimeMillis();


  private final CommunicationCreator left;
  private final CommunicationCreator right;

  public ShimmerService(@Qualifier("left") CommunicationCreator left, @Qualifier("right") CommunicationCreator right) {
    // Initialize gradient pattern: sin function, 20 dots per 360 degrees
    List<Integer> pattern = new ArrayList<>();
    int dotsPerCycle = 20;
    int maxVal = 31;
    for (int i = 0; i < dotsPerCycle; i++) {
      // sin(x) goes from -1 to 1. 
      // We want to map it to 0 to maxVal.
      // (sin(x) + 1) / 2 goes from 0 to 1.
      double angle = 2 * Math.PI * i / dotsPerCycle;
      double sinVal = Math.sin(angle);
      int val = (int) Math.round(((sinVal + 1) / 2.0) * maxVal);
      pattern.add(val);
    }
    this.gradientPattern = Collections.unmodifiableList(pattern);
    this.left = left;
    this.right = right;
  }

  @PostConstruct
  public void init() {
    if (left == null) {
      log.error("Left Device communication not initialized!");
      return;
    }
    if (right == null) {
      log.error("Right Device communication not initialized!");
      return;
    }
    log.info("Device communication initialized successfully");
    log.info("Starting shimmer animation via scheduler");
  }

  @Scheduled(fixedRate = FRAME_TIME_MS)
  public void animate() {
    int pos = position.getAndIncrement() % gradientPattern.size();

    CompletableFuture<Void> rightFuture = CompletableFuture.runAsync(() -> updateDisplay(right, pos, false, false));
    CompletableFuture<Void> leftFuture = CompletableFuture.runAsync(() -> updateDisplay(left, pos, true, true));

    CompletableFuture.allOf(rightFuture, leftFuture).join();

    int count = frameCount.incrementAndGet();
    if (count % 10 == 0) {  // Log FPS every 10 frames
      long currentTime = System.currentTimeMillis();
      float fps = 1000.0f * count / (currentTime - startTime);
      log.info("Current FPS: {}", String.format("%.2f", fps));
    }
  }

  private void updateDisplay(CommunicationCreator device, int startPosition, boolean invertAngle, boolean reverseDirection) {
    try {
      // Pre-prepare all column data
      byte[][] columnData = new byte[MATRIX_WIDTH][];
      int patternSize = gradientPattern.size();

      for (int col = 0; col < MATRIX_WIDTH; col++) {
        columnData[col] = new byte[MATRIX_HEIGHT + 1];
        columnData[col][0] = (byte) col; // The first byte is the column index

        for (int row = 0; row < MATRIX_HEIGHT; row++) {
          int spatialOffset = col + (invertAngle ? -row : row);
          int patternIdx = Math.floorMod(startPosition + (reverseDirection ? -spatialOffset : spatialOffset), patternSize);
          columnData[col][row + 1] = gradientPattern.get(patternIdx).byteValue();
        }
      }

      // Send columns with verification
      for (int col = 0; col < MATRIX_WIDTH; col++) {
        device.sendCommand(CommunicationCreator.CommandVals.STAGE_GREY_COL, columnData[col]);

        Thread.sleep(INTER_COLUMN_DELAY_MS);
      }

      // Draw buffer with verification
      device.sendCommand(CommunicationCreator.CommandVals.DRAW_GREY_COL_BUFFER, new byte[0]);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      log.error("Error updating display", e);
    }
  }

}