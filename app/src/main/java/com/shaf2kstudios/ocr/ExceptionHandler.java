package com.shaf2kstudios.ocr;

import android.app.Activity;
import android.content.Intent;

public class ExceptionHandler implements
        java.lang.Thread.UncaughtExceptionHandler {
    private final Activity myContext;

    public ExceptionHandler(Activity context) {
        myContext = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/html");
        intent.putExtra(Intent.EXTRA_EMAIL, "shaf2k@gmail.com");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Error Log");
        intent.putExtra(Intent.EXTRA_TEXT, exception.getStackTrace());

        myContext.startActivity(Intent.createChooser(intent, "Send Email"));

    }
}
