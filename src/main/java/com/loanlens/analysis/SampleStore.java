package com.loanlens.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The bundled synthetic Loan Estimates (PDF) and their ground-truth answers (JSON) for the evals. */
@Component
public class SampleStore {

    public record Sample(String id, String lender, JsonNode truth) {
    }

    private final List<Sample> samples = new ArrayList<>();

    public SampleStore(ObjectMapper mapper) throws IOException {
        for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath:samples/*.json")) {
            try (InputStream in = r.getInputStream()) {
                JsonNode n = mapper.readTree(in);
                samples.add(new Sample(n.get("id").asText(), n.get("lender").asText(), n));
            }
        }
        samples.sort(Comparator.comparing(Sample::id));
    }

    public List<Sample> all() {
        return List.copyOf(samples);
    }

    public Sample get(String id) {
        return samples.stream().filter(s -> s.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown sample: " + id));
    }

    public byte[] pdf(String id) {
        get(id);
        try (InputStream in = new ClassPathResource("samples/" + id + ".pdf").getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Missing PDF for sample " + id, e);
        }
    }
}
