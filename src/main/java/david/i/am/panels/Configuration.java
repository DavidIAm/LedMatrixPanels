package david.i.am.panels;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class Configuration {
  @Bean("left")
  public CommunicationCreator left() {
    return new CommunicationCreator("/dev/ttyACM1", 115200);
  }
  @Bean("right")
  public CommunicationCreator right() {
    return new CommunicationCreator("/dev/ttyACM0", 115200);
  }
}
