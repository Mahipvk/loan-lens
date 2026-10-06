package com.loanlens.check;

import com.loanlens.model.LoanTerms;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedFlagRulesTest {

    private final RedFlagRules rules = new RedFlagRules();

    private static LoanTerms terms(String type, double amount, double rate, double pi, double apr, Boolean locked,
                                   Boolean prepay, double points, double origination) {
        return new LoanTerms("Test Lender", type, 30, amount, rate, pi, apr, locked, prepay, false,
                points, origination, 9000.0, 89000.0, List.of());
    }

    private List<String> codes(LoanTerms t) {
        return rules.evaluate(t).stream().map(RedFlag::code).toList();
    }

    @Test
    void cleanFixedLoanHasNoFlags() {
        assertThat(codes(terms("Fixed Rate", 320000, 6.25, 1970.30, 6.312, true, false, 0, 1600))).isEmpty();
    }

    @Test
    void costlyLoanRaisesExpectedFlags() {
        assertThat(codes(terms("Fixed Rate", 320000, 6.125, 1944.35, 6.398, false, true, 1.5, 7440)))
                .containsExactlyInAnyOrder("PREPAYMENT_PENALTY", "RATE_NOT_LOCKED", "HIGH_ORIGINATION", "POINTS_PAID");
    }

    @Test
    void wrongPrintedPaymentIsCaught() {
        assertThat(codes(terms("Fixed Rate", 320000, 6.25, 2010.30, 6.312, true, false, 0, 1600)))
                .containsExactly("PAYMENT_MISMATCH");
    }
}
