package shopeasy_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class ShopEasyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopEasyApplication.class, args);
    }

    @GetMapping("/")
    public String home() {
        return "Welcome to ShopEasy Application! " +
               "DevSecOps Pipeline Running Successfully!";
    }

    @GetMapping("/health")
    public String health() {
        return "ShopEasy App is Healthy! Status: UP";
    }

    @GetMapping("/version")
    public String version() {
        return "ShopEasy App Version: 1.0.0";
    }
}