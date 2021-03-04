package datadog.trace.bootstrap.security;

public abstract class Engine {
  public static Engine INSTANCE;

  public abstract void deliverNotifications();
}
