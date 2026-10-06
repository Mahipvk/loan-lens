package com.loanlens.check;

/** Standard amortization formula. Used to double-check the monthly payment the LLM read off the page. */
public final class PaymentCalculator {

    private PaymentCalculator() {
    }

    public static double monthlyPrincipalAndInterest(double loanAmount, double annualRatePercent, int years) {
        int n = years * 12;
        double r = annualRatePercent / 100.0 / 12.0;
        if (r == 0) {
            return round2(loanAmount / n);
        }
        return round2(loanAmount * r / (1 - Math.pow(1 + r, -n)));
    }

    /** Approximate cost over the first 5 years: 60 monthly P&I payments plus closing costs. */
    public static double fiveYearCost(double monthlyPI, double closingCosts) {
        return round2(60 * monthlyPI + closingCosts);
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
