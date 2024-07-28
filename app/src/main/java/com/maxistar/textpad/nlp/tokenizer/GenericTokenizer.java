package com.maxistar.textpad.nlp.tokenizer;

import java.util.ArrayList;

public class GenericTokenizer {
    final static int MODE_DEFAULT = 0;
    final static int MODE_PUNCTUATION = 1;
    final static int MODE_WHITESPACE = 2;
    final static int MODE_WORD = 3;

    public String[] tokenizeString(String input) {
        int length = input.length();
        int mode = MODE_DEFAULT;
        ArrayList<String> result = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String currentChar = input.substring(i, i + 1);
            if (mode == MODE_WORD) {
                if (isLetter(currentChar)) {
                    currentWord.append(currentChar);
                } else if (isPunctuation(currentChar)) {
                    result.add(currentWord.toString());
                    result.add(currentChar);
                    mode = MODE_PUNCTUATION;
                    currentWord = new StringBuilder();
                } else if (isWhitespace(currentChar)) {
                    result.add(currentWord.toString());
                    result.add(currentChar);
                    mode = MODE_WHITESPACE;
                    currentWord = new StringBuilder();
                }
            } else if (mode == MODE_PUNCTUATION) {
                if (isLetter(currentChar)) {
                    currentWord.append(currentChar);
                    mode = MODE_WORD;
                } else if (isPunctuation(currentChar)) {
                    result.add(currentChar);
                } else if (isWhitespace(currentChar)) {
                    result.add(currentChar);
                    mode = MODE_WHITESPACE;
                }
            } else if (mode == MODE_WHITESPACE) {
                if (isLetter(currentChar)) {
                    currentWord.append(currentChar);
                    mode = MODE_WORD;
                } else if (isPunctuation(currentChar)) {
                    result.add(currentChar);
                    mode = MODE_PUNCTUATION;
                } else if (isWhitespace(currentChar)) {
                    result.add(currentChar);
                }
            } else {
                if (isLetter(currentChar)) {
                    currentWord.append(currentChar);
                    mode = MODE_WORD;
                } else if (isPunctuation(currentChar)) {
                    result.add(currentChar);
                    mode = MODE_PUNCTUATION;
                } else if (isWhitespace(currentChar)) {
                    result.add(currentChar);
                    mode = MODE_WHITESPACE;
                }
            }
        }
        String[] res = new String[result.size()];
        return result.toArray(res);
    }

    private boolean isWhitespace(String currentChar) {
        return currentChar.equals(" ") || currentChar.equals("\n");
    }

    private boolean isPunctuation(String currentChar) {
        return currentChar.matches("[—?,:;.…!«»)(]");
    }

    private boolean isLetter(String currentChar) {
        return currentChar.matches("[a-zA-Zа-яА-Я\\-]");
    }
}
