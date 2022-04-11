package com.maxistar.textpad.service;

import android.content.Context;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.TreeMap;

public class AlternativeUrlsService {
    static final String ALTERNATIVE_URLS_FILENAME = "alternative_urls";

    TreeMap<String, String> storage;

    private void loadItems(Context context) {
        if (storage == null) {
            storage = loadAlternativeUrl(context);
        }
    }

    public boolean hasAlternativeUrl(Uri uri, Context context) {
        loadItems(context);
        String uriString = uri.toString();
        return storage.containsKey(uriString);
    }

    public Uri getAlternativeUrl(Uri driveUri, Context context) {
        loadItems(context);
        String uriString = driveUri.toString();
        if (storage.containsKey(uriString)) {
            return Uri.parse(storage.get(uriString));
        }
        return null;
    }

    /**
     * @param driveUri Media Url
     * @param mediaUrl File System Url
     */
    public void addAlternativeUrl(Uri driveUri, Uri mediaUrl, Context context) {
        loadItems(context);
        storage.put(driveUri.toString(), mediaUrl.toString());
        saveAlternativeUrls(storage, context);
    }

    public void clearAlternativeUrls(Context context) {
        storage = new TreeMap<>();
        saveAlternativeUrls(storage, context);
    }

    private TreeMap<String, String> loadAlternativeUrl(Context context) {
        TreeMap<String, String> alternativeUris = new TreeMap<>();
        try {
            FileInputStream fis = context.getApplicationContext().openFileInput(ALTERNATIVE_URLS_FILENAME);
            ObjectInputStream objectIn = new ObjectInputStream(fis);
            alternativeUris = (TreeMap<String, String>) objectIn.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return alternativeUris;
    }

    private void saveAlternativeUrls(TreeMap<String, String> alternativeUrls, Context context)
    {
        FileOutputStream fos;
        try {
            fos = context.openFileOutput(ALTERNATIVE_URLS_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream objectOut = new ObjectOutputStream(fos);
            objectOut.writeObject(alternativeUrls);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
