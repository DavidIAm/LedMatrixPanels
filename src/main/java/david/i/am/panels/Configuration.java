package david.i.am.panels;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class Configuration {
  @Bean("left")
  public CommunicationCreator left() {
    try {
      return new CommunicationCreator("/dev/ttyACM1", 115200);
    } catch (Exception e) {
      return new CommunicationCreator(null, 0);
    }
  }
  @Bean("right")
  public CommunicationCreator right() {
    try {
      return new CommunicationCreator("/dev/ttyACM0", 115200);
    } catch (Exception e) {
      return new CommunicationCreator(null, 0);
    }
  }
}
