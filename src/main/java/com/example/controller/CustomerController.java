package com.example.controller;

import com.example.dto.APIResponse;
import com.example.model.Customer;
import com.example.service.CustomerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@CacheConfig(cacheNames = "customerCache")
public class CustomerController {
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }


    @PostMapping("/sendtokafka")
    public ResponseEntity<String> initializeCache() {
        customerService.sendCustomers();
        return ResponseEntity.ok("Customers sent to Kafka and cached.");
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publishCustomerStatements() {
        customerService.schedulePublishCustomers();
        return ResponseEntity.ok("Customer statements published successfully.");
    }

    @GetMapping("/publishCache")
    public ResponseEntity<String> cacheCustomerInRedis() {
        long startTime = System.currentTimeMillis();
        customerService.sendToRedis(); // Adjust this based on your logic
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime; // Calculate elapsed time
        logger.info("Data sent to Redis in {} ms", elapsedTime);
        return ResponseEntity.ok("Redis sent the data.");
    }

    @GetMapping("/all")
    public ResponseEntity<String> getAllCustomers() {
        List<Customer> customers = customerService.displayAllCustomers();
        return ResponseEntity.ok("Fetched the data from Redis");
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<String> getCustomerById(@PathVariable int customerId) {
        try {
            Customer customer = customerService.getCustomerById(customerId); // Fetch the customer by ID
            if (customer != null) {
                return ResponseEntity.ok("Fetched customer data: " + customer); // Return the customer data
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Customer not found");
            }
        } catch (JsonProcessingException e) {
            System.err.println("Failed to deserialize customer from Redis: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing customer data");
        }
    }
    @GetMapping("/pagination/{offset}/{pageSize}")
    public ResponseEntity<APIResponse<Page<Customer>>> getCustomersWithPagination(
            @PathVariable int offset, @PathVariable int pageSize) {

        Page<Customer> customersWithPagination = customerService.findCustomersWithPagination(offset, pageSize);

        // Print customers to the console
        customersWithPagination.getContent().forEach(customer -> {
            logger.info("ID: {}, Name: {}, LocalDateTime: {}", customer.getId(), customer.getName(), customer.getLocaldatetime());
        });

        return ResponseEntity.ok(new APIResponse<>((int) customersWithPagination.getTotalElements(), customersWithPagination));
    }


}