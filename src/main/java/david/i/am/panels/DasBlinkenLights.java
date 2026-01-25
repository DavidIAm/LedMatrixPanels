package david.i.am.panels;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Random;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DasBlinkenLights implements PanelService {
  private Random random;

  private final ProfileState profileState;

  public DasBlinkenLights(ProfileState profileState) {
    this.profileState = profileState;
  }

  @PostConstruct
  void init() {
    random = new Random();
  }

  @PreDestroy
  void destroy() {
    // nothing needed here
  }

  @Override
  public String getProfileName() {
    return "dasblinkenlights";
  }

  @Override
  public void showLeft(CommunicationCreator left) {
    if (!isActive(profileState)) {
      return;
    }
    log.trace("DasBlinkenLights: showLeft");
    left.sendDraw(thirtyNineRandomBytes());
  }

  @Override
  public void showRight(CommunicationCreator right) {
    if (!isActive(profileState)) {
      return;
    }
    log.trace("DasBlinkenLights: showRight");
    right.sendDraw(thirtyNineRandomBytes());
  }
  
  byte[] thirtyNineRandomBytes() {
    byte[] buffer = new byte[40];
    IntStream.range(0, 39)
        .forEach(i -> buffer[i] = (byte) random.nextInt(256));
    return buffer;
  }
}
