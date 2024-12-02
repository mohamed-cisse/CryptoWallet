package com.assignment.cryptowallet.service;

import com.assignment.cryptowallet.dto.AssetDto;
import com.assignment.cryptowallet.dto.ResponseDto;
import com.assignment.cryptowallet.dto.WalletDto;
import com.assignment.cryptowallet.mapper.WalletMapper;
import com.assignment.cryptowallet.model.Asset;
import com.assignment.cryptowallet.model.Currency;
import com.assignment.cryptowallet.model.Wallet;
import com.assignment.cryptowallet.repository.AssetRepository;
import com.assignment.cryptowallet.repository.CurrencyRepository;
import com.assignment.cryptowallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class CryptoService {
    private static final Logger logger = LoggerFactory.getLogger(CryptoService.class);
    private final RestTemplate restTemplate;
    private final WalletRepository walletRepository;
    private final AssetRepository assetRepository;
    private final CurrencyRepository currencyRepository;
    private final WalletMapper walletMapper;
    @Value("${coincap.api.url}")
    private String apiUrl;
    @Value("${currency.history.start}")
    private int historyStart;

    @Autowired
    public CryptoService(RestTemplate restTemplate,
                         WalletRepository walletRepository,
                         AssetRepository assetRepository,
                         CurrencyRepository currencyRepository,
                         WalletMapper walletMapper) {
        this.restTemplate = restTemplate;
        this.walletRepository = walletRepository;
        this.assetRepository = assetRepository;
        this.currencyRepository = currencyRepository;
        this.walletMapper = walletMapper;
    }

    @Transactional
    public ResponseDto registerWallet(WalletDto walletDto) {
        if (walletDto == null) {
            throw new IllegalArgumentException("wallet is null");
        }
        Wallet wallet = walletMapper.toEntity(walletDto);
        registerCurrency(walletDto.assets());
        ResponseDto responseDto = calculateResponseStatistics(wallet);
        wallet.getAssets().forEach(asset -> asset.setWallet(wallet));
        saveAssets(wallet.getAssets());
        walletRepository.save(wallet);
        return responseDto;

    }

    private void saveAssets(List<Asset> asset) {
        assetRepository.saveAll(asset);
    }

    @Async
    public CompletableFuture<BigDecimal> fetchPrice(String name) {
        String url = String.format("%s/assets/%s", apiUrl, name.toLowerCase());
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return CompletableFuture.completedFuture(new BigDecimal((String) data.get("priceUsd")));
    }

    private void registerCurrency(List<AssetDto> assets) {
        logger.info("Starting asset registration for {} currencies", assets.size());

        List<Currency> currencies = assets.stream()
                .map(this::getCurrencyPrice)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        try {
            if (!currencies.isEmpty()) {
                currencyRepository.saveAll(currencies);
                logger.info("Successfully saved {} currencies to the database", currencies.size());
            } else {
                logger.info("asset already exists");
            }
        } catch (Exception e) {
            logger.error("Error saving currencies to database", e);
        }
    }

    private Currency getCurrencyPrice(AssetDto assetDto) {
        logger.debug("Getting price for asset: {}", assetDto.symbol());
        try {
            if (!isCurrencyExist(assetDto.symbol())) {
                String currencyName = getCurrencyName(assetDto.symbol());
                String latestPrice = getCurrencyHistoricalPrice(currencyName);

                if (latestPrice != null) {
                    logger.debug("Price set for {}: {}", assetDto.symbol(), latestPrice);
                    return new Currency(currencyName, assetDto.symbol(), new BigDecimal(latestPrice));
                }
                logger.warn("No historical price found for asset: {}", assetDto.symbol());

            }
        } catch (Exception e) {
            logger.error("Error getting price for asset: {}", assetDto.symbol(), e);
        }
        return null;
    }

    private boolean isCurrencyExist(String symbol) {
        return currencyRepository.getCurrenciesBySymbol(symbol).isPresent();
    }

    private String getCurrencyName(String symbol) {
        String searchUrl = "https://api.coincap.io/v2/assets?search=" + symbol + "&limit=1";
        logger.debug("Requesting currency name for symbol: {}", symbol);

        try {
            ResponseEntity<String> searchResponse = restTemplate.getForEntity(searchUrl, String.class);
            if (searchResponse.getStatusCode().is2xxSuccessful() && searchResponse.getBody() != null) {
                return extractCurrencyName(searchResponse.getBody());
            }
            logger.error("Failed to retrieve currency name for symbol: {}. Response code: {}", symbol, searchResponse.getStatusCode());

        } catch (Exception e) {
            logger.error("Error retrieving currency name for symbol: {}", symbol, e);
        }
        return null;
    }

    private String extractCurrencyName(String searchResponseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(searchResponseBody);
            JsonNode data = root.path("data");
            if (data.isArray() && data.size() > 0) {
                return data.get(0).path("name").asText();
            }
            logger.warn("No data found in search response");
        } catch (Exception e) {
            logger.error("Error extracting currency name from response", e);
        }
        return null;
    }

    private String getCurrencyHistoricalPrice(String currencyName) {
        if (currencyName != null) {
            validateHistoryStart();
            Instant now = Instant.now();
            Instant start = now.minus(historyStart, ChronoUnit.MILLIS);
            Instant end = start.plus(1, ChronoUnit.MINUTES);

            long startTimestamp = start.toEpochMilli();
            long endTimestamp = end.toEpochMilli();

            String historyUrl = "https://api.coincap.io/v2/assets/"
                    + currencyName.toLowerCase()
                    + "/history?interval=m1&start=" + startTimestamp
                    + "&end=" + endTimestamp;

            logger.debug("Requesting historical price for currency: {}", currencyName);
            try {
                ResponseEntity<String> historyResponse = restTemplate.getForEntity(historyUrl, String.class);
                if (historyResponse.getStatusCode().is2xxSuccessful() && historyResponse.getBody() != null) {
                    return processHistoricalData(historyResponse.getBody());
                }
                logger.error("Failed to retrieve historical price for currency: {}. Response code: {}", currencyName, historyResponse.getStatusCode());
            } catch (Exception e) {
                logger.error("Error retrieving historical price for currency: {}", currencyName, e);
            }
        } else {
            logger.warn("No currency name found");
        }
        return null;
    }

    private void validateHistoryStart() {
        if (historyStart < 60000) {
            historyStart = 60000;
        }
    }

    private String processHistoricalData(String historyResponseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(historyResponseBody);
            JsonNode data = root.path("data");
            if (data.isArray() && data.size() > 0) {
                String price = data.get(0).path("priceUsd").asText();
                return price;
            }
            logger.warn("No historical data found in the response");
        } catch (Exception e) {
            logger.error("Error processing historical data", e);
        }
        return null;
    }

    private ResponseDto calculateResponseStatistics(Wallet wallet) {
        List<Asset> assets = wallet.getAssets();

        if (assets.isEmpty()) {
            throw new NoSuchElementException("No assets available in the wallet");
        }

        BigDecimal totalValue = calculateTotalValue(assets);

        Asset bestAsset = assets.stream()
                .max(Comparator.comparing(this::calculatePerformance))
                .orElseThrow();

        Asset worstAsset = assets.stream()
                .min(Comparator.comparing(this::calculatePerformance))
                .orElseThrow();

        BigDecimal bestPerformance = calculatePerformance(bestAsset);
        BigDecimal worstPerformance = calculatePerformance(worstAsset);
        LocalDateTime lastUpdated = LocalDateTime.now();
        return new ResponseDto(totalValue,
                bestAsset.getSymbol(),
                bestPerformance,
                worstAsset.getSymbol(),
                worstPerformance, lastUpdated);
    }

    private BigDecimal calculatePerformance(Asset asset) {
        Currency currency = currencyRepository.findBySymbol(asset.getSymbol()).orElseThrow();
        return currency.getLatestPrice()
                .subtract(asset.getPrice())
                .divide(asset.getPrice(), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateTotalValue(List<Asset> assets) {
        List<String> symbols = assets.stream()
                .map(Asset::getSymbol)
                .toList();

        List<Currency> currencies = currencyRepository.findBySymbolIn(symbols).orElseThrow();

        Map<String, BigDecimal> currencyPriceMap = currencies.stream()
                .collect(Collectors.toMap(Currency::getSymbol, Currency::getLatestPrice));

        return assets.stream()
                .map(asset -> {
                    BigDecimal currencyPrice = currencyPriceMap.get(asset.getSymbol());
                    return asset.getQuantity().multiply(currencyPrice);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
