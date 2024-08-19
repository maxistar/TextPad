package com.maxistar.textpad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;

import java.util.List;

public class LanguageDetailsChecker extends BroadcastReceiver
{
    private List<String> supportedLanguages;

    private String languagePreference;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle results = getResultExtras(true);
        if (results.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE))
        {
            languagePreference =
                    results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
        }
        if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES))
        {
            supportedLanguages =
                    results.getStringArrayList(
                            RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
            Log.i("dfdfdf", "Available input method locale: " + supportedLanguages.toString());
        }
    }
}
