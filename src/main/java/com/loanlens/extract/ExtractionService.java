package com.loanlens.extract;

import com.loanlens.model.LoanTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

/** Step 3: the LLM reads the (redacted) document text and fills in the LoanTerms structure. JSON mode, temperature 0. */
@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private static final String SYSTEM = """
            You extract data from US mortgage Loan Estimate documents.
            Copy values exactly as printed. Dollar amounts and percentages are numbers only (no $ or %).
            If a value is not in the document, use null. Never guess or calculate a value that is not printed.
            """;

    private static final String USER = """
            DOCUMENT TEXT:
            {document}

            Extract the loan terms.
            {format}
            """;

    private final ChatClient chatClient;

    public ExtractionService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).defaultSystem(SYSTEM).build();
    }

    public LoanTerms extract(String redactedText) {
        BeanOutputConverter<LoanTerms> converter = new BeanOutputConverter<>(LoanTerms.class);
        String raw = chatClient.prompt()
                .options(OllamaOptions.builder().format("json").build())
                .user(u -> u.text(USER).param("document", redactedText).param("format", converter.getFormat()))
                .call()
                .content();
        try {
            return converter.convert(raw);
        } catch (RuntimeException e) {
            log.warn("Extraction returned invalid JSON: {}", raw);
            throw new IllegalStateException("The model did not return valid JSON for this document", e);
        }
    }
}
