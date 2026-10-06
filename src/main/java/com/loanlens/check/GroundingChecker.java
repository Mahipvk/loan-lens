package com.loanlens.check;

import java.util.Locale;

/**
 * "Don't trust, verify": checks that a number the LLM extracted actually appears in the document text.
 * Catches hallucinated or mis-read values before they are shown to the user.
 */
public final class GroundingChecker {

    private GroundingChecker() {
    }

    public static boolean appearsInText(Double value, String documentText) {
        if (value == null || documentText == null) {
            return false;
        }
        String text = documentText.replace(",", "").replace("$", "");
        double v = value;
        String[] candidates = {
                String.format(Locale.US, "%.0f", v),
                String.format(Locale.US, "%.2f", v),
                String.format(Locale.US, "%.3f", v),
                String.format(Locale.US, "%.1f", v),
        };
        for (String c : candidates) {
            // whole-number match only when the value really is whole (avoid 6.25 matching "6")
            if (c.indexOf('.') < 0 && v != Math.rint(v)) {
                continue;
            }
            if (containsNumber(text, c)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsNumber(String text, String number) {
        int from = 0;
        while (true) {
            int i = text.indexOf(number, from);
            if (i < 0) {
                return false;
            }
            boolean leftOk = i == 0 || !Character.isDigit(text.charAt(i - 1));
            int end = i + number.length();
            boolean rightOk = end >= text.length() || !(Character.isDigit(text.charAt(end))
                    || (text.charAt(end) == '.' && end + 1 < text.length() && Character.isDigit(text.charAt(end + 1))));
            if (leftOk && rightOk) {
                return true;
            }
            from = i + 1;
        }
    }
}
