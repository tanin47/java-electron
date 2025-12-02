package tanin.javaelectron;

import com.eclipsesource.json.Json;
import sun.misc.Signal;
import tanin.ejwf.SelfSignedCertificate;
import tanin.javaelectron.nativeinterface.Base;
import tanin.javaelectron.nativeinterface.MacOsApi;
import tanin.javaelectron.nativeinterface.WebviewNative;
import tanin.javaelectron.nativeinterface.WindowsApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tanin.javaelectron.nativeinterface.WebviewNative.N;

public class Browser {
  public static interface OnFileSelected {
    void invoke(String path);
  }

  private static final Logger logger = Logger.getLogger(Browser.class.getName());

  String url;
  boolean isDebug;
  private long pointer;
  SelfSignedCertificate cert;

  private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

  private MacOsApi.OnFileSelected onFileSelected = null;

  public Browser(String url, boolean isDebug) {
    this.url = url;
    this.isDebug = isDebug;
  }

  public void run() throws InterruptedException {
    if (Base.CURRENT_OS == Base.OperatingSystem.MAC) {
      MacOsApi.N.setupMenu();
    }

    pointer = N.webview_create(isDebug, null);
    N.webview_navigate(pointer, url);

    Signal.handle(new Signal("INT"), sig -> terminate());
    Runtime.getRuntime().addShutdownHook(new Thread(this::terminate));

    if (Base.CURRENT_OS == Base.OperatingSystem.MAC) {
      MacOsApi.N.nsWindowMakeKeyAndOrderFront();
    }
    N.webview_run(pointer);
    if (this.pointer != 0) {
      N.webview_destroy(this.pointer);
      this.pointer = 0;
    }
  }

  public long getWindowPointer() {
    return N.webview_get_window(pointer);
  }

  public void eval(String js) {
    N.webview_dispatch(pointer, ($pointer, arg) -> N.webview_eval(pointer, js), 0);
  }

  private void terminate() {
    logger.info("Received a shutdown hook. Terminating the webview...");
    try {
      if (this.pointer != 0) {
        N.webview_terminate(this.pointer);
        this.pointer = 0;
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error while terminating webview", e);
    }
  }

  void openFileDialog(boolean isSaved, OnFileSelected fileSelected) {
    if (Base.CURRENT_OS == Base.OperatingSystem.WINDOWS) {
      var thread = new Thread(() -> {
        var pointer = WindowsApi.N.openFileDialog(getWindowPointer(), isSaved);

        if (pointer == null) {
          logger.info("No file has been selected");
        } else {
          try {
            String filePath = pointer.getString(0);
            fileSelected.invoke(filePath);
          } finally {
            WindowsApi.N.freeString(pointer);
          }
        }
      });
      thread.start();
    } else if (Base.CURRENT_OS == Base.OperatingSystem.MAC) {
        onFileSelected = filePath -> {
          System.out.println("Opening file: " + filePath);

          MacOsApi.N.startAccessingSecurityScopedResource(filePath);
          try {
            fileSelected.invoke(filePath);
          } finally {
            MacOsApi.N.stopAccessingSecurityScopedResource(filePath);
          }
          onFileSelected = null;
        };

        if (isSaved) {
          MacOsApi.N.saveFile(onFileSelected);
        } else {
          MacOsApi.N.openFile(onFileSelected);
        }
    } else {
      throw new RuntimeException("Unsupported OS: " + OS_NAME);
    }
  }
}
