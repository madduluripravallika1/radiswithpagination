package com.example.service;

import com.example.ksql.DatabaseConnectionManager;
import com.example.model.Customer;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.confluent.ksql.api.client.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CustomerService {

    private static final String REDIS_CACHE_KEY_PREFIX = "customers";
    private static final String TOPIC = "customers100";

    @Autowired
    private ObjectMapper objectMapper; // Autowire ObjectMapper

    @Autowired
    private DatabaseConnectionManager databaseConnectionManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate; // Change Object to String for Redis

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Method to send customers to Kafka
    public void sendCustomers() {
        for (int i = 1; i <= 10; i++) {
            Customer customer = new Customer();
            customer.setId(i);
            customer.setName("Customer" + i);
            customer.setLocaldatetime(ZonedDateTime.now(ZoneOffset.UTC)); // Adjust time for uniqueness

            kafkaTemplate.send(TOPIC, String.valueOf(customer.getId()), customer);
            cacheCustomerInRedis(customer);

            System.out.println("Sent message: " + customer);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void schedulePublishCustomers() {
        System.out.println("Starting to publish customers...");

        List<Customer> customers = getAll();
        System.out.println("Number of customers fetched: " + customers.size());

        for (Customer customer : customers) {
            if (customer != null) {
                cacheCustomerInRedis(customer);
                System.out.println("Sent customer to Kafka: " + customer);
            } else {
                System.err.println("Null customer or customer ID, skipping cache.");
            }
        }

        System.out.println("All customers published to Kafka and cached in Redis.");
    }

    public List<Customer> sendToRedis() {
        List<Customer> customers = getAll();

        System.out.println("Customers: " + customers);
        System.out.println("Number of customers: " + customers.size());

        for (Customer customer : customers) {
            if (customer != null) {
                cacheCustomerInRedis(customer);
            } else {
                System.err.println("Null customer or customer ID, skipping cache.");
            }
        }

        return customers;
    }

    private void cacheCustomerInRedis(Customer customer) {
        try {
            String key = REDIS_CACHE_KEY_PREFIX + ":" + customer.getId();
            // Serialize the Customer object to a JSON string
            String customerJson = objectMapper.writeValueAsString(customer);
            redisTemplate.opsForValue().set(key, customerJson);
            System.out.println("Cached customer in Redis: " + customer);
        } catch (Exception e) {
            System.err.println("Failed to cache customer in Redis: " + e.getMessage());
        }
    }

    public Customer getCustomerFromRedis(int customerId) {
        String key = REDIS_CACHE_KEY_PREFIX + ":" + customerId;
        String customerJson = (String) redisTemplate.opsForValue().get(key);

        if (customerJson != null) {
            try {
                // Deserialize the JSON string back to a Customer object
                return objectMapper.readValue(customerJson, Customer.class);
            } catch (JsonProcessingException e) {
                System.err.println("Failed to deserialize customer from Redis: " + e.getMessage());
            }
        }
        return null; // Return null if not found
    }

    public List<Customer> getAll() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM CUSTOMERS_TABLE EMIT CHANGES LIMIT 10;";

        try {
            List<Row> rows = databaseConnectionManager.getData(sql);
            System.out.println("Rows retrieved: " + rows.size());

            if (rows.isEmpty()) {
                System.err.println("No data retrieved from KSQL.");
                return customers;
            }

            for (Row row : rows) {
                try {
                    Customer customer = new Customer();
                    customer.setId(Integer.parseInt(row.getString("id")));
                    customer.setName(row.getString("name"));
                    customer.setLocaldatetime(ZonedDateTime.parse(row.getString("localdatetime")));
                    customers.add(customer);
                    System.out.println("Customer added: " + customer);
                } catch (NumberFormatException e) {
                    // Handle the exception if necessary
                }
            }
        } catch (Exception e) {
            // Handle the exception if necessary
        }
        return customers;
    }
    public List<Customer> displayAllCustomers() {
        List<Customer> allCustomers = getAllCustomers();
        System.out.println("Retrieved " + allCustomers.size() + " customers from Redis.");

        for (Customer customer : allCustomers) {
            System.out.println("Customer ID: " + customer.getId() + ", Name: " + customer.getName()  + ", localdatetime: " + customer.getLocaldatetime());
        }
        return allCustomers;
    }

    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();

        for (int i = 1; i <= 10; i++) { // Assuming you have up to 100 customers
            String key = REDIS_CACHE_KEY_PREFIX + ":" + i; // Use the correct prefix for keys
            String customerJson = (String) redisTemplate.opsForValue().get(key); // Get as String

            if (customerJson != null) {
                try {
                    // Deserialize the object back to Customer
                    Customer customer = objectMapper.readValue(customerJson, Customer.class);
                    customers.add(customer);
                } catch (Exception e) {
                    // Log the exception if needed
                    System.err.println("Failed to convert customer object: " + e.getMessage());
                }
            }
        }

        return customers; // Return the list of customers
    }

    public Customer getCustomerById(int customerId) throws JsonProcessingException {
        String key = REDIS_CACHE_KEY_PREFIX + ":" + customerId; // Build the Redis key for the customer
        String customerJson = (String) redisTemplate.opsForValue().get(key); // Get the customer JSON string

        if (customerJson != null) {
            // Deserialize the JSON string back to a Customer object
            return objectMapper.readValue(customerJson, Customer.class);
        }
        return null; // Return null if not found
    }

    public Page<Customer> findCustomersWithPagination(int offset, int pageSize) {
        List<Customer> allCustomers = displayAllCustomers(); // Get all customers from your data source
        int totalCustomers = allCustomers.size();

        // Calculate start and end indices
        int start = Math.min(offset * pageSize, totalCustomers);
        int end = Math.min(start + pageSize, totalCustomers);

        List<Customer> customersPage = allCustomers.subList(start, end); // Get the sublist for pagination
        return new PageImpl<>(customersPage, PageRequest.of(offset, pageSize), totalCustomers);
    }
}
