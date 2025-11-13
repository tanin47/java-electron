Java Electron
===============================================================================

It's like Electron but for Java. Now you can build cross-platform desktop apps with Java, JavaScript, HTML, and CSS.

How to run
-----------

1. Install a JetBrains JDK that includes JCEF. Put it under `./jdk`.
2. Modify `gradle.properties` to point `org.gradle.java.home` to the JetBrains JDK. It must be a full path.
3. Run `./gradlew run`.
4. Run `./gradlew jpackage` to build the DMG installer.
4. Run `./gradlew notarize` to notarize the DMG and the app.

To-Dos
--------

- [ ] Run in Mac's App Sandbox
- [ ] Publishable to App Store
