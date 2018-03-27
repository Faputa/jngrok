package ngrok.log;

public interface Logger
{
	void log(String fmt, Object... args);
	void err(String fmt, Object... args);
}
