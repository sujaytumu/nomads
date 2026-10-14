package com.tripfactory.nomad.service.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure equal-split balance maths (no Spring/JPA) so it can be unit tested in isolation.
 * All amounts are in minor units (paise).
 */
public final class ExpenseBalanceCalculator {

    private ExpenseBalanceCalculator() {
    }

    public record Payment(long paidByUserId, long amountMinor) {
    }

    public record Transfer(long fromUserId, long toUserId, long amountMinor) {
    }

    /** net = paid - fair share. Positive: is owed money. Negative: owes money. */
    public static Map<Long, Long> netBalances(List<Long> memberIds, List<Payment> payments) {
        Map<Long, Long> net = new LinkedHashMap<>();
        for (Long id : memberIds) {
            net.put(id, 0L);
        }
        int n = memberIds.size();
        if (n == 0) {
            return net;
        }
        for (Payment p : payments) {
            net.merge(p.paidByUserId(), p.amountMinor(), Long::sum);
            long share = p.amountMinor() / n;
            long remainder = p.amountMinor() % n;
            for (int i = 0; i < n; i++) {
                long owed = share + (i < remainder ? 1 : 0);
                net.merge(memberIds.get(i), -owed, Long::sum);
            }
        }
        return net;
    }

    /** Greedy settle-up: fewest practical transfers that zero every balance. */
    public static List<Transfer> settle(Map<Long, Long> net) {
        List<long[]> debtors = new ArrayList<>();
        List<long[]> creditors = new ArrayList<>();
        net.forEach((id, v) -> {
            if (v < 0) debtors.add(new long[] {id, -v});
            else if (v > 0) creditors.add(new long[] {id, v});
        });
        debtors.sort(Comparator.<long[]>comparingLong(a -> a[1]).reversed());
        creditors.sort(Comparator.<long[]>comparingLong(a -> a[1]).reversed());

        List<Transfer> out = new ArrayList<>();
        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            long amt = Math.min(debtors.get(i)[1], creditors.get(j)[1]);
            out.add(new Transfer(debtors.get(i)[0], creditors.get(j)[0], amt));
            debtors.get(i)[1] -= amt;
            creditors.get(j)[1] -= amt;
            if (debtors.get(i)[1] == 0) i++;
            if (creditors.get(j)[1] == 0) j++;
        }
        return out;
    }
}
