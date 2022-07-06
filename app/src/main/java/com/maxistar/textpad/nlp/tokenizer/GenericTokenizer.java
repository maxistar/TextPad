package com.maxistar.textpad.nlp.tokenizer;

public class GenericTokenizer {
    public String[] tokenizeString(String input) {

        String PUNCTUATION_SPACE = " ";
        String PUNCTUATION_COMA = ",";
        String PUNCTUATION_N = ",";

        return new String[]{
                "Однажды", PUNCTUATION_COMA, PUNCTUATION_SPACE, "в", PUNCTUATION_SPACE, "студеную", PUNCTUATION_SPACE, "зимнюю", PUNCTUATION_SPACE, "пору", PUNCTUATION_N
        };
    }
}
