package com.loanlens.check;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCalculatorTest {

    @Test
    void matchesStandardAmortization() {
        assertThat(PaymentCalculator.monthlyPrincipalAndInterest(320000, 6.25, 30)).isEqualTo(1970.30);
        assertThat(PaymentCalculator.monthlyPrincipalAndInterest(250000, 5.5, 15)).isEqualTo(2042.71);
    }

    @Test
    void fiveYearCostIsSixtyPaymentsPlusClosing() {
        assertThat(PaymentCalculator.fiveYearCost(1000, 5000)).isEqualTo(65000);
    }
}
