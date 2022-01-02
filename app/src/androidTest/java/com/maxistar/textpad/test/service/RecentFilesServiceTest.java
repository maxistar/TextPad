package com.maxistar.textpad.test.service;

import android.content.Context;

import com.maxistar.textpad.service.RecentFilesService;

import org.junit.Before;
import org.junit.Test;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import androidx.test.core.app.ApplicationProvider;
import static org.junit.Assert.assertArrayEquals;

public class RecentFilesServiceTest {
    static final String RECENT_FILES_FILENAME = "recent_files_filename";

    @Before
    public void setUp() {
        deleteCache();
    }

    private void deleteCache() {
        FileOutputStream fos;
        Context context = ApplicationProvider.getApplicationContext();
        ArrayList<String> recentFiles = new ArrayList<>();
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

    @Test
    public void testAddRecentFile() {
        Context context = ApplicationProvider.getApplicationContext();
        RecentFilesService recentFilesService = new RecentFilesService();
        recentFilesService.addRecentFile("url1", context);
        recentFilesService.addRecentFile("url2", context);
        recentFilesService.addRecentFile("url3", context);
        recentFilesService.addRecentFile("url4", context);

        RecentFilesService recentFilesService2 = new RecentFilesService();
        ArrayList<String> result = recentFilesService2.getLastFiles(1, context);

        assertArrayEquals(new String[]{"url3", "url2", "url1"}, result.toArray());
    }

    @Test
    public void testAddRecentFileOverflow() {
        Context context = ApplicationProvider.getApplicationContext();
        RecentFilesService recentFilesService = new RecentFilesService();
        recentFilesService.addRecentFile("url1", context);
        recentFilesService.addRecentFile("url2", context);
        recentFilesService.addRecentFile("url3", context);
        recentFilesService.addRecentFile("url4", context);
        recentFilesService.addRecentFile("url5", context);
        recentFilesService.addRecentFile("url6", context);
        recentFilesService.addRecentFile("url7", context);

        RecentFilesService recentFilesService2 = new RecentFilesService();
        ArrayList<String> result = recentFilesService2.getLastFiles(1, context);

        assertArrayEquals(new String[]{"url6", "url5", "url4", "url3", "url2"}, result.toArray());
    }

    @Test
    public void testAddRecentFileReorder() {
        Context context = ApplicationProvider.getApplicationContext();
        RecentFilesService recentFilesService = new RecentFilesService();
        recentFilesService.addRecentFile("url7", context);
        recentFilesService.addRecentFile("url6", context);
        recentFilesService.addRecentFile("url5", context);
        recentFilesService.addRecentFile("url4", context);
        recentFilesService.addRecentFile("url3", context);
        recentFilesService.addRecentFile("url2", context);
        recentFilesService.addRecentFile("url1", context);

        //add 4 again
        recentFilesService.addRecentFile("url4", context);

        RecentFilesService recentFilesService2 = new RecentFilesService();
        ArrayList<String> result = recentFilesService2.getLastFiles(1, context);

        assertArrayEquals(new String[]{"url1", "url2", "url3", "url5", "url6"}, result.toArray());
    }


    @Test
    public void testMaxStoredFiles() {
        Context context = ApplicationProvider.getApplicationContext();
        RecentFilesService recentFilesService = new RecentFilesService();
        recentFilesService.addRecentFile("url1", context);
        recentFilesService.addRecentFile("url2", context);
        recentFilesService.addRecentFile("url3", context);
        recentFilesService.addRecentFile("url4", context);
        recentFilesService.addRecentFile("url5", context);
        recentFilesService.addRecentFile("url6", context);
        recentFilesService.addRecentFile("url7", context);
        recentFilesService.addRecentFile("url8", context);
        recentFilesService.addRecentFile("url9", context);
        recentFilesService.addRecentFile("url10", context);
        recentFilesService.addRecentFile("url11", context);
        
        assertArrayEquals(
                new String[]{
                    "url2", "url3", "url4", "url5", "url6", "url7", "url8", "url9", "url10", "url11"
                },
                getStoredElements(context)
        );
    }

    private String[] getStoredElements(Context context) {
        ArrayList<String> history = new ArrayList<>();
        try {
            FileInputStream fis = context.getApplicationContext()
                    .openFileInput(RECENT_FILES_FILENAME);
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
        String[] res = new String[history.size()];
        history.toArray(res);
        return res;
    }
}
