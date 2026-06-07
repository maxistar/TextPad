package com.maxistar.textpad;

import android.content.Context;

import com.maxistar.textpad.service.AlternativeUrlsService;
import com.maxistar.textpad.service.RecentFilesService;
import com.maxistar.textpad.service.SettingsService;
import com.maxistar.textpad.service.ThemeService;
import com.maxistar.textpad.service.WakeLockService;
import com.maxistar.textpad.service.WorkspaceFolderService;
import com.maxistar.textpad.workspace.SharedPreferencesWorkspaceRegistryStore;
import com.maxistar.textpad.workspace.WorkspaceRegistryCodec;

public class ServiceLocator {
    private static ServiceLocator instance = null;

    private ServiceLocator() {}

    private SettingsService settingsService;

    private RecentFilesService recentFilesService;

    private AlternativeUrlsService alternativeUrlsService;

    private ThemeService themeService;

    private WakeLockService wakeLockService = null;
    private WorkspaceFolderService workspaceFolderService;

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
            settingsService = new SettingsService();
            settingsService.loadSettings(context);
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

    public ThemeService getThemeService(Context context) {
        if (themeService == null) {
            themeService = new ThemeService(getSettingsService(context));
        }
        return themeService;
    }

    public WakeLockService getWakeLockService() {
        if (wakeLockService == null) {
            wakeLockService = new WakeLockService();
        }
        return wakeLockService;
    }

    public WorkspaceFolderService getWorkspaceFolderService(Context context) {
        if (workspaceFolderService == null) {
            workspaceFolderService = new WorkspaceFolderService(
                    new SharedPreferencesWorkspaceRegistryStore(context),
                    new WorkspaceRegistryCodec()
            );
        }
        return workspaceFolderService;
    }


}
