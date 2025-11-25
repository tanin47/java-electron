Java Electron
===============================================================================

It's like Electron but for Java. Now you can build cross-platform desktop apps with Java, JavaScript, HTML, and CSS.

The example app runs in MacOS's App Sandbox, can be notarized successfully, and is ~50MB in its final size. The example app currently works with Mac
(Apple Silicon). It also publishes to TestFlight and Apple App Store successfully.

You can download the app here: https://github.com/tanin47/java-electron/releases

Other platforms are coming soon as I work toward launching the desktop version of [Backdoor](https://github.com/tanin47/backdoor) (Self-hostable single-jar database querying and editing tool for you and your team) on different platforms.

![Demo Application](demo.png)


If you have questions or are stuck, please don't hesitate to open an issue. I'm always happy to help!

Supported platforms
--------------------

| Platform              | Status         |
|-----------------------|----------------|
| MacOS (Apple Silicon) | ✅ Supported   |
| Windows               | 🟡 In Progress |
| Linux                 | 🟡 In Progress |
| MacOS (Intel)         | 🔜 Not Sure    |



How to run
-----------

1. Run `npm run hmr` in one terminal and run `./gradlew run` in another terminal. This supports hot-reloading your JS code.


How to package a DMG
---------------------

Run `./gradlew jpackage` to build the DMG installer. Then, you can extract the DMG at `./build/jpackage`.


How to notarize
----------------

You will need setup your bundle ID, certificate, and provisionprofile:

1. Open `./build.gradle.kts` and modify the variables accordingly.
2. Replace `./src/mac-resources/provisionprofile/notarization/*.provisionprofile` with your provision profile, which you can get from https://developer.apple.com/account/resources/profiles/list
3. Run `./gradlew staple` to build, notarize, and staple the DMG.


How to publish to TestFlight and App Store
--------------------------------------------

You will need setup your bundle ID, certificate, and provisionprofile:

1. Open `./build.gradle.kts` and modify the variables accordingly.
2. Replace `./src/mac-resources/provisionprofile/app_store/*.provisionprofile` with your provision profile, which you can get from https://developer.apple.com/account/resources/profiles/list
3. Run `./gradlew uploadPkgToAppStore` to upload the build to both TestFlight and App Store.


Architecture
-------------

Java Electron uses [webview](https://github.com/webview/webview) to render the UI. The communication between the UI and Java goes through HTTP. Therefore, Java Electron consists of 2 main components: 

1. The web server (powered by [Minum](https://github.com/byronka/minum)). Minum is chosen because it has no external dependencies and extremely small (350KB). It also powers Backdoor](https://github.com/tanin47/backdoor), which is the Java-based web app that I'm converting to a Desktop app. You can swap Minum with your fav web server framework!
2. The web view (powered by [webview](https://github.com/webview/webview)) that points to the web server

The web server implements 2 security mechanisms to prevent MITM:

1. The web view communicates through HTTPS using a self-signed certificate unique to each run. Since the communication is only for localhost, this is considered secure.
2. The web server generates an API key unique to each run and passes it to the web view. The web view later passes the API key back through an HTTP request in order to authenticate itself.

With the above mechanisms, no other processes on your machine will be able to access the web server nor intercept the HTTP requests.

The motivation for using HTTP is to facilitate converting a web app that already uses HTTP. 
This would reduce a lot of code changes and branches while converting [Backdoor](https://github.com/tanin47/backdoor), a self-hostable single-jar database querying and editing tool written in Java and Svelte.
Using HTTP/AJAX/Fetch is also more familiar for me and many other people.

An alternative is to use the native "bridge" for communication. You can see an example in in the [webview-java](https://github.com/webview/webview_java) repo.


How to prepare a new WebView library
-------------------------------------

Java Electron uses a custom webview library ([repo](https://github.com/tanin47/webview)) that supports a self-signed certificate for localhost.

The webview library for Mac ARM has been built and put at `./src/main/resources/webview/libwebview.dylib`. There is no need to rebuild the library.
