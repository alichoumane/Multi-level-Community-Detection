package iotools;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.*;

import javax.swing.JTextArea;

/**
 * Used by all programs accross the platform to perform logging. This class provides flexibility in output, it allows the output to
 * be redirected to the ui whenever needed, while still printed on System.out, and written to a file. The logger also provides multi
 * level logging. It is recommended to start with {@link Level#FINER}.
 *
 */
public class CustomLogger{
	private Logger logger;
	private StreamHandler consoleHandler;
	public static String logFolder = null;
	private static FileHandler fileHandler;
	
	public static Formatter getCustomFormatter() {
		Formatter formatter = new Formatter() {
			@Override
			public String format(LogRecord logRecord) {
				String message="";
				if(logRecord.getLevel().intValue()>Level.INFO.intValue()) {
					message = logRecord.getLevel().getName()+" "+logRecord.getSourceClassName()+": ";
				}
				message+=logRecord.getMessage();
				return message;
			}
		};
		return formatter;
	}
	public CustomLogger(String name, Level initialLevel) {
		logger = Logger.getLogger(name);
		logger.setLevel(initialLevel);
		logger.setUseParentHandlers(false);
		
		Formatter formatter = getCustomFormatter();
		//add console handler
		consoleHandler = addHandler(System.out, formatter);
		consoleHandler.setLevel(initialLevel);
		
		//add file handler
		if(logFolder!=null && fileHandler==null) {
			try {
				fileHandler = new FileHandler(logFolder+".log", false);
				fileHandler.setLevel(Level.ALL);
				logger.addHandler(fileHandler);
			} catch (SecurityException e) {
				logger.log(Level.WARNING, "unable to write log file, "+e.getMessage()+"\n");
			} catch (IOException e) {
				logger.log(Level.ALL, "unable to write log file, "+e.getMessage()+"\n");
			}
		}
	}
	
	public void setFileHandler(String file) {
		//add file handler
		try {
			fileHandler = new FileHandler(file, false);
			fileHandler.setLevel(Level.ALL);
			logger.addHandler(fileHandler);
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "unable to write log file, "+e.getMessage()+"\n");
		} catch (IOException e) {
			logger.log(Level.ALL, "unable to write log file, "+e.getMessage()+"\n");
		}
	}
	public void setLevel(Level level) {
		logger.setLevel(level);
	}
	public void setConsoleLevel(Level level) {
		consoleHandler.setLevel(level);
	}
	public void log(Level level, String msg) {
		logger.log(level, msg);
	}
	public Handler[] getHandlers() {
		return logger.getHandlers();
	}
	public void removeHandler(Handler handler) {
		logger.removeHandler(handler);
	}
	public void addHandler(Handler handler) {
		logger.addHandler(handler);
	}
	public StreamHandler addHandler(PrintStream stream, Formatter formatter) {
		StreamHandler handler = new StreamHandler(stream, formatter){
			@Override
			public synchronized void publish(LogRecord record) {
				super.publish(record);
				flush();
			}
			@Override
			public void close() throws SecurityException{
				flush();
			}
		};
		addHandler(handler);
		return handler;
	}
	public Handler addHandler(JTextArea textArea, Formatter formatter) {
		return addHandler(new PrintStream(new OutputStream() {
			
			@Override
			public void write(int arg) throws IOException {
				textArea.append(String.valueOf((char)arg));
				textArea.setCaretPosition(textArea.getDocument().getLength());
			}
		}), formatter);
	}
}
