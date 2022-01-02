package com.maxistar.textpad.service;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.ListIterator;

public class RecentFilesService {
    static final int MAX_ELEMENTS_STORED = 10;
    static final int MAX_ELEMENTS_SHOWN = 5;
    static final String RECENT_FILES_FILENAME = "recent_files_filename";

    ArrayList<String> items;

    public RecentFilesService() {
    }

    /**
     * Add element to recent file
     *
     * @param url Url
     * @param context Context
     */
    public void addRecentFile(String url, Context context) {
        loadItems(context);

        //items.remove(url);
        items.add(url);
        removeOldestItems(url);


        saveRecentFiles(items, context);
    }

    private void removeOldestItems(String url) {
        ArrayList<String> newItems = new ArrayList<>();
        int counter = 0;
        int lastIndex = items.size();
        int skip = items.size() - MAX_ELEMENTS_STORED;
        for(String item: items) {
            counter++;
            if (counter != lastIndex && item.equals(url)) {
                continue;
            }
            if (counter <= skip) {
                continue;
            }
            newItems.add(item);
        }
        this.items = newItems;
    }

    private void loadItems(Context context) {
        if (items == null) {
            items = loadRecentFiles(context);
        }
    }

    /**
     * Get Last files in reverse order, show last added file first, skipping n last added files
     *
     * @param skip Number to skip
     * @param context Context
     * @return Recent Urls
     */
    public ArrayList<String> getLastFiles(int skip, Context context) {
        loadItems(context);
        ArrayList<String> result = new ArrayList<>();
        int counter = 0;
        ListIterator<String> it = items.listIterator(items.size());
        //it.
        while (it.hasPrevious()) {
            String name = it.previous();
            counter++;
            if (counter <= skip) {
                continue;
            }
            if (counter > MAX_ELEMENTS_SHOWN + 1) {
                continue;
            }

            result.add(name);
        }

        return  result;
    }

    private ArrayList<String> loadRecentFiles(Context context) {
        ArrayList<String> history = new ArrayList<>();
        try {
            FileInputStream fis = context.getApplicationContext().openFileInput(RECENT_FILES_FILENAME);
            ObjectInputStream objectIn = new ObjectInputStream(fis);
            history = (ArrayList<String>) objectIn.readObject();
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
        return history;
    }

    private void saveRecentFiles(ArrayList<String> recentFiles, Context context)
    {
        FileOutputStream fos;
        try {
            fos = context.openFileOutput(RECENT_FILES_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream objectOut = new ObjectOutputStream(fos);
            objectOut.writeObject(recentFiles);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
