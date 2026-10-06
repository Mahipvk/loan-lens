package com.loanlens.guard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedactorTest {

    @Test
    void masksPersonalDetailsButKeepsLoanNumbers() {
        String doc = """
                APPLICANTS  Jordan A. Rivera
                CONTACT  jordan.rivera@example.com   (703) 555-0142
                PROPERTY  1200 Example Lane, Springfield, VA 22150
                LOAN TYPE  Conventional      LOAN ID #  0104846730
                Loan Amount $320,000
                Monthly Principal & Interest $1,970.30
                """;
        String out = new Redactor().redact(doc);
        assertThat(out).doesNotContain("Jordan", "rivera@", "555-0142", "Example Lane", "0104846730");
        assertThat(out).contains("[NAME]", "[EMAIL]", "[PHONE]", "[ADDRESS]", "[LOAN_ID]", "$320,000", "$1,970.30");
    }
}
