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

This editor can be useful for editing small text files and writing short notes.

### Syntax highlighting

Optional syntax highlighting is available for JSON, Markdown, and JavaScript. It is disabled by default and can be enabled in **Settings > Appearance > Syntax highlighting**.

The editor uses a file's extension in **Auto** mode. The editor menu also lets you select Plain text, JSON, Markdown, or JavaScript for the current document. A manual selection lasts until another document is opened or a new document is created.

Highlighting is lexical and does not validate syntax. JavaScript regular-expression literals are left unstyled, and expressions inside template-string interpolation are not parsed separately. Documents larger than 256,000 characters remain plain text to keep editing responsive.

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
