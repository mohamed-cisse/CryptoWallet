package com.assignment.cryptowallet.dto;

import java.math.BigDecimal;

public record AssetDto(String symbol,
                       BigDecimal quantity,
                       BigDecimal price) {
}
