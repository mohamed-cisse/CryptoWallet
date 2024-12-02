package com.assignment.cryptowallet.service;

import com.assignment.cryptowallet.model.Currency;
import com.assignment.cryptowallet.repository.CurrencyRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class PriceUpdateScheduler {
    private static final Logger logger = LoggerFactory.getLogger(PriceUpdateScheduler.class);
    private final CurrencyRepository currencyRepository;
    private final CryptoService cryptoService;
    private final ThreadPoolExecutor taskExecutor;
    @Value("${currency.update.duration:60000}")
    private long updateDuration;
    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

    @PostConstruct
    public void startScheduledTask() {
        taskScheduler.initialize();
        taskScheduler.scheduleAtFixedRate(this::fetchAndUpdatePrices, updateDuration);
    }

    @Autowired
    public PriceUpdateScheduler(CurrencyRepository currencyRepository,
                                CryptoService cryptoService,
                                ThreadPoolExecutor taskExecutor) {

        this.currencyRepository = currencyRepository;
        this.cryptoService = cryptoService;
        this.taskExecutor = taskExecutor;
    }


    public void fetchAndUpdatePrices() {
        try {
            if (currencyRepository.count() == 0) {
                logger.info("No currencies found");
                return;
            }
            List<Currency> currencies = currencyRepository.findAll();
            logger.info("-----------------------------------------------------------------------------------------");
            int batchSize = 3;
            for (int i = 0; i < currencies.size(); i += batchSize) {
                int end = Math.min(i + batchSize, currencies.size());
                List<Currency> batch = currencies.subList(i, end);

                List<CompletableFuture<Void>> futures = batch.stream()
                        .map(currency -> CompletableFuture.runAsync(() -> {
                            try {
                                logger.info("Submitted request {} at {}", currency.getSymbol(), LocalDateTime.now());
                                BigDecimal price = cryptoService.fetchPrice(currency.getName()).join();
                                currency.setLatestPrice(price);
                                currency.setUpdateTime(LocalDateTime.now());
                                currencyRepository.save(currency);
                                logger.debug("Updated price for {} at {}", currency.getSymbol(), LocalDateTime.now());
                            } catch (Exception e) {
                                logger.error("Failed to update price for {}: {}", currency.getSymbol(), e.getMessage());
                            }
                        }, taskExecutor))
                        .toList();

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        } catch (Exception e) {
            logger.error("Failed to update price");
        }
    }

}

