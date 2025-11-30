package tanin.javaelectron;

import sun.misc.Signal;
import tanin.ejwf.SelfSignedCertificate;
import tanin.javaelectron.nativeinterface.MacOsApi;

import java.util.logging.Level;
import java.util.logging.Logger;

import static tanin.javaelectron.nativeinterface.WebviewNative.N;

public class Browser {
  public static interface JsInvoker {
    void invoke(String js);
  }
  
  private static final Logger logger = Logger.getLogger(Browser.class.getName());

  String url;
  boolean isDebug;
  private long pointer;
  SelfSignedCertificate cert;

  public Browser(String url, boolean isDebug) {
    this.url = url;
    this.isDebug = isDebug;
  }

  public void run() throws InterruptedException {
    MacOsApi.N.setupMenu();

    pointer = N.webview_create(isDebug, null);
    N.webview_navigate(pointer, url);

    Signal.handle(new Signal("INT"), sig -> terminate());
    Runtime.getRuntime().addShutdownHook(new Thread(this::terminate));

    MacOsApi.N.nsWindowMakeKeyAndOrderFront();
    N.webview_run(pointer);
    if (this.pointer != 0) {
      N.webview_destroy(this.pointer);
      this.pointer = 0;
    }
  }

  public void eval(String js) {
    N.webview_eval(pointer, js);
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
}
