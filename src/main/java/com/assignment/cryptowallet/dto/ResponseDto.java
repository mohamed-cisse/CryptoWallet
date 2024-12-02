package com.assignment.cryptowallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public record ResponseDto(
     BigDecimal totalValue,
     String bestAsset,
     BigDecimal bestPerformance,
     String worstAsset,
     BigDecimal worstPerformance,
     LocalDateTime lastUpdated
){}
