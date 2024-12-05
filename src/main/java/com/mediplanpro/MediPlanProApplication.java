package com.mediplanpro;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class MediPlanProApplication {

    public static void main(String[] args) {

        SpringApplication.run(MediPlanProApplication.class, args);
    }

}
