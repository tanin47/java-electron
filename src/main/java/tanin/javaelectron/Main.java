package tanin.javaelectron;

import tanin.ejwf.MinumBuilder;
import tanin.ejwf.SelfSignedCertificate;
import tanin.javaelectron.nativeinterface.Base;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
  private static final Logger logger = Logger.getLogger(Main.class.getName());

  static {
    try (var configFile = Main.class.getResourceAsStream("/logging.properties")) {
      LogManager.getLogManager().readConfiguration(configFile);
      logger.info("The log config (logging.properties) has been loaded.");
    } catch (IOException e) {
      logger.warning("Could not load the log config file (logging.properties): " + e.getMessage());
    }

    // Setting up the native lib path must be the first thing we do.
    logger.info("Native lib path: " + Base.nativeDir.getAbsolutePath());
  }

  public static void main(String[] args) throws Exception {
    var cert = SelfSignedCertificate.generate("localhost");
    logger.info("The SSL cert is randomly generated on each run:");
    logger.info("  Certificate SHA-256 Fingerprint: " + SelfSignedCertificate.getSHA256Fingerprint(cert.cert().getEncoded()));

    var authKey = SelfSignedCertificate.generateRandomString(32);
    var main = new Server(cert, authKey);
    logger.info("Starting...");
    main.start();

    var sslPort = main.minum.getSslServer().getPort();

    var browser = new Browser("https://localhost:" + sslPort + "?authKey=" + authKey, MinumBuilder.IS_LOCAL_DEV);
    browser.run();

    logger.info("Exiting");
  }
}
