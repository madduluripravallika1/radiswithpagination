    package com.example.serdes;

    import com.example.model.Customer;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
    import com.fasterxml.jackson.core.JsonProcessingException;
    import java.io.IOException;
    import org.apache.kafka.common.header.Headers;
    import org.apache.kafka.common.serialization.Deserializer;
    import org.apache.kafka.common.serialization.Serdes;
    import org.apache.kafka.common.serialization.Serializer;

    public class CustomerSerde extends Serdes.WrapperSerde<Customer> {

        private static final ObjectMapper objectMapper = new ObjectMapper();

        static {
            // Register the JavaTimeModule for handling Java 8 date/time types
            objectMapper.registerModule(new JavaTimeModule());
        }

        public CustomerSerde() {
            super(new CustomerSerializer(), new CustomerDeserializer());
        }

        public static class CustomerSerializer implements Serializer<Customer> {
            @Override
            public byte[] serialize(String topic, Customer customer) {
                if (customer == null) {
                    return null; // Return null if customer is null
                }
                try {
                    return objectMapper.writeValueAsBytes(customer);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Error serializing customer", e);
                }
            }
        }

        public static class CustomerDeserializer implements Deserializer<Customer> {
            @Override
            public Customer deserialize(String topic, byte[] data) {
                if (data == null) {
                    return null; // Return null if data is null
                }
                try {
                    return objectMapper.readValue(data, Customer.class);
                } catch (IOException e) {
                    throw new RuntimeException("Error deserializing customer", e);
                }
            }

            @Override
            public Customer deserialize(String topic, Headers headers, byte[] data) {
                return deserialize(topic, data); // Delegate to the primary deserialize method
            }
        }
    }
