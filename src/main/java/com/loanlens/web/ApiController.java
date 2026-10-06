package com.loanlens.web;

import com.loanlens.analysis.AnalysisResult;
import com.loanlens.analysis.AnalysisService;
import com.loanlens.analysis.SampleStore;
import com.loanlens.check.PaymentCalculator;
import com.loanlens.eval.EvalService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    public record SampleInfo(String id, String lender) {
    }

    public record Comparison(AnalysisResult a, AnalysisResult b, String lowerFiveYearCost, double difference) {
    }

    private final SampleStore samples;
    private final AnalysisService analysis;
    private final EvalService evals;

    public ApiController(SampleStore samples, AnalysisService analysis, EvalService evals) {
        this.samples = samples;
        this.analysis = analysis;
        this.evals = evals;
    }

    @GetMapping("/samples")
    public List<SampleInfo> samples() {
        return samples.all().stream().map(s -> new SampleInfo(s.id(), s.lender())).toList();
    }

    @GetMapping(value = "/samples/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] samplePdf(@PathVariable String id) {
        return samples.pdf(id);
    }

    @PostMapping("/analyze/sample/{id}")
    public AnalysisResult analyzeSample(@PathVariable String id, @RequestParam(defaultValue = "English") String language) {
        return analysis.analyze(id, samples.pdf(id), true, language);
    }

    @PostMapping(value = "/analyze/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResult analyzeUpload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(defaultValue = "English") String language) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a PDF file");
        }
        return analysis.analyze(file.getOriginalFilename(), file.getBytes(), true, language);
    }

    @PostMapping("/compare")
    public Comparison compare(@RequestParam String a, @RequestParam String b) {
        AnalysisResult ra = analysis.analyze(a, samples.pdf(a), false, "English");
        AnalysisResult rb = analysis.analyze(b, samples.pdf(b), false, "English");
        String lower = ra.fiveYearCost() <= rb.fiveYearCost() ? a : b;
        return new Comparison(ra, rb, lower, PaymentCalculator.round2(Math.abs(ra.fiveYearCost() - rb.fiveYearCost())));
    }

    @PostMapping("/evals")
    public EvalService.EvalSummary evals() {
        return evals.runAll();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
