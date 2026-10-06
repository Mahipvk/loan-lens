package com.loanlens.guard;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Guardrail: masks personal data in the document text BEFORE it reaches the LLM.
 * Uses the form's own labels (APPLICANTS, LOAN ID #, CONTACT) plus generic patterns, because
 * names can't be found reliably with a regex but labelled fields can.
 */
public class Redactor {

    private static final Map<Pattern, String> RULES = new LinkedHashMap<>();

    static {
        RULES.put(Pattern.compile("(?im)^(\\s*APPLICANTS?\\s+).+$"), "$1[NAME]");
        RULES.put(Pattern.compile("(?im)^(\\s*PROPERTY\\s+).+$"), "$1[ADDRESS]");
        RULES.put(Pattern.compile("(?i)(LOAN ID\\s*#?\\s*)\\d+"), "$1[LOAN_ID]");
        RULES.put(Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[EMAIL]");
        RULES.put(Pattern.compile("\\(?\\b\\d{3}\\)?[ .-]?\\d{3}[ .-]\\d{4}\\b"), "[PHONE]");
        RULES.put(Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"), "[SSN]");
        RULES.put(Pattern.compile("\\b(?:\\d[ -]?){13,16}\\b"), "[ACCOUNT]");
    }

    public String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = text;
        for (Map.Entry<Pattern, String> rule : RULES.entrySet()) {
            out = rule.getKey().matcher(out).replaceAll(rule.getValue());
        }
        return out;
    }
}
