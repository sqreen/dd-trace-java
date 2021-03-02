package datadog.trace.instrumentation.akkahttp;

import akka.http.scaladsl.model.HttpRequest;
import akka.http.scaladsl.model.HttpResponse;
import datadog.trace.bootstrap.instrumentation.api.URIDataAdapter;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.HttpServerDecorator;

public class AkkaHttpServerDecorator
    extends HttpServerDecorator<HttpRequest, HttpRequest, HttpResponse> {
  public static final CharSequence AKKA_REQUEST =
      UTF8BytesString.createConstant("akka-http.request");
  public static final CharSequence AKKA_HTTP_SERVER =
      UTF8BytesString.createConstant("akka-http-server");
  public static final AkkaHttpServerDecorator DECORATE = new AkkaHttpServerDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"akka-http", "akka-http-server"};
  }

  @Override
  protected CharSequence component() {
    return AKKA_HTTP_SERVER;
  }

  @Override
  protected String method(final HttpRequest httpRequest) {
    return httpRequest.method().value();
  }

  @Override
  protected URIDataAdapter url(final HttpRequest httpRequest) {
    return new UriAdapter(httpRequest.uri());
  }

  @Override
  protected String peerHostIP(final HttpRequest httpRequest) {
    return null;
  }

  @Override
  protected int peerPort(final HttpRequest httpRequest) {
    return 0;
  }

  @Override
  protected int status(final HttpResponse httpResponse) {
    return httpResponse.status().intValue();
  }

  @Override
  protected void setStatus(HttpResponse httpResponse, int status) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void setHeader(HttpResponse httpResponse, String name, String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void writeBody(HttpResponse httpResponse, byte[] body) {
    throw new UnsupportedOperationException();
  }
}
