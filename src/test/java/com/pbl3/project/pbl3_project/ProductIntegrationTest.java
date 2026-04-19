package com.pbl3.project.pbl3_project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.repository.CategoryRepository;
import com.pbl3.project.pbl3_project.repository.InventoryPositionBaselineRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryPositionBaselineRepository inventoryPositionBaselineRepository;

    @BeforeEach
    void setup() {
        inventoryPositionBaselineRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testCreateProductApiIsDesktopOnlyAndGetProductsStillWorks() throws Exception {
        Category category = new Category();
        category.setName("Test Category");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Description");
        product.setPrice(new BigDecimal("100.00"));
        product.setQuantity(50);
        product.setCategory(category);

        productRepository.save(product);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Product"));
    }
}
