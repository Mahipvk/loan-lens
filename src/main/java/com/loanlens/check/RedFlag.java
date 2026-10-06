package com.loanlens.check;

/** A plain-language "question to ask your lender". Produced by deterministic rules, not by the LLM. */
public record RedFlag(String code, String title, String why, String askYourLender) {
}
