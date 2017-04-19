package utils;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public abstract class Log {

    public static int LOG_NONE = 0;
    public static int LOG_INFO = 1;
    public static int LOG_DEBUG = 2;
    public static int logLevel = LOG_INFO;

    public static void info(String msg) {
        info(msg, null);
    }

    public static void info(String msg, Throwable t) {
        if (logLevel >= LOG_INFO) {
            System.out.print(msg);
            if (t != null) {
                if (t.getMessage() != null) {
                    System.out.print(t.getMessage());
                } else if (t.getLocalizedMessage() != null){
                    System.out.print(t.getLocalizedMessage());
                }else{
                    t.printStackTrace();
                }
            }
        }
        System.out.println();
    }

    public static void debug(Class<?> klass, String msg) {
        debug(klass, msg, null);
    }

    public static void debug(Class<?> klass, String msg, Throwable t) {
        if (logLevel >= LOG_DEBUG) {
            System.out.println("[" + klass.getName() + "]" + msg);
            if (t != null) {
                t.printStackTrace();
            }
        }
    }

}
