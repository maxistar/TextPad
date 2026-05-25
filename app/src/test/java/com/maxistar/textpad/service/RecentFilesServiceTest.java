package com.maxistar.textpad.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecentFilesServiceTest {
    @Test
    public void addingDuplicateMovesItToNewestPosition() {
        ArrayList<String> items = list("url1", "url2", "url3");

        ArrayList<String> result = RecentFilesService.addRecentFile(items, "url2");

        assertEquals(list("url1", "url3", "url2"), result);
    }

    @Test
    public void addingDuplicateLeavesOnlyOneOccurrence() {
        ArrayList<String> items = list("url1", "url2", "url3", "url2");

        ArrayList<String> result = RecentFilesService.addRecentFile(items, "url2");

        assertEquals(list("url1", "url3", "url2"), result);
    }

    @Test
    public void trimmingRemovesOnlyOldestEntries() {
        ArrayList<String> items = list(
                "url1", "url2", "url3", "url4", "url5",
                "url6", "url7", "url8", "url9", "url10"
        );

        ArrayList<String> result = RecentFilesService.addRecentFile(items, "url11");

        assertEquals(list(
                "url2", "url3", "url4", "url5", "url6",
                "url7", "url8", "url9", "url10", "url11"
        ), result);
    }

    @Test
    public void duplicateRemovalHappensBeforeTrimming() {
        ArrayList<String> items = list(
                "url1", "url2", "url3", "url4", "url5",
                "url6", "url7", "url8", "url9", "url10"
        );

        ArrayList<String> result = RecentFilesService.addRecentFile(items, "url2");

        assertEquals(list(
                "url1", "url3", "url4", "url5", "url6",
                "url7", "url8", "url9", "url10", "url2"
        ), result);
    }

    @Test
    public void lastFilesAreReturnedNewestFirst() {
        ArrayList<String> items = list("url1", "url2", "url3", "url4");

        ArrayList<String> result = RecentFilesService.getLastFiles(items, 0);

        assertEquals(list("url4", "url3", "url2", "url1"), result);
    }

    @Test
    public void lastFilesRespectSkipArgument() {
        ArrayList<String> items = list("url1", "url2", "url3", "url4");

        ArrayList<String> result = RecentFilesService.getLastFiles(items, 1);

        assertEquals(list("url3", "url2", "url1"), result);
    }

    @Test
    public void lastFilesRespectDisplayLimit() {
        ArrayList<String> items = list("url1", "url2", "url3", "url4", "url5", "url6", "url7");

        ArrayList<String> result = RecentFilesService.getLastFiles(items, 0);

        assertEquals(list("url7", "url6", "url5", "url4", "url3"), result);
    }

    private ArrayList<String> list(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }
}
