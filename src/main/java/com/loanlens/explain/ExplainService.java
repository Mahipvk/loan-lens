package com.loanlens.explain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanlens.check.RedFlag;
import com.loanlens.model.LoanTerms;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Step 5: the LLM explains the loan to a first-time buyer. It only sees the VERIFIED numbers and the
 * rule-based flags, never the raw document, so it can't invent new figures.
 */
@Service
public class ExplainService {

    private static final String SYSTEM = """
            You explain mortgage Loan Estimates to first-time home buyers in plain, friendly language.
            Use ONLY the numbers given to you. Do not invent figures, rates or fees.
            Do not tell the person which loan to choose and do not give financial or legal advice;
            explain what the terms mean and what to ask the lender.
            Never guess the reason for a flagged problem. If a figure in the document does not match our calculation,
            say plainly that the numbers don't match and that the borrower should ask the lender to explain or correct it.
            When the document's monthly payment and our recalculated payment differ, present the recalculated one as
            "what the payment should be for this amount, rate and term".
            Write about 150-220 words: what the loan is, what they pay each month, what they pay at closing,
            and what each flagged item means. Short paragraphs, no tables.
            """;

    private static final String USER = """
            Write the explanation in {language}.

            LOAN TERMS (extracted and verified):
            {terms}

            CHECKS:
            Recalculated monthly principal and interest: {computedPayment}
            Approximate cost over the first 5 years (60 payments + closing costs): {fiveYear}

            FLAGS (questions to ask the lender):
            {flags}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper mapper;

    public ExplainService(ChatModel chatModel, ObjectMapper mapper) {
        this.chatClient = ChatClient.builder(chatModel).defaultSystem(SYSTEM).build();
        this.mapper = mapper;
    }

    public String explain(LoanTerms terms, List<RedFlag> flags, double computedPayment, double fiveYear, String language) {
        String flagText = flags.isEmpty() ? "none"
                : String.join("\n", flags.stream().map(f -> "- " + f.title() + ": " + f.why() + " Ask: " + f.askYourLender()).toList());
        return chatClient.prompt()
                .user(u -> u.text(USER)
                        .param("language", language == null || language.isBlank() ? "English" : language)
                        .param("terms", toJson(terms))
                        .param("computedPayment", String.format("$%,.2f", computedPayment))
                        .param("fiveYear", String.format("$%,.2f", fiveYear))
                        .param("flags", flagText))
                .call()
                .content();
    }

    private String toJson(LoanTerms terms) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(terms);
        } catch (JsonProcessingException e) {
            return String.valueOf(terms);
        }
    }
}
