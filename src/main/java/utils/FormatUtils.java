package utils;

import io.data.response.Response;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class FormatUtils {

    private static final DateFormat OBEX_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    public static String getTime(final Date time) {
        return OBEX_DATE_FORMAT.format(time);
    }

    public static boolean threatResponse(final Response res) {
        boolean b = false;
        if (res != null) {
            switch (res.getType() & 0x7F) {
                case Response.CONTINUE:
                    b = true;
                    break;
                case Response.SUCCESS:
                    b = true;
                    break;
                case Response.BADREQUEST:
                    b = false;
                    break;
                case Response.CREATED:
                    b = true;
                    break;
                default:
                    b = false;
            }
        }
        return b;
    }

    public static byte[] buildPerm(final boolean read, final boolean write, final boolean delete, final byte type) {
        byte[] prem;
        int i = 4 + (read ? 1 : 0) + (write ? 1 : 0) + (delete ? 1 : 0);
        int j = i;
        prem = new byte[i];
        prem[--i] = '\"';

        if (delete) {
            prem[--i] = 'D';
        }

        if (write) {
            prem[--i] = 'W';
        }

        if (read) {
            prem[--i] = 'R';
        }

        prem[--i] = '\"';
        prem[--i] = (byte) (j - 2);
        prem[--i] = type;
        return prem;
    }

    public static String bytesToName(final byte[] name) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length; i++) {
            if (name[i] != 0) {
                builder.append((char) name[i]);
            }
        }
        return builder.toString();
    }

    public static byte[] buildPerm(final String userPerm, final String groupPerm) {
        byte[] prem;
        int i = 6 + userPerm.length() + groupPerm.length();
        int j = i;
        prem = new byte[i];
        prem[--i] = '\"';

        for (int k = groupPerm.length() - 1; k > -1; k--) {
            prem[--i] = (byte) groupPerm.charAt(k);
        }

        prem[--i] = '\"';
        prem[--i] = '\"';

        for (int k = userPerm.length() - 1; k > -1; k--) {
            prem[--i] = (byte) userPerm.charAt(k);
        }

        prem[--i] = '\"';
        prem[--i] = (byte) (j - 2);
        prem[--i] = 0x38;
        return prem;
    }

    public static String bytesToPerm(byte[] perm) {
        StringBuilder s = new StringBuilder();

        for (int i = 3; i < perm.length; i++) {
            byte b = perm[i];
            if (b != '\"') {
                s.append((char) b);
            }
        }
        
        return s.toString();
    }
    private static final DateFormat format = SimpleDateFormat.getInstance();

    public static String dateFormat(final Date date) {
        return format.format(date);
    }

    public static StringBuilder listingFormat(StringBuilder builder, String filename, String size, Date date) {
        int l = builder.length();
        builder.append(filename).append("                    ").setLength(l + 20);
        builder.append(size).append("           ").setLength(l + 30);

        if (date != null) {
            builder.append(FormatUtils.dateFormat(date));
        }

        builder.append('\n');
        return builder;
    }

    public static StringBuilder listingFormat(StringBuilder builder, String filename, String size, Date date, String perm1, String perm2) {
        int l = builder.length();
        builder.append(filename).append("                    ").setLength(l = l + 20);
        builder.append(size).append("           ").setLength(l = l + 10);
        builder.append(perm1).append("    ").setLength(l = l + 4);
        builder.append(perm2).append("    ").setLength(l = l + 4);

        if (date != null) {
            builder.append(FormatUtils.dateFormat(date));
        }

        builder.append('\n');
        return builder;
    }

    public static Date getTime(final String value) {
        Calendar c = GregorianCalendar.getInstance();
        int year = Integer.parseInt(value.substring(0, 4));
        int month = Integer.parseInt(value.substring(4, 6));
        int date = Integer.parseInt(value.substring(6, 8));
        int hrs = Integer.parseInt(value.substring(9, 11));
        int min = Integer.parseInt(value.substring(11, 13));
        int sec = Integer.parseInt(value.substring(13, 15));
        c.set(year, month - 1, date, hrs, min, sec);
        return c.getTime();
    }
}
