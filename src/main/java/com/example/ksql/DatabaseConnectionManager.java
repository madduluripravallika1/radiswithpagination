package com.example.ksql;

import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.ClientOptions;
import io.confluent.ksql.api.client.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class DatabaseConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);
    private final Client ksqlClient;

    @Autowired
    private ClientOptions ksqlClientOptions;

    public DatabaseConnectionManager() {
        ClientOptions options = ClientOptions.create()
                .setHost("localhost")  // Replace with your KSQLDB host
                .setPort(8088);        // Replace with your KSQLDB port
        this.ksqlClient = Client.create(options);
    }

    public List<Row> getData(String sql) throws ExecutionException, InterruptedException {
        logger.info("Initiating the data retrieval");
        List<Row> rows = new ArrayList<>();
        try {
            logger.info("SQL: " + sql);
            CompletableFuture<List<Row>> future = ksqlClient.executeQuery(sql);
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Thread was interrupted before getting KSQL results.");
            }

            // Set a timeout of 10 seconds
            List<Row> resultRows = future.get(10, TimeUnit.SECONDS);

            logger.info("Received results. Num rows: " + resultRows.size());

            for (Row row : resultRows) {
                rows.add(row);
                logger.info("Row: " + row.values());
            }
        } catch (InterruptedException e) {
            logger.error("Query was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException | TimeoutException e) {
            logger.error("Error executing KSQL query: " + e.getMessage(), e);
            throw new RuntimeException("Error executing KSQL query", e);
        } finally {
        }
        return rows;
    }
}
