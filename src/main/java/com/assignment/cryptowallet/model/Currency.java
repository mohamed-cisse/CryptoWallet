package com.assignment.cryptowallet.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String symbol;
    private BigDecimal LatestPrice;
    private LocalDateTime updateTime;

    public Currency(String name, String symbol, BigDecimal latestPrice, LocalDateTime updateTime) {
        this.name = name;
        this.symbol = symbol;
        LatestPrice = latestPrice;
        this.updateTime = updateTime;
    }

    public Currency(String name, String symbol, BigDecimal latestPrice) {
        this.name = name;
        this.symbol = symbol;
        LatestPrice = latestPrice;
    }

    public Currency() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getLatestPrice() {
        return LatestPrice;
    }

    public void setLatestPrice(BigDecimal latestPrice) {
        LatestPrice = latestPrice;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}