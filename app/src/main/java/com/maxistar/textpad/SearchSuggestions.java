package com.maxistar.textpad;

import android.content.SearchRecentSuggestionsProvider;

/* SearchSuggestions
 * 		Pretty simple to provide search suggestions */
public class SearchSuggestions extends SearchRecentSuggestionsProvider {
    public final static int MODE = DATABASE_MODE_QUERIES;

    public SearchSuggestions() {
        setupSuggestions(TPStrings.AUTHORITY, MODE);
    }
} // end class SearchSuggestions