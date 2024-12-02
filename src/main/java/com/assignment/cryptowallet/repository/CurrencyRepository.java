package com.assignment.cryptowallet.repository;

import com.assignment.cryptowallet.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    Optional<Currency> findBySymbol(String symbol);
    Optional<List<Currency>> findBySymbolIn(List<String> symbols);
    Optional<Currency> getCurrenciesBySymbol(String symbol);
}

