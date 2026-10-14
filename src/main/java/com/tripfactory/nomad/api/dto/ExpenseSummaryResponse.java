package com.tripfactory.nomad.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExpenseSummaryResponse {

    private BigDecimal total;
    private List<Item> expenses;
    private List<Balance> balances;
    private List<Settlement> settlements;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {
        private Long id;
        private Long paidById;
        private String paidByName;
        private String description;
        private BigDecimal amount;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Balance {
        private Long userId;
        private String name;
        /** Positive: is owed. Negative: owes. */
        private BigDecimal net;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Settlement {
        private Long fromUserId;
        private String fromName;
        private Long toUserId;
        private String toName;
        private BigDecimal amount;
    }
}
