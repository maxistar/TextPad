package com.maxistar.textpad;

import android.content.Context;

import com.maxistar.textpad.service.AlternativeUrlsService;
import com.maxistar.textpad.service.RecentFilesService;

public class ServiceLocator {
    private static ServiceLocator instance = null;

    private ServiceLocator() {}

    private SettingsService settingsService;

    private RecentFilesService recentFilesService;

    private AlternativeUrlsService alternativeUrlsService;

    public static ServiceLocator getInstance() {
        if (instance == null) {
            synchronized(ServiceLocator.class) {
                instance = new ServiceLocator();
            }
        }
        return instance;
    }

    public SettingsService getSettingsService(Context context) {
        if (settingsService == null) {
            settingsService = new SettingsService(context);
        }
        return settingsService;
    }

    public RecentFilesService getRecentFilesService() {
        if (recentFilesService == null) {
            recentFilesService = new RecentFilesService();
        }
        return recentFilesService;
    }

    public AlternativeUrlsService getAlternativeUrlsService() {
        if (alternativeUrlsService == null) {
            alternativeUrlsService = new AlternativeUrlsService();
        }
        return alternativeUrlsService;
    }

}
