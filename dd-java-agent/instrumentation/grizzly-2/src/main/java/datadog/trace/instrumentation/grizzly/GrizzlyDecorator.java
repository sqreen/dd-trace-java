package datadog.trace.instrumentation.grizzly;

import datadog.trace.bootstrap.instrumentation.api.URIDataAdapter;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.HttpServerDecorator;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.IOException;
import java.io.OutputStream;

@Slf4j
public class GrizzlyDecorator extends HttpServerDecorator<Request, Request, Response> {
  public static final CharSequence GRIZZLY = UTF8BytesString.createConstant("grizzly");
  public static final CharSequence GRIZZLY_REQUEST =
      UTF8BytesString.createConstant("grizzly.request");
  public static final GrizzlyDecorator DECORATE = new GrizzlyDecorator();

  @Override
  protected String method(final Request request) {
    return request.getMethod().getMethodString();
  }

  @Override
  protected URIDataAdapter url(final Request request) {
    return new RequestURIDataAdapter(request);
  }

  @Override
  protected String peerHostIP(final Request request) {
    return request.getRemoteAddr();
  }

  @Override
  protected int peerPort(final Request request) {
    return request.getRemotePort();
  }

  @Override
  protected int status(final Response containerResponse) {
    return containerResponse.getStatus();
  }

  @Override
  protected void setStatus(Response response, int status) {
    response.setStatus(status);
  }

  @Override
  protected void setHeader(Response response, String name, String value) {
    response.setHeader(name, value);
  }

  @Override
  protected void writeBody(Response response, byte[] body) {
    try (OutputStream os = response.getOutputStream()) {
      os.write(body);
    } catch (IOException e) {
      log.error("Error writing response body");
    }
  }

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"grizzly"};
  }

  @Override
  protected CharSequence component() {
    return GRIZZLY;
  }
}
