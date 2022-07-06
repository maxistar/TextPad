package com.maxistar.textpad.test.nlp.tokenizer;

import com.maxistar.textpad.nlp.tokenizer.GenericTokenizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GenericTokenizerTest {

    @Test
    public void testTokenizer() {
        String inputString = "Однажды, в студеную зимнюю пору\n" /*+
                "Я из лесу вышел; был сильный мороз.\n" +
                "Гляжу, поднимается медленно в гору\n" +
                "Лошадка, везущая хворосту воз.\n" +
                "И шествуя важно, в спокойствии чинном,\n" +
                "Лошадку ведет под уздцы мужичок\n" +
                "В больших сапогах, в полушубке овчинном,\n" +
                "В больших рукавицах… а сам с ноготок!\n" +
                "«Здорово парнище!» — «Ступай себе мимо!»\n" +
                "— «Уж больно ты грозен, как я погляжу!\n" +
                "Откуда дровишки?» — «Из лесу, вестимо;\n" +
                "Отец, слышишь, рубит, а я отвожу».\n" +
                "(В лесу раздавался топор дровосека.)\n" +
                "«А что, у отца-то большая семья?»\n" +
                "— «Семья-то большая, да два человека\n" +
                "Всего мужиков-то: отец мой да я…»\n" +
                "— «Так вот оно что! А как звать тебя?» — «Власом».\n" +
                "— «А кой-тебе годик?» — «Шестой миновал…\n" +
                "Ну, мертвая!» — крикнул малюточка басом,\n" +
                "Рванул под уздцы и быстрей зашагал."*/;

        String PUNCTUATION_SPACE = " ";
        String PUNCTUATION_COMA = ",";
        String PUNCTUATION_N = ",";

        String[] expectedTokens = new String[] {
                "Однажды", PUNCTUATION_COMA, PUNCTUATION_SPACE, "в", PUNCTUATION_SPACE, "студеную", PUNCTUATION_SPACE, "зимнюю", PUNCTUATION_SPACE, "пору", PUNCTUATION_N
        };

        GenericTokenizer tokenizer = new GenericTokenizer();
        String[] actualTokens = tokenizer.tokenizeString(inputString);


        assertEquals(expectedTokens, actualTokens);
    }

}
