package datadog.trace.bootstrap.instrumentation.security;

import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class BlockPage {

  private static final String PAGE = "attack_detected.html";

  public static final byte[] body;
  public static final String bodyLen;

  static {
    body = loadBody() ;
    bodyLen = Integer.toString(body.length);
  }

  private BlockPage() {
    // hide default constructor
  }

  private static byte[] loadBody() {
    try (InputStream is = ClassLoader.getSystemClassLoader().getSystemResourceAsStream("inst/" + PAGE)) {
      if (is != null) {
        DataInputStream dis = new DataInputStream(is);

        // copy to memory is required to get the content length
        // which can't be sent after sending the response body
        byte[] resp = new byte[dis.available()];
        dis.readFully(resp);
        dis.close();
        return resp;
      }
    } catch (IOException e) {
      log.warn("Error reading error page.", e);
    }
    log.error("unable to load embedded blocked page.");
    return "Sorry, you're been blocked".getBytes();
  }
}
