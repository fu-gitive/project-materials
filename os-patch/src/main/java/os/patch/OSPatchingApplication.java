package os.patch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class OSPatchingApplication {
    public static void main(String[] args) {
        SpringApplication.run(OSPatchingApplication.class, args);
    }
}
