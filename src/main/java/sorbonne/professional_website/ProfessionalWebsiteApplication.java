package sorbonne.professional_website;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import sorbonne.professional_website.upload.CloudinaryStorageProperties;
import sorbonne.professional_website.upload.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, CloudinaryStorageProperties.class})
public class ProfessionalWebsiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProfessionalWebsiteApplication.class, args);
	}
}