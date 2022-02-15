package com.maxistar.textpad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class AboutBox extends DialogPreference
{
    // This is the constructor called by the inflater
    public AboutBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected View onCreateDialogView() {
        final SpannableString s =
                new SpannableString(l(R.string.about_message));;
        Linkify.addLinks(s, Linkify.WEB_URLS);
        final TextView view = new TextView(getContext());
        view.setText(s);
        view.setPadding(32, 32, 32, 32);
        view.setMovementMethod(LinkMovementMethod.getInstance());
        return view;
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setTitle("About Textpad");
        builder.setPositiveButton(R.string.Continue, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        builder.setNegativeButton(null, null);
    }

    String l(int id) {
        return getContext().getResources().getString(id);
    }

}
