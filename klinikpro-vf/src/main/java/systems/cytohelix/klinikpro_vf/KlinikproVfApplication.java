package systems.cytohelix.klinikpro_vf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KlinikproVfApplication {

	public static void main(String[] args) {
		SpringApplication.run(KlinikproVfApplication.class, args);
	}

}
