package ngrok.log;

public abstract class Logger {

    // 如果没有指定日志，则使用默认日志
    private static Logger _logger = new LoggerImpl();

    public static synchronized void setLogger(Logger logger) {
        _logger = logger;
    }

    public static synchronized Logger getLogger() {
        return _logger;
    }

    public abstract void info(String fmt, Object... args);

    public abstract void err(String fmt, Object... args);
}
