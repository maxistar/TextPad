# Simple Text Editor for Android

This is very simple text editor that can help you with editing simple text files. 

The application is made for fun and supported by its author at his free time. If some of features are missing
the reason of it either the author did not have enough time to implement it or nobody helped him to do so.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.maxistar.textpad/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.maxistar.textpad)

This editor can be useful for editing of small texts, write down small notes etc...  At the moment application supports only plain text files.

### Folder workspaces

Open **Folders** from the editor menu or swipe from the start edge, then choose **Add folder**. Android asks which folder TextPad may access. Added folders remain available after restarting the application while Android keeps the persisted permission.

Folders are loaded only when expanded. TextPad opens provider files with a `text/*` MIME type internally and also recognizes `.txt`, `.md`, `.markdown`, `.json`, `.js`, `.mjs`, `.cjs`, `.log`, and `.srt` when provider metadata is incomplete. Other files remain visible and are opened by a compatible installed application.

Long-press a root to refresh it, choose it again after access is revoked, or remove it from the list. Removing a root does not delete files. The first release does not provide workspace search, tabs, file creation, rename, move, delete, Git integration, or persisted expansion state.

The code is open so anyone can review code, send pull requests, new features, translations and so on. 

Github repo: https://github.com/maxistar/TextPad.

Website: https://simpleditor.org

Any suggestions, pull requests, translations for this project are welcomed. Thank you!

### Issues Tracker

https://github.com/maxistar/TextPad/issues

### How to compile and run an application 

In order to compile the application locally you will need to install [Android Studio](https://developer.android.com/studio)
Follow the documentation of how to open and compile your [first android application](https://developer.android.com/training/basics/firstapp).
Once your setup is done, opening and compiling the existing project should not cause any difficulties.

in pipeline an application uses [maxistar/android](https://github.com/maxistar/android-docker-image) docker image

### How to run sniffer

./gradlew lint

### How to translate application

You can use [this service](https://crowdin.com/project/simple-text-editor) to add a new translation or suggest better one

### To Do

many many things to do.
