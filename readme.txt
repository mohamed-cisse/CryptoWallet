CryptoWallet
Overview

CryptoWallet is a Spring Boot-based application designed to manage cryptocurrency wallets. It allows users to register wallets with their crypto assets, fetch real-time price updates for those assets, calculate portfolio value, and identify the best and worst performing assets. The application leverages external APIs for live cryptocurrency data and updates asset prices at regular intervals.
Features

    Wallet Registration: Register a wallet with cryptocurrency assets (symbol, quantity, and price).
    Real-Time Price Updates: Fetch and update the latest prices for assets using the CoinCap API.
    Portfolio Statistics: Calculate the total wallet value, identify the best-performing asset, and the worst-performing asset.
    Scheduled Updates: Periodically update asset prices in batches using a scheduler.
    REST API: Provide an API for wallet registration, fetching wallet details, and real-time portfolio statistics.

Technology Stack

    Backend: Java 17+, Spring Boot 3
    Database: H2 (In-Memory SQL Database)
    Dependency Management: Maven
    REST Client: RestTemplate
    Scheduler: ThreadPoolTaskScheduler
    Testing: JUnit 5, Mockito

Project Structure

    src/main/java:
        controller/WalletController: Handles REST API requests.
        dto/: Data Transfer Objects (e.g., AssetDto, WalletDto, ResponseDto).
        mapper/: MapStruct-based mappers for converting between entities and DTOs.
        model/: Entity classes representing Wallets, Assets, and Currencies.
        repository/: JPA Repositories for database interactions.
        service/: Business logic, including price fetching and wallet updates.
        configuration/: Configuration classes (e.g., RestTemplate and Scheduler setup).
    src/main/resources/application.properties: Configuration for database, API, and scheduler settings.

Installation

    Clone the Repository

git clone <repository_url>
cd CryptoWallet

Build the Project Ensure Maven is installed and run:

mvn clean install

Run the Application

    mvn spring-boot:run

    Access H2 Console Visit http://localhost:8080/h2-console and use the following credentials:
        JDBC URL: jdbc:h2:mem:testdb
        Username: sa
        Password: password

REST API Endpoints

    Register a Wallet
        URL: /api/wallet/register
        Method: POST
        Request Body:

{
  "assets": [
    { "symbol": "BTC", "quantity": 0.1, "price": 30000 },
    { "symbol": "ETH", "quantity": 2, "price": 2000 }
  ]
}

Response:

    {
      "totalValue": 34000,
      "bestAsset": "BTC",
      "bestPerformance": 15.0,
      "worstAsset": "ETH",
      "worstPerformance": 10.0,
      "lastUpdated": "2024-11-01T10:00:00"
    }

Fetch Wallet Details

    URL: /api/wallet/{id}
    Method: GET
    Response:

        {
          "id": 1,
          "assets": [
            { "symbol": "BTC", "quantity": 0.1, "price": 35000 },
            { "symbol": "ETH", "quantity": 2, "price": 2200 }
          ],
          "totalValue": 39400,
          "bestAsset": "BTC",
          "bestPerformance": 14.3,
          "worstAsset": "ETH",
          "worstPerformance": 10.0
        }

    Update Prices
        Scheduled updates fetch the latest prices at intervals specified in application.properties (currency.update.duration).

Configuration

Modify application.properties for custom settings:

spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
coincap.api.url=https://api.coincap.io/v2
currency.update.duration=60000

Tests

    Unit Tests: Located in src/test/java. Use JUnit 5 and Mockito for testing service and controller layers.
    Run Tests:

    mvn test

Known Issues

    Ensure the external CoinCap API is reachable; network issues may cause failures in fetching live data.