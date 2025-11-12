// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package tanin.jpsi;

import com.jetbrains.cef.JCefAppConfig;
import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Browser extends JFrame {
  private static final long serialVersionUID = -5570653778104813836L;
  private final CefApp cefApp_;
  private final CefClient client_;
  private final CefBrowser browser_;
  private final Component browerUI_;
  private boolean browserFocus_ = true;

  /**
   * To display a simple browser window, it suffices completely to create an
   * instance of the class CefBrowser and to assign its UI component to your
   * application (e.g. to your content pane).
   * But to be more verbose, this CTOR keeps an instance of each object on the
   * way to the browser UI.
   */
  Browser(String[] args, String startURL, boolean useOSR, boolean isTransparent) throws Exception {
    if (!CefApp.startup(args)) {
      throw new Exception();
    }

    JCefAppConfig config = JCefAppConfig.getInstance();
    List<String> appArgs = new ArrayList<>(Arrays.asList(args));
    appArgs.addAll(config.getAppArgsAsList());
    args = appArgs.toArray(new String[0]);

    // (1) The entry point to JCEF is always the class CefApp. There is only one
    //     instance per application and therefore you have to call the method
    //     "getInstance()" instead of a CTOR.
    //
    //     CefApp is responsible for the global CEF context. It loads all
    //     required native libraries, initializes CEF accordingly, starts a
    //     background task to handle CEF's message loop and takes care of
    //     shutting down CEF after disposing it.
    CefApp.addAppHandler(new CefAppHandlerAdapter(args) {
      @Override
      public void stateHasChanged(org.cef.CefApp.CefAppState state) {
        // Shutdown the app if the native CEF part is terminated
        if (state == CefAppState.TERMINATED) System.exit(0);
      }
    });
    CefSettings settings = config.getCefSettings();
    settings.windowless_rendering_enabled = useOSR;
    cefApp_ = CefApp.getInstance(settings);

    // (2) JCEF can handle one to many browser instances simultaneous. These
    //     browser instances are logically grouped together by an instance of
    //     the class CefClient. In your application you can create one to many
    //     instances of CefClient with one to many CefBrowser instances per
    //     client. To get an instance of CefClient you have to use the method
    //     "createClient()" of your CefApp instance. Calling an CTOR of
    //     CefClient is not supported.
    //
    //     CefClient is a connector to all possible events which come from the
    //     CefBrowser instances. Those events could be simple things like the
    //     change of the browser title or more complex ones like context menu
    //     events. By assigning handlers to CefClient you can control the
    //     behavior of the browser. See tests.detailed.MainFrame for an example
    //     of how to use these handlers.
    client_ = cefApp_.createClient();

    // (3) One CefBrowser instance is responsible to control what you'll see on
    //     the UI component of the instance. It can be displayed off-screen
    //     rendered or windowed rendered. To get an instance of CefBrowser you
    //     have to call the method "createBrowser()" of your CefClient
    //     instances.
    //
    //     CefBrowser has methods like "goBack()", "goForward()", "loadURL()",
    //     and many more which are used to control the behavior of the displayed
    //     content. The UI is held within a UI-Compontent which can be accessed
    //     by calling the method "getUIComponent()" on the instance of CefBrowser.
    //     The UI component is inherited from a java.awt.Component and therefore
    //     it can be embedded into any AWT UI.
    browser_ = client_.createBrowser(startURL, useOSR, isTransparent);
    browerUI_ = browser_.getUIComponent();


    // Clear focus from the address field when the browser gains focus.
    client_.addFocusHandler(new CefFocusHandlerAdapter() {
      @Override
      public void onGotFocus(CefBrowser browser) {
        if (browserFocus_) return;
        browserFocus_ = true;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        browser.setFocus(true);
      }

      @Override
      public void onTakeFocus(CefBrowser browser, boolean next) {
        browserFocus_ = false;
      }
    });

    // (5) All UI components are assigned to the default content pane of this
    //     JFrame and afterwards the frame is made visible to the user.
    getContentPane().add(browerUI_, BorderLayout.CENTER);
    pack();
    setSize(800, 600);
    setVisible(true);

    // (6) To take care of shutting down CEF accordingly, it's important to call
    //     the method "dispose()" of the CefApp instance if the Java
    //     application will be closed. Otherwise you'll get asserts from CEF.
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        CefApp.getInstance().dispose();
        dispose();
      }
    });
  }
}
