package tanin.javaelectron;

import com.eclipsesource.json.Json;
import com.renomad.minum.web.FullSystem;
import com.renomad.minum.web.Response;
import com.renomad.minum.web.StatusLine;
import tanin.ejwf.MinumBuilder;
import tanin.ejwf.SelfSignedCertificate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static com.renomad.minum.web.RequestLine.Method.GET;
import static com.renomad.minum.web.RequestLine.Method.POST;

public class Main {
  private static final Logger logger = Logger.getLogger(Main.class.getName());

  static {
    try (var configFile = Main.class.getResourceAsStream("/logging.properties")) {
      LogManager.getLogManager().readConfiguration(configFile);
      logger.info("The log config (logging.properties) has been loaded.");
    } catch (IOException e) {
      logger.warning("Could not load the log config file (logging.properties): " + e.getMessage());
    }
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
