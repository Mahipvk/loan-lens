package com.loanlens.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.loanlens.analysis.AnalysisResult;
import com.loanlens.analysis.AnalysisService;
import com.loanlens.analysis.SampleStore;
import com.loanlens.check.RedFlag;
import com.loanlens.model.LoanTerms;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scores the pipeline against ground truth:
 *  - field accuracy: did the LLM extract each of 10 key fields correctly?
 *  - flag precision/recall: did we raise exactly the expected red flags?
 */
@Service
public class EvalService {

    public record FieldResult(String field, String expected, String actual, boolean correct) {
    }

    public record SampleResult(String sampleId, int fieldsCorrect, int fieldsTotal, List<FieldResult> fields,
                               List<String> expectedFlags, List<String> actualFlags, long millis) {
    }

    public record EvalSummary(String model, int samples, double fieldAccuracy, double flagPrecision,
                              double flagRecall, long avgMillis, List<SampleResult> results) {
    }

    private final SampleStore samples;
    private final AnalysisService analysis;

    public EvalService(SampleStore samples, AnalysisService analysis) {
        this.samples = samples;
        this.analysis = analysis;
    }

    public EvalSummary runAll() {
        List<SampleResult> results = new ArrayList<>();
        int fieldsCorrect = 0, fieldsTotal = 0, tp = 0, fp = 0, fn = 0;
        long totalMillis = 0;
        String model = "";
        for (SampleStore.Sample s : samples.all()) {
            AnalysisResult r = analysis.analyze(s.id(), samples.pdf(s.id()), false, "English");
            model = r.model();
            totalMillis += r.millis();
            List<FieldResult> fields = compare(s.truth().get("expected"), r.terms());
            int ok = (int) fields.stream().filter(FieldResult::correct).count();
            fieldsCorrect += ok;
            fieldsTotal += fields.size();

            Set<String> expected = new HashSet<>();
            s.truth().get("expectedFlags").forEach(n -> expected.add(n.asText()));
            Set<String> actual = new HashSet<>(r.flags().stream().map(RedFlag::code).toList());
            for (String a : actual) {
                if (expected.contains(a)) tp++; else fp++;
            }
            for (String e : expected) {
                if (!actual.contains(e)) fn++;
            }
            results.add(new SampleResult(s.id(), ok, fields.size(), fields,
                    expected.stream().sorted().toList(), actual.stream().sorted().toList(), r.millis()));
        }
        int n = results.size();
        return new EvalSummary(model, n,
                fieldsTotal == 0 ? 0 : (double) fieldsCorrect / fieldsTotal,
                tp + fp == 0 ? 1.0 : (double) tp / (tp + fp),
                tp + fn == 0 ? 1.0 : (double) tp / (tp + fn),
                n == 0 ? 0 : totalMillis / n, results);
    }

    static List<FieldResult> compare(JsonNode expected, LoanTerms t) {
        Map<String, Object> actual = new LinkedHashMap<>();
        actual.put("loanAmount", t.loanAmount());
        actual.put("interestRate", t.interestRate());
        actual.put("loanTermYears", t.loanTermYears());
        actual.put("monthlyPrincipalInterest", t.monthlyPrincipalInterest());
        actual.put("apr", t.apr());
        actual.put("originationCharges", t.originationCharges());
        actual.put("totalClosingCosts", t.totalClosingCosts());
        actual.put("prepaymentPenalty", t.prepaymentPenalty());
        actual.put("balloonPayment", t.balloonPayment());
        actual.put("rateLocked", t.rateLocked());

        List<FieldResult> out = new ArrayList<>();
        for (Map.Entry<String, Object> e : actual.entrySet()) {
            JsonNode exp = expected.get(e.getKey());
            Object act = e.getValue();
            boolean correct;
            if (exp == null || act == null) {
                correct = exp == null && act == null;
            } else if (exp.isBoolean()) {
                correct = act instanceof Boolean b && b == exp.asBoolean();
            } else {
                double a = ((Number) act).doubleValue();
                correct = Math.abs(a - exp.asDouble()) <= 0.005 + 1e-9;
            }
            out.add(new FieldResult(e.getKey(), exp == null ? "null" : exp.asText(), String.valueOf(act), correct));
        }
        return out;
    }
}
