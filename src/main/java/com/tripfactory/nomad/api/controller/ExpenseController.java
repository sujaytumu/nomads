package com.tripfactory.nomad.api.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.tripfactory.nomad.api.dto.ExpenseCreateRequest;
import com.tripfactory.nomad.api.dto.ExpenseSummaryResponse;
import com.tripfactory.nomad.api.dto.SettlementCreateRequest;
import com.tripfactory.nomad.domain.entity.GroupExpense;
import com.tripfactory.nomad.domain.entity.GroupSettlement;
import com.tripfactory.nomad.domain.entity.TripGroup;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.GroupExpenseRepository;
import com.tripfactory.nomad.repository.GroupSettlementRepository;
import com.tripfactory.nomad.repository.TripGroupRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;
import com.tripfactory.nomad.service.util.ExpenseBalanceCalculator;
import com.tripfactory.nomad.service.util.ExpenseBalanceCalculator.Payment;
import com.tripfactory.nomad.service.util.ExpenseBalanceCalculator.Transfer;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final GroupExpenseRepository expenseRepository;
    private final GroupSettlementRepository settlementRepository;
    private final TripGroupRepository tripGroupRepository;
    private final TripRequestRepository tripRequestRepository;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    public ResponseEntity<ExpenseSummaryResponse> addExpense(@PathVariable Long groupId,
            @Valid @RequestBody ExpenseCreateRequest request) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User payer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long minor = request.getAmount().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        if (minor <= 0) {
            throw new BadRequestException("Amount must be positive");
        }

        GroupExpense expense = new GroupExpense();
        expense.setGroup(group);
        expense.setPaidBy(payer);
        expense.setDescription(request.getDescription().trim());
        expense.setAmountMinor(minor);
        expenseRepository.save(expense);
        return new ResponseEntity<>(buildSummary(groupId), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    public ResponseEntity<ExpenseSummaryResponse> getSummary(@PathVariable Long groupId) {
        return ResponseEntity.ok(buildSummary(groupId));
    }

    /** "I paid you back": records a pay-back from the signed-in member to another member. */
    @PostMapping("/settlements")
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    @Transactional
    public ResponseEntity<ExpenseSummaryResponse> recordSettlement(@PathVariable Long groupId,
            @Valid @RequestBody SettlementCreateRequest request) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User to = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (me.getId().equals(to.getId())) {
            throw new BadRequestException("You can't pay yourself");
        }
        long minor = request.getAmount().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        if (minor <= 0) {
            throw new BadRequestException("Amount must be positive");
        }

        // Only what is really owed can be marked as paid: keeps the books honest.
        ExpenseSummaryResponse current = buildSummary(groupId);
        long owedByMe = 0;
        long owedToThem = 0;
        for (ExpenseSummaryResponse.Balance b : current.getBalances()) {
            long v = b.getNet().movePointRight(2).longValueExact();
            if (b.getUserId().equals(me.getId())) owedByMe = Math.max(0, -v);
            if (b.getUserId().equals(to.getId())) owedToThem = Math.max(0, v);
        }
        if (minor > Math.min(owedByMe, owedToThem)) {
            throw new BadRequestException("That is more than you owe this person");
        }

        GroupSettlement s = new GroupSettlement();
        s.setGroup(group);
        s.setFromUser(me);
        s.setToUser(to);
        s.setAmountMinor(minor);
        settlementRepository.save(s);
        return new ResponseEntity<>(buildSummary(groupId), HttpStatus.CREATED);
    }

    /** Undo a recorded pay-back. Either person involved can remove it (for example if it was marked by mistake). */
    @DeleteMapping("/settlements/{settlementId}")
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    @Transactional
    public ResponseEntity<ExpenseSummaryResponse> undoSettlement(@PathVariable Long groupId, @PathVariable Long settlementId) {
        GroupSettlement s = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!s.getGroup().getId().equals(groupId)) {
            throw new ResourceNotFoundException("Payment not found");
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!s.getFromUser().getId().equals(me.getId()) && !s.getToUser().getId().equals(me.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Only the two people involved can remove this");
        }
        settlementRepository.delete(s);
        return ResponseEntity.ok(buildSummary(groupId));
    }

    private ExpenseSummaryResponse buildSummary(Long groupId) {
        List<TripRequest> members = tripRequestRepository.findGroupMembers(groupId);
        List<Long> memberIds = new ArrayList<>();
        Map<Long, String> names = new HashMap<>();
        Map<Long, String> upis = new HashMap<>();
        for (TripRequest tr : members) {
            Long uid = tr.getUser().getId();
            if (!memberIds.contains(uid)) {
                memberIds.add(uid);
                names.put(uid, tr.getUser().getName());
                upis.put(uid, tr.getUser().getUpiId());
            }
        }

        List<GroupExpense> expenses = expenseRepository.findByGroupIdWithPayer(groupId);
        List<Payment> payments = new ArrayList<>();
        List<ExpenseSummaryResponse.Item> items = new ArrayList<>();
        long total = 0;
        for (GroupExpense e : expenses) {
            Long payerId = e.getPaidBy().getId();
            // A payer who has since left the group still counts toward balances.
            if (!memberIds.contains(payerId)) {
                memberIds.add(payerId);
                names.put(payerId, e.getPaidBy().getName());
                upis.put(payerId, e.getPaidBy().getUpiId());
            }
            payments.add(new Payment(payerId, e.getAmountMinor()));
            total += e.getAmountMinor();

            ExpenseSummaryResponse.Item item = new ExpenseSummaryResponse.Item();
            item.setId(e.getId());
            item.setPaidById(payerId);
            item.setPaidByName(e.getPaidBy().getName());
            item.setDescription(e.getDescription());
            item.setAmount(rupees(e.getAmountMinor()));
            item.setCreatedAt(e.getCreatedAt());
            items.add(item);
        }

        Map<Long, Long> net = ExpenseBalanceCalculator.netBalances(memberIds, payments);

        // Pay-backs members have recorded reduce what is still owed.
        List<Transfer> paidBack = new ArrayList<>();
        List<ExpenseSummaryResponse.PaidBack> paidItems = new ArrayList<>();
        for (GroupSettlement gs : settlementRepository.findByGroupIdWithUsers(groupId)) {
            Long from = gs.getFromUser().getId();
            Long to = gs.getToUser().getId();
            for (User u : new User[] { gs.getFromUser(), gs.getToUser() }) {
                if (!net.containsKey(u.getId())) {
                    net.put(u.getId(), 0L);
                    names.put(u.getId(), u.getName());
                    upis.put(u.getId(), u.getUpiId());
                }
            }
            paidBack.add(new Transfer(from, to, gs.getAmountMinor()));
            ExpenseSummaryResponse.PaidBack pb = new ExpenseSummaryResponse.PaidBack();
            pb.setId(gs.getId());
            pb.setFromUserId(from);
            pb.setFromName(gs.getFromUser().getName());
            pb.setToUserId(to);
            pb.setToName(gs.getToUser().getName());
            pb.setAmount(rupees(gs.getAmountMinor()));
            pb.setCreatedAt(gs.getCreatedAt());
            paidItems.add(pb);
        }
        ExpenseBalanceCalculator.applyTransfers(net, paidBack);
        List<ExpenseSummaryResponse.Balance> balances = new ArrayList<>();
        net.forEach((uid, v) -> {
            ExpenseSummaryResponse.Balance b = new ExpenseSummaryResponse.Balance();
            b.setUserId(uid);
            b.setName(names.get(uid));
            b.setNet(rupees(v));
            balances.add(b);
        });

        List<ExpenseSummaryResponse.Settlement> settlements = new ArrayList<>();
        for (Transfer t : ExpenseBalanceCalculator.settle(net)) {
            ExpenseSummaryResponse.Settlement s = new ExpenseSummaryResponse.Settlement();
            s.setFromUserId(t.fromUserId());
            s.setFromName(names.get(t.fromUserId()));
            s.setToUserId(t.toUserId());
            s.setToName(names.get(t.toUserId()));
            s.setToUpiId(upis.get(t.toUserId()));
            s.setAmount(rupees(t.amountMinor()));
            settlements.add(s);
        }

        ExpenseSummaryResponse response = new ExpenseSummaryResponse();
        response.setTotal(rupees(total));
        response.setExpenses(items);
        response.setBalances(balances);
        response.setSettlements(settlements);
        response.setPayments(paidItems);
        return response;
    }

    private static BigDecimal rupees(long minor) {
        return BigDecimal.valueOf(minor, 2);
    }
}
