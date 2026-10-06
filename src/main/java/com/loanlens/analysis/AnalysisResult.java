package com.loanlens.analysis;

import com.loanlens.check.RedFlag;
import com.loanlens.model.LoanTerms;

import java.util.List;
import java.util.Map;

/** Everything the UI shows for one document. */
public record AnalysisResult(
        String source,
        LoanTerms terms,
        Map<String, Boolean> verifiedInDocument,
        double recalculatedMonthlyPayment,
        double fiveYearCost,
        List<RedFlag> flags,
        String explanation,
        String redactedPreview,
        long millis,
        String model) {
}
