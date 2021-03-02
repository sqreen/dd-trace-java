package datadog.trace.instrumentation.finatra;

import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import datadog.trace.bootstrap.instrumentation.api.DefaultURIDataAdapter;
import datadog.trace.bootstrap.instrumentation.api.URIDataAdapter;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.HttpServerDecorator;
import java.net.URI;

public class FinatraDecorator extends HttpServerDecorator<Request, Request, Response> {

  public static final CharSequence FINATRA = UTF8BytesString.createConstant("finatra");
  public static final CharSequence FINATRA_CONTROLLER =
      UTF8BytesString.createConstant("finatra.controller");
  public static final CharSequence FINATRA_REQUEST =
      UTF8BytesString.createConstant("finatra.request");
  public static final FinatraDecorator DECORATE = new FinatraDecorator();

  @Override
  protected CharSequence component() {
    return FINATRA;
  }

  @Override
  protected String method(final Request request) {
    return request.method().name();
  }

  @Override
  protected URIDataAdapter url(final Request request) {
    return new DefaultURIDataAdapter(URI.create(request.uri()));
  }

  @Override
  protected String peerHostIP(final Request request) {
    return request.remoteAddress().getHostAddress();
  }

  @Override
  protected int peerPort(final Request request) {
    return request.remotePort();
  }

  @Override
  protected int status(final Response response) {
    return response.statusCode();
  }

  @Override
  protected void setStatus(Response response, int status) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void setHeader(Response response, String name, String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void writeBody(Response response, byte[] body) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"finatra"};
  }
}
