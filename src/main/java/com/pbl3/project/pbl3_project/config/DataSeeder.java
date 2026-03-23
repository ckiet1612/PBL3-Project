package com.pbl3.project.pbl3_project.config;

import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword("admin"); // Plain text for now
                admin.setFullName("System Administrator");
                admin.setRole(Role.ADMIN);
                
                userRepository.save(admin);
                System.out.println("Default admin user created: admin/admin");
            }
        };
    }

    @Bean
    CommandLineRunner initProducts(com.pbl3.project.pbl3_project.repository.CategoryRepository categoryRepository,
                                  com.pbl3.project.pbl3_project.repository.ProductRepository productRepository) {
        return args -> {
            if (categoryRepository.count() == 0) {
                com.pbl3.project.pbl3_project.entity.Category electronics = new com.pbl3.project.pbl3_project.entity.Category();
                electronics.setName("Electronics");
                categoryRepository.save(electronics);

                com.pbl3.project.pbl3_project.entity.Product laptop = new com.pbl3.project.pbl3_project.entity.Product();
                laptop.setName("MacBook Pro");
                laptop.setDescription("Apple M3 Chip");
                laptop.setPrice(1999.99);
                laptop.setQuantity(10);
                laptop.setCategory(electronics);
                productRepository.save(laptop);
                
                System.out.println("Sample product created: MacBook Pro");
            }
        };
    }

    @Bean
    CommandLineRunner fixMinStockLevel(com.pbl3.project.pbl3_project.repository.ProductRepository productRepository) {
        return args -> {
            java.util.List<com.pbl3.project.pbl3_project.entity.Product> products = productRepository.findAll();
            int fixed = 0;
            for (com.pbl3.project.pbl3_project.entity.Product p : products) {
                if (p.getMinStockLevel() == null || p.getMinStockLevel() == 0) {
                    p.setMinStockLevel(10);
                    productRepository.save(p);
                    fixed++;
                }
            }
            if (fixed > 0) {
                System.out.println("Fixed min_stock_level for " + fixed + " products (set to 10).");
            }
        };
    }
}
