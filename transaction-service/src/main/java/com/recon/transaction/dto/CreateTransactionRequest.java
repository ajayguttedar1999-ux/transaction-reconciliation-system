package com.recon.transaction.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateTransactionRequest(
        @NotBlank(message = "Transaction ID is mandatory")
        String transactionId,

        @NotBlank(message = "Product ID is mandatory")
        String productId,

        @NotBlank(message = "Account ID is mandatory")
        String accountId,

        @NotNull(message = "Quantity cannot be null")
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotNull(message = "Price cannot be null")
        @DecimalMin(value = "0.0001", message = "Price must be greater than zero")
        BigDecimal price,

        @NotBlank(message = "Currency is mandatory")
        @Size(min = 3, max = 3, message = "Currency must be 3-character ISO code")
        String currency,

        @NotNull(message = "Trade Date is mandatory")
        Instant tradeDate
) {}
