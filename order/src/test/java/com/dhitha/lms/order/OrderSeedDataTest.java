package com.dhitha.lms.order;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class OrderSeedDataTest {

  @Test
  void seedDataIncludesReturnedOrderHistory() throws IOException, URISyntaxException {
    String dataSql =
        Files.readString(
            Paths.get(
                OrderSeedDataTest.class.getClassLoader().getResource("data.sql").toURI()));

    assertTrue(
        dataSql.contains("insert into book_order_history"),
        "Admin order history needs at least one seeded returned-order record");
  }
}
