package utils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class IOUtils {

    public IOUtils() {}

    public static String readStandardInput() {
        StringBuilder buf = new StringBuilder();
        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
            try {
                int c = System.in.read();
                if (c != 13) {
                    if (c == 10) {
                        break;
                    } else {
                        buf.append((char) c);
                    }
                }
                if (c == -1) {
                    return null;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return buf.toString().trim();
    }

//    /**
//     *  This is a hack to ports like /dev/ttyACM0 work in RxTx.
//     * @param connPortPath the port path to add;
//     * @since 1.0
//     */
//    public static CommPortIdentifier addPortToRxTx(final String connPortPath) throws NoSuchPortException {
//        if (connPortPath.indexOf("ttyS") < 0 && connPortPath.indexOf("COM") < 0 && connPortPath.indexOf("tty.") < 0) {
//            System.setProperty("gnu.io.rxtx.SerialPorts", connPortPath);
//        }
//        return CommPortIdentifier.getPortIdentifier(connPortPath);
//    }

    public static boolean arrayContainsOK(final byte[] b) {
        for (int i = b.length - 1; i >= 3; i--) {
            if (b[i] == 13
                    && b[i - 1] == 75
                    && b[i - 2] == 79
                    && b[i - 3] == 10) {
                return true;
            }
        }
        return false;
    }

    public static boolean arrayContains(final byte[] what, final byte[] b) {
        boolean r = false;
        for (int i = b.length - 1; i > what.length; i--) {
            int j = what.length;
            while (j > -1) {
                if (b[i] != what[j]) {
                    r = false;
                    continue;
                } else {
                    r = true;
                }
            }
            if (r) {
                return r;
            }
        }
        return r;
    }

    public static String[] split(String input) {
        if (!input.contains("\"")) {
            return input.split(" ");
        }

        ArrayList<String> l = new ArrayList<String>();
        boolean isInQuotes = false;
        String stack = "";
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '\"') {
                if (isInQuotes) {
                    if (stack.length() > 0) {
                        l.add(stack);
                        stack = "";
                    }
                    isInQuotes = false;
                } else {
                    isInQuotes = true;
                }
            } else if (input.charAt(i) == ' ') {
                if (isInQuotes) {
                    stack += " ";
                } else {
                    if (stack.length() > 0) {
                        l.add(stack);
                        stack = "";
                    }
                }
            } else {
                stack += input.charAt(i);
            }
        }

        if (stack.length() > 0) {
            l.add(stack);
            stack = "";
        }

        return l.toArray(new String[l.size()]);
    }
}
