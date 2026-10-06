package com.loanlens.check;

import com.loanlens.model.LoanTerms;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic rules on the extracted terms. Kept in Java (not the LLM) so the same loan always gets
 * the same flags and every flag can be explained and tested.
 */
@Component
public class RedFlagRules {

    public List<RedFlag> evaluate(LoanTerms t) {
        List<RedFlag> flags = new ArrayList<>();
        if (Boolean.TRUE.equals(t.prepaymentPenalty())) {
            flags.add(new RedFlag("PREPAYMENT_PENALTY", "This loan has a prepayment penalty",
                    "You could be charged a fee if you sell, refinance or pay off the loan early.",
                    "How much is the penalty, for how many years, and can you offer the loan without it?"));
        }
        if (Boolean.TRUE.equals(t.balloonPayment())) {
            flags.add(new RedFlag("BALLOON_PAYMENT", "This loan has a balloon payment",
                    "A large lump sum will be due at a set date, on top of your monthly payments.",
                    "When is the balloon due, how much is it, and what happens if I can't refinance?"));
        }
        if (Boolean.FALSE.equals(t.rateLocked())) {
            flags.add(new RedFlag("RATE_NOT_LOCKED", "Your interest rate is not locked",
                    "The rate and points can still change before closing, so your payment could go up.",
                    "Can I lock this rate now, for how long, and what does the lock cost?"));
        }
        if (t.loanType() != null && t.loanType().toLowerCase(Locale.ROOT).contains("adjust")) {
            flags.add(new RedFlag("ADJUSTABLE_RATE", "The interest rate can change",
                    "After the fixed period ends, your rate and monthly payment can rise.",
                    "How high can the rate and payment go, and when is the first change?"));
        }
        if (t.originationCharges() != null && t.loanAmount() != null && t.loanAmount() > 0
                && t.originationCharges() > 0.01 * t.loanAmount()) {
            double pct = t.originationCharges() / t.loanAmount() * 100;
            flags.add(new RedFlag("HIGH_ORIGINATION", String.format(Locale.US, "Lender fees are %.1f%% of the loan", pct),
                    "Origination charges above about 1% of the loan amount are worth comparing with other lenders.",
                    "Which of these fees can be reduced or waived?"));
        }
        if (t.pointsPercent() != null && t.pointsPercent() >= 1.0) {
            flags.add(new RedFlag("POINTS_PAID", String.format(Locale.US, "You are paying %.2f points", t.pointsPercent()),
                    "Points are upfront interest that buys a lower rate. They only pay off if you keep the loan long enough.",
                    "What is the rate without points, and how many years until the points pay for themselves?"));
        }
        if (t.apr() != null && t.interestRate() != null && t.apr() - t.interestRate() > 0.5) {
            flags.add(new RedFlag("APR_GAP", "The APR is much higher than the interest rate",
                    String.format(Locale.US, "A gap of %.2f points usually means significant fees are built into the loan.", t.apr() - t.interestRate()),
                    "Which costs make up the difference between the rate and the APR?"));
        }
        if (t.loanAmount() != null && t.interestRate() != null && t.loanTermYears() != null
                && t.monthlyPrincipalInterest() != null && !isAdjustable(t)) {
            double expected = PaymentCalculator.monthlyPrincipalAndInterest(t.loanAmount(), t.interestRate(), t.loanTermYears());
            if (Math.abs(expected - t.monthlyPrincipalInterest()) > 1.0) {
                flags.add(new RedFlag("PAYMENT_MISMATCH", "The monthly payment doesn't add up",
                        String.format(Locale.US, "For this amount, rate and term the payment should be about $%,.2f, but the document shows $%,.2f.",
                                expected, t.monthlyPrincipalInterest()),
                        "Can you explain or correct the monthly principal and interest figure?"));
            }
        }
        return flags;
    }

    private static boolean isAdjustable(LoanTerms t) {
        return t.loanType() != null && t.loanType().toLowerCase(Locale.ROOT).contains("adjust");
    }
}
