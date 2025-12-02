package tanin.javaelectron;

import com.eclipsesource.json.Json;
import com.renomad.minum.web.FullSystem;
import com.renomad.minum.web.Response;
import com.renomad.minum.web.StatusLine;
import tanin.ejwf.MinumBuilder;
import tanin.ejwf.SelfSignedCertificate;
import tanin.javaelectron.nativeinterface.MacOsApi;
import tanin.javaelectron.nativeinterface.WebviewNative;
import tanin.javaelectron.nativeinterface.WindowsApi;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.renomad.minum.web.RequestLine.Method.GET;
import static com.renomad.minum.web.RequestLine.Method.POST;

public class Server {
  private static final Logger logger = Logger.getLogger(Server.class.getName());

  public FullSystem minum;
  SelfSignedCertificate cert;
  String authKey;
  public Browser browser;

  public Server(SelfSignedCertificate cert, String authKey) {
    this.cert = cert;
    this.authKey = authKey;
  }

  public static final String AUTH_KEY_COOKIE_KEY = "Auth";

  private Browser.OnFileSelected onFileSelected = null;

  public void start() throws CertificateException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, IOException {
    var keyStorePassword = SelfSignedCertificate.generateRandomString(64);
    var keyStoreFile = SelfSignedCertificate.generateKeyStoreFile(cert, keyStorePassword);
    logger.info("Generated keystore file: " + keyStoreFile);
    minum = MinumBuilder.build(keyStoreFile.getAbsolutePath(), keyStorePassword);
    var wf = minum.getWebFramework();

    logger.info("Registering Minum...");


    wf.registerPreHandler((inputs) -> {
      var request = inputs.clientRequest();
      var authKeyFromQueryString = request.getRequestLine().getPathDetails().getQueryString().get("authKey");
      var authKeyFromCookie = Optional.ofNullable(extractCookieByKey(AUTH_KEY_COOKIE_KEY, request.getHeaders().valueByKey("Cookie")))
        .map(v -> v.substring((AUTH_KEY_COOKIE_KEY + "=").length())).orElse(null);

      if (this.authKey.equals(authKeyFromQueryString) || this.authKey.equals(authKeyFromCookie)) {
        // ok
      } else {
        logger.info("The auth key is invalid. Got: " + authKeyFromQueryString + " and " + authKeyFromCookie);
        return Response.buildResponse(
          StatusLine.StatusCode.CODE_401_UNAUTHORIZED,
          Map.of("Content-Type", "text/plain"),
          "The auth key is invalid."
        );
      }

      try {
        logger.info(request.getRequestLine().getMethod() + " " + request.getRequestLine().getPathDetails().getIsolatedPath());
        var response = inputs.endpoint().apply(inputs.clientRequest());
        logger.info(request.getRequestLine().getMethod() + " " + request.getRequestLine().getPathDetails().getIsolatedPath() + " " + response.getStatusCode());
        return response;
      } catch (Exception e) {
        return Response.buildResponse(
          StatusLine.StatusCode.CODE_400_BAD_REQUEST,
          Map.of("Content-Type", "application/json"),
          Json.object()
            .add("errors", Json.array(e.getMessage()))
            .toString()
        );
      } catch (Throwable e) {
        logger.log(Level.SEVERE, request.getRequestLine().getMethod() + " " + request.getRequestLine().getPathDetails().getIsolatedPath() + " raised an error.", e);
        throw e;
      }
    });

    wf.registerPath(
      GET,
      "",
      r -> {
        String content = new String(Server.class.getResourceAsStream("/html/index.html").readAllBytes());
        return Response.htmlOk(
          content,
          Map.of(
            "Set-Cookie", AUTH_KEY_COOKIE_KEY + "=" + this.authKey + "; Max-Age=86400; Path=/; Secure; HttpOnly"
          ));
      }
    );

    AtomicInteger counter = new AtomicInteger();

    wf.registerPath(
      POST,
      "ask-java",
      req -> {
        var json = Json.parse(req.getBody().asString());
        var msg = json.asObject().get("msg").asString();
        System.out.println("Javascripts said: " + msg);
        Thread.sleep(2000);
        return Response.buildResponse(
          StatusLine.StatusCode.CODE_200_OK,
          Map.of("Content-Type", "application/json"),
          Json.object()
            .add("response", "Hello from Java (" + counter.getAndIncrement() + ")")
            .toString()
        );
      }
    );

    // We cannot use webview_bind due to the synchronous nature of it. The callback has to be blocked.
    // However, if the callback is blocked, then the file dialog which needs to run on the main thread wouldn't show.
    wf.registerPath(
      POST,
      "open-file",
      req -> {
        onFileSelected = filePath -> {
          try {
            String content = Files.readString(Path.of(filePath));
            browser.eval("window.triggerFileContentRead(" + Json.object().add("content", content).toString()  + ")");
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        };
        browser.openFileDialog(false, onFileSelected);

        return Response.buildResponse(
          StatusLine.StatusCode.CODE_200_OK,
          Map.of("Content-Type", "application/json"),
          Json.object().toString()
        );
      }
    );

    wf.registerPath(
      POST,
      "save-file",
      req -> {

        onFileSelected = filePath -> {
          System.out.println("Saving file: " + filePath);
          try {
            String randomContent = "Random content generated at: " + java.time.LocalDateTime.now();
            Files.writeString(Path.of(filePath), randomContent);
            System.out.println("Successfully wrote content to file: " + filePath);

            browser.eval("window.triggerFileSaved(" + Json.object().add("filePath", filePath).toString()  + ")");
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        };
        browser.openFileDialog(true, onFileSelected);

        return Response.buildResponse(
          StatusLine.StatusCode.CODE_200_OK,
          Map.of("Content-Type", "application/json"),
          Json.object().toString()
        );
      }
    );

    logger.info("Finishing registering...");
  }

  public void stop() {
    if (minum != null) {
      minum.shutdown();
    }
  }

  private String extractCookieByKey(String cookieKey, List<String> cookies) {
    if (cookies == null) {
      return null;
    }
    return Arrays.stream(cookies.getFirst().split(";"))
      .filter(s -> s.trim().startsWith(cookieKey + "="))
      .findFirst()
      .map(String::trim)
      .orElse(null);
  }
}
