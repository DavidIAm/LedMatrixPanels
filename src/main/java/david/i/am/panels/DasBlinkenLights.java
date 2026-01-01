package david.i.am.panels;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Random;
import java.util.stream.IntStream;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DasBlinkenLights {
  private CommunicationCreator left;
  private CommunicationCreator right;
  private Random random;

  @PostConstruct
  void init() {
    random = new Random();
    left = new CommunicationCreator("/dev/ttyACM0", 115200);
//    left.setDisplayOn();
    left.setBrightness(0x20);
    right = new CommunicationCreator("/dev/ttyACM1", 115200);
//    right.setDisplayOn();
    right.setBrightness(0x20);
  }

  @PreDestroy
  void destroy() {
    left.close();
    right.close();
  }

  @Scheduled(initialDelay = 125, fixedRate = 250)
  void showLeft() {
    left.sendDraw(thirtyNineRandomBytes());
  }
  @Scheduled(fixedRate = 250)
  void showRight() {
    right.sendDraw(thirtyNineRandomBytes());
  }
  
  byte[] thirtyNineRandomBytes() {
    byte[] buffer = new byte[40];
    IntStream.range(0, 39)
        .forEach(i -> buffer[i] = (byte) random.nextInt(256));
    return buffer;
  }
}
