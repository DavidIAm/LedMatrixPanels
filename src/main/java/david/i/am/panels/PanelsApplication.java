package david.i.am.panels;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PanelsApplication {

  public static void main(String[] args) {
    SpringApplication.run(PanelsApplication.class, args);
  }
}

