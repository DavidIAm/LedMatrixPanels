package david.i.am.panels;

import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PanelsApplication {

  @Getter
  private static ConfigurableApplicationContext lastContext;

  public static void main(String[] args) {
    lastContext = SpringApplication.run(PanelsApplication.class, args);
  }

}

