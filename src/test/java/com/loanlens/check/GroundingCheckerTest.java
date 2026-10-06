package com.loanlens.check;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GroundingCheckerTest {

    private final String doc = "Loan Amount $320,000\nInterest Rate 6.250 %\nMonthly Principal & Interest $1,970.30\nAPR 6.312 %";

    @Test
    void findsNumbersAsPrinted() {
        assertThat(GroundingChecker.appearsInText(320000.0, doc)).isTrue();
        assertThat(GroundingChecker.appearsInText(6.25, doc)).isTrue();
        assertThat(GroundingChecker.appearsInText(1970.3, doc)).isTrue();
        assertThat(GroundingChecker.appearsInText(6.312, doc)).isTrue();
    }

    @Test
    void rejectsNumbersThatAreNotThere() {
        assertThat(GroundingChecker.appearsInText(1970.0, doc)).isFalse();
        assertThat(GroundingChecker.appearsInText(6.3, doc)).isFalse();
        assertThat(GroundingChecker.appearsInText(null, doc)).isFalse();
    }
}
