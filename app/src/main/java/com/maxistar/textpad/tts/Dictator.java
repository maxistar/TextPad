package com.maxistar.textpad.tts;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.maxistar.textpad.R;
import com.maxistar.textpad.TPStrings;

import java.util.ArrayList;
import java.util.Locale;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Dictator implements RecognitionListener  {

    private final String LOG_TAG = "VoiceRecognition";

    private ProgressBar speechProgressBar;

    private SpeechRecognizer speech = null;

    private EditText mText;

    VoiceReader voiceReader;

    public Dictator() {
        voiceReader = new VoiceReader();
    }

    Context context;

    String lastDictation = "";

    static final int COMMAND_MODE = 0;
    static final int DICTATION_MODE = 1;

    static final int COMMAND_NONE = 0;
    static final int COMMAND_UNKNOWN = 1;
    static final int COMMAND_DICTATE = 2;
    static final int COMMAND_EXIT = 3;
    static final int COMMAND_REPEAT = 4;

    int mode = 0;
    int lastCommand = 0;

    public void startDictation(EditText mText, Activity context, ProgressBar speechProgressBar) {

        this.context = context;


        this.speechProgressBar = speechProgressBar;
        this.mText = mText;
        checkPermissions(context);

        speech = SpeechRecognizer.createSpeechRecognizer(context);
        speech.setRecognitionListener(this);
        //startHearing(context);

        sayWaitingForCommand();
    }

    public void sayWaitingForCommand() {
        sayMessage(R.string.tts_waiting_the_command);
    }


    public void sayReady() {
        sayMessage(R.string.tts_ready_to_dictate);
    }

    public void sayMessage(int stringId) {
        sayString(context.getResources().getString(stringId));
    }

    public void sayString(String str) {
        Locale locale = Locale.getDefault();
        String timeStr = String.valueOf(System.currentTimeMillis());
        voiceReader.speak(str, "somerandomkey" + timeStr, locale);
    }

    public void startHearing(Context context) {
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        String language = "ru";
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
        speech.startListening(recognizerIntent);
    }

    private void checkPermissions(Activity context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(context, "", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        speechProgressBar.setVisibility(View.VISIBLE);
        Log.i(LOG_TAG, "onBeginningOfSpeech");
        speechProgressBar.setIndeterminate(false);
        speechProgressBar.setMax(10);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        speechProgressBar.setProgress((int) rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
        Log.i(LOG_TAG, "onBufferReceived: " +  bytes);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        speechProgressBar.setIndeterminate(true);
        //toggleButton.setChecked(false);
    }

    @Override
    public void onError(int errorCode) {
        speechProgressBar.setVisibility(View.INVISIBLE);
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
    }

    @Override
    public void onResults(Bundle results) {
        speechProgressBar.setVisibility(View.INVISIBLE);
        Log.i("LOG", "onResults");
        ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (mode == COMMAND_MODE) {
            if (isCommandDictate(result)) {
                lastCommand = COMMAND_DICTATE;
                sayReady();
            } else if (isCommandRepeat(result)) {
                lastCommand = COMMAND_REPEAT;
                repeatLast();
            } else if (isCommandExit(result)) {
                lastCommand = COMMAND_EXIT;
                sayGoodBye();
            } else {
                lastCommand = COMMAND_UNKNOWN;
                sayUnknownCommand();
            }
        } else { //dictation
            lastDictation = result.get(0);
            this.mText.setText(result.get(0) + "\n" + this.mText.getText());
            mode = COMMAND_MODE;
            lastCommand = COMMAND_NONE;
            sayWaitingForCommand();
        }
    }

    void sayGoodBye() {
        sayMessage(R.string.tts_good_bye);
    }

    void sayUnknownCommand() {
        sayMessage(R.string.tts_unknown_command);
    }

    void repeatLast() {
        sayString(lastDictation);
    }

    boolean isCommandRepeat(ArrayList<String> result) {
        String command = context.getResources().getString(R.string.tts_repeat);
        for (String variant : result) {
            if (command.toLowerCase().equals(variant.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    boolean isCommandDictate(ArrayList<String> result) {
        String command = context.getResources().getString(R.string.tts_dictate);
        for (String variant : result) {
            if (command.toLowerCase().equals(variant.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    boolean isCommandExit(ArrayList<String> result) {
        String command = context.getResources().getString(R.string.tts_done);
        for (String variant : result) {
            if (command.toLowerCase().equals(variant.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPartialResults(Bundle bundle) {
        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onEvent(int i, Bundle bundle) {
        Log.i(LOG_TAG, "onEvent");
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    class VoiceReader {
        TextToSpeech tts;
        boolean ttsReady = false;
        boolean speaking = false;
        String text;
        String textId;
        Locale locale;

        void init(Context context) {
            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    // set our locale only if init was success.
                    if (status == TextToSpeech.SUCCESS) {
                        ttsReady = true;
                        speak(text, "word:" + textId, locale);
                    }
                }
            });
            tts.setLanguage(locale);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String s) {
                    speaking = true;
                }

                @Override
                public void onDone(String s) {
                    speaking = false;
                    if (mode == COMMAND_MODE) {
                        if (lastCommand == COMMAND_DICTATE) {
                            mode = DICTATION_MODE;
                            startHearingDelay();
                        } else if (lastCommand == COMMAND_EXIT) {
                            //exit
                        } else if (lastCommand == COMMAND_REPEAT) {
                            lastCommand = COMMAND_NONE;
                            sayWaitingForCommand();
                        } else if (lastCommand == COMMAND_UNKNOWN) {
                            startHearingDelay();
                        } else {
                            startHearingDelay();
                        }
                    }
                }

                @Override
                public void onError(String s) {
                    speaking = false;
                }
            });
        }

        void startHearingDelay() {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startHearing(context);
                }
            }, 100);
        }

        private void speak(String phrase, String id, Locale locale) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
            this.text = phrase;
            this.textId = id;
            this.locale = locale;
            if (tts == null) {
                this.init(context);
                return;
            }
            if (!ttsReady) {
                return;
            }

            tts.setLanguage(locale);
            tts.speak(phrase, TextToSpeech.QUEUE_ADD, null, id);
        }
    }
}
