package com.cityfix;

import com.cityfix.dao.DatabaseConnection;
import com.cityfix.service.PrioritizerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class CityFixApplication {

    public static void main(String[] args) {
        // Initialize database schema from schema.sql
        initializeDatabase();

        SpringApplication.run(CityFixApplication.class, args);
        System.out.println("============================================================");
        System.out.println(" CityFix Municipal Infrastructure Prioritizer is running!  ");
        System.out.println(" UI Dashboard: http://localhost:8080/                      ");
        System.out.println(" Repair Tasks: http://localhost:8080/tasks.html            ");
        System.out.println(" Reports View: http://localhost:8080/reports.html          ");
        System.out.println(" Citizen Form: http://localhost:8080/submit.html           ");
        System.out.println("============================================================");
    }

    private static void initializeDatabase() {
        try (InputStream in = CityFixApplication.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in != null) {
                String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                DatabaseConnection.initializeSchema(sql);
            }
        } catch (Exception e) {
            System.err.println("Note: Automatic schema setup skipped or manual DB connected.");
        }
    }

    @Bean
    public PrioritizerService prioritizerService() {
        return new PrioritizerService();
    }
}

