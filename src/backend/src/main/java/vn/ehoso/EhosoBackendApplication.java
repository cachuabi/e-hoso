package vn.ehoso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point of the e-HoSo backend. */
@SpringBootApplication
@EnableScheduling
public class EhosoBackendApplication {

  /** Starts the application. */
  public static void main(String[] args) {
    SpringApplication.run(EhosoBackendApplication.class, args);
  }
}
