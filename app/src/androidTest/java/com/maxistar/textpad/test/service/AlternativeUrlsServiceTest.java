package com.maxistar.textpad.test.service;

import android.content.Context;
import android.net.Uri;

import com.maxistar.textpad.service.AlternativeUrlsService;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import androidx.test.core.app.ApplicationProvider;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AlternativeUrlsServiceTest {
    static final String ALTERNATIVE_URLS_FILENAME = "alternative_urls";

    // @BeforeEach
    public void setUp() {
        deleteAlternativeUrls();
    }

    private void deleteAlternativeUrls() {
        FileOutputStream fos;
        Context context = ApplicationProvider.getApplicationContext();
        ArrayList<String> recentFiles = new ArrayList<>();
        try {
            fos = context.openFileOutput(ALTERNATIVE_URLS_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream objectOut = new ObjectOutputStream(fos);
            objectOut.writeObject(recentFiles);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // @Test
    public void testUrls() {
        Context context = ApplicationProvider.getApplicationContext();
        AlternativeUrlsService recentFilesService = new AlternativeUrlsService();

        recentFilesService.addAlternativeUrl(
            Uri.parse("file:///storage/emulated/0/Documents/work/%D0%B8%D0%B4%D0%B5%D0%B8.md"),
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fwork%2F%D0%B8%D0%B4%D0%B5%D0%B8.md"),
            context
        );

        AlternativeUrlsService recentFilesService2 = new AlternativeUrlsService();

        assertTrue(
            recentFilesService2.hasAlternativeUrl(
                Uri.parse("file:///storage/emulated/0/Documents/work/%D0%B8%D0%B4%D0%B5%D0%B8.md"),
                context
            )
        );

        Uri uri = recentFilesService2.getAlternativeUrl(
            Uri.parse("file:///storage/emulated/0/Documents/work/%D0%B8%D0%B4%D0%B5%D0%B8.md"),
            context
        );

        assertEquals(
            uri,
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fwork%2F%D0%B8%D0%B4%D0%B5%D0%B8.md")
        );

        recentFilesService2.clearAlternativeUrls(context);

        assertFalse(
            recentFilesService2.hasAlternativeUrl(
                Uri.parse("file:///storage/emulated/0/Documents/work/%D0%B8%D0%B4%D0%B5%D0%B8.md"),
                context
            )
        );
    }
}
