package com.mutation.mutation_ai_studio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

@SpringBootApplication
public class MutationAiStudioApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(MutationAiStudioApplication.class);
		boolean cliScanMode = isCliScanMode(args);

		if (cliScanMode) {
			application.setWebApplicationType(WebApplicationType.NONE);
			application.setDefaultProperties(Map.of(
					"spring.autoconfigure.exclude",
					"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
							+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
							+ "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
			));
		}

		ConfigurableApplicationContext context = application.run(args);

		if (cliScanMode) {
			int exitCode = SpringApplication.exit(context);
			System.exit(exitCode);
		}
	}

	private static boolean isCliScanMode(String[] args) {
		return args != null && args.length > 0 && "scan".equals(args[0]);
	}

}
