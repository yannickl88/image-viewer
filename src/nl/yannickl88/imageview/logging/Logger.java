package nl.yannickl88.imageview.logging;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Logger which uses imageviewer.log for logging.
 */
public class Logger {
    /**
     * Log a critical error
     */
    public static void log(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

        log(e.getMessage() + "\n" + sw.toString());
    }

    /**
     * Log a message
     */
    public static void log(String string) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("imageviewer.log", true));

            writer.newLine();   //Add new line
            writer.write((new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS")).format(Calendar.getInstance().getTime()));
            writer.write(" " + string);
            writer.close();
        } catch (IOException ioException) {
        }
    }
}
