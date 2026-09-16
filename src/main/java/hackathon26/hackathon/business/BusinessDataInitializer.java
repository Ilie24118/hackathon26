package hackathon26.hackathon.business;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class BusinessDataInitializer implements CommandLineRunner {
    private final BusinessService service;
    BusinessDataInitializer(BusinessService service) { this.service = service; }
    @Override public void run(String... args) { service.importBundledSnapshot(); }
}
