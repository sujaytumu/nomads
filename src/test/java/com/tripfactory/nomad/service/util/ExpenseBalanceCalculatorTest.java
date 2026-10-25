package com.tripfactory.nomad.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.service.util.ExpenseBalanceCalculator.Payment;
import com.tripfactory.nomad.service.util.ExpenseBalanceCalculator.Transfer;

class ExpenseBalanceCalculatorTest {

    @Test
    void equalSplitAndSettle() {
        Map<Long, Long> net = ExpenseBalanceCalculator.netBalances(
                List.of(1L, 2L, 3L), List.of(new Payment(1L, 3000_00)));
        assertEquals(2000_00, net.get(1L));
        assertEquals(-1000_00, net.get(2L));
        assertEquals(-1000_00, net.get(3L));

        List<Transfer> t = ExpenseBalanceCalculator.settle(net);
        assertEquals(2, t.size());
        assertTrue(t.stream().allMatch(x -> x.toUserId() == 1L && x.amountMinor() == 1000_00));
    }

    @Test
    void remainderPaiseNeverLostAndBalancesSumToZero() {
        Map<Long, Long> net = ExpenseBalanceCalculator.netBalances(
                List.of(1L, 2L, 3L), List.of(new Payment(2L, 100), new Payment(3L, 1)));
        assertEquals(0L, net.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void noExpensesMeansNoTransfers() {
        assertTrue(ExpenseBalanceCalculator.settle(
                ExpenseBalanceCalculator.netBalances(List.of(1L, 2L), List.of())).isEmpty());
    }

    @Test
    void recordedPayBackShrinksWhatIsOwed() {
        Map<Long, Long> net = ExpenseBalanceCalculator.netBalances(
                List.of(1L, 2L, 3L), List.of(new Payment(1L, 3000_00)));
        // 2 pays 1 back in full; only 3 still owes.
        ExpenseBalanceCalculator.applyTransfers(net, List.of(new Transfer(2L, 1L, 1000_00)));
        assertEquals(1000_00, net.get(1L));
        assertEquals(0L, net.get(2L));
        assertEquals(-1000_00, net.get(3L));
        assertEquals(0L, net.values().stream().mapToLong(Long::longValue).sum());
        List<Transfer> left = ExpenseBalanceCalculator.settle(net);
        assertEquals(1, left.size());
        assertEquals(3L, left.get(0).fromUserId());
    }
}
