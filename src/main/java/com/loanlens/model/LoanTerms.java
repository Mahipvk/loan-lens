package com.loanlens.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** What the LLM must extract from the Loan Estimate. Spring AI turns this record into a JSON schema. */
public record LoanTerms(
        @JsonPropertyDescription("Lender name at the top of the document")
        String lender,
        @JsonPropertyDescription("Fixed Rate or Adjustable Rate, from the PRODUCT field")
        String loanType,
        @JsonPropertyDescription("Loan term in years")
        Integer loanTermYears,
        @JsonPropertyDescription("Loan Amount in dollars, number only")
        Double loanAmount,
        @JsonPropertyDescription("Interest Rate in percent, e.g. 6.25")
        Double interestRate,
        @JsonPropertyDescription("Monthly Principal & Interest in dollars, exactly as printed")
        Double monthlyPrincipalInterest,
        @JsonPropertyDescription("Annual Percentage Rate (APR) in percent")
        Double apr,
        @JsonPropertyDescription("true if RATE LOCK says YES, false if NO")
        Boolean rateLocked,
        @JsonPropertyDescription("true if Prepayment Penalty says YES")
        Boolean prepaymentPenalty,
        @JsonPropertyDescription("true if Balloon Payment says YES")
        Boolean balloonPayment,
        @JsonPropertyDescription("Points as a percent of the loan amount, e.g. 1.5 (0 if none)")
        Double pointsPercent,
        @JsonPropertyDescription("Total of section A. Origination Charges in dollars")
        Double originationCharges,
        @JsonPropertyDescription("Estimated Closing Costs in dollars")
        Double totalClosingCosts,
        @JsonPropertyDescription("Estimated Cash to Close in dollars")
        Double cashToClose,
        @JsonPropertyDescription("Each line under A. Origination Charges, with its dollar amount")
        List<Fee> originationFees) {

    public record Fee(String name, Double amount) {
    }
}
