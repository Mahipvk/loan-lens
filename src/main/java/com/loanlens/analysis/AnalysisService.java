package com.loanlens.analysis;

import com.loanlens.check.GroundingChecker;
import com.loanlens.check.PaymentCalculator;
import com.loanlens.check.RedFlag;
import com.loanlens.check.RedFlagRules;
import com.loanlens.explain.ExplainService;
import com.loanlens.extract.ExtractionService;
import com.loanlens.guard.Redactor;
import com.loanlens.model.LoanTerms;
import com.loanlens.pdf.PdfTextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The pipeline:  PDF -> text -> redact -> LLM extracts terms -> Java verifies numbers + recalculates payment
 *                -> Java rules raise flags -> LLM explains (from verified data only).
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final PdfTextExtractor pdf;
    private final Redactor redactor = new Redactor();
    private final ExtractionService extraction;
    private final RedFlagRules rules;
    private final ExplainService explainer;
    private final String model;

    public AnalysisService(PdfTextExtractor pdf, ExtractionService extraction, RedFlagRules rules,
                           ExplainService explainer, @Value("${spring.ai.ollama.chat.options.model}") String model) {
        this.pdf = pdf;
        this.extraction = extraction;
        this.rules = rules;
        this.explainer = explainer;
        this.model = model;
    }

    public AnalysisResult analyze(String source, byte[] pdfBytes, boolean withExplanation, String language) {
        long start = System.currentTimeMillis();
        String text = pdf.extract(pdfBytes);
        String redacted = redactor.redact(text);

        LoanTerms terms = extraction.extract(redacted);

        Map<String, Boolean> verified = new LinkedHashMap<>();
        verified.put("loanAmount", GroundingChecker.appearsInText(terms.loanAmount(), text));
        verified.put("interestRate", GroundingChecker.appearsInText(terms.interestRate(), text));
        verified.put("monthlyPrincipalInterest", GroundingChecker.appearsInText(terms.monthlyPrincipalInterest(), text));
        verified.put("apr", GroundingChecker.appearsInText(terms.apr(), text));
        verified.put("originationCharges", GroundingChecker.appearsInText(terms.originationCharges(), text));
        verified.put("totalClosingCosts", GroundingChecker.appearsInText(terms.totalClosingCosts(), text));
        verified.put("cashToClose", GroundingChecker.appearsInText(terms.cashToClose(), text));

        double computed = (terms.loanAmount() != null && terms.interestRate() != null && terms.loanTermYears() != null)
                ? PaymentCalculator.monthlyPrincipalAndInterest(terms.loanAmount(), terms.interestRate(), terms.loanTermYears())
                : 0;
        double fiveYear = PaymentCalculator.fiveYearCost(computed,
                terms.totalClosingCosts() == null ? 0 : terms.totalClosingCosts());

        List<RedFlag> flags = rules.evaluate(terms);
        String explanation = withExplanation ? explainer.explain(terms, flags, computed, fiveYear, language) : null;

        long millis = System.currentTimeMillis() - start;
        log.info("Analyzed {} in {} ms, flags {}", source, millis, flags.stream().map(RedFlag::code).toList());
        String preview = redacted.length() > 600 ? redacted.substring(0, 600) + "..." : redacted;
        return new AnalysisResult(source, terms, verified, computed, fiveYear, flags, explanation, preview, millis, model);
    }
}
