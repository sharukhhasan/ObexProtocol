package utils;

import io.data.Header;
import io.data.response.GetResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class ByteUtils {

    public static byte[] intToBytes(final int integer, final int bLength) {
        if (integer > Integer.MAX_VALUE || integer < Integer.MIN_VALUE) {
            return null;
        }
        byte[] bytes = new byte[bLength];
        for (int i = 0; i < bLength; i++) {
            bytes[i] = new Integer(integer >> (bLength - 1 - i) * 8).byteValue();
        }
        return bytes;
    }

    public static int bytesToInt(final byte[] bytes) {
        int result = 0;
        if (bytes == null) {
            return 0;
        }
        for (int i = 0; i < bytes.length; i++) {
            int temp = (int) bytes[i];
            if (temp < 0) {
                temp = 0x100 + temp;
            }
            result += temp << (8 * (bytes.length - 1 - i));
        }

        return result;
    }

    public static long bytesToLong(final byte[] bytes) {
        long result = 0;

        for (int i = 0; i < bytes.length; i++) {
            int temp = (int) bytes[i];
            if (temp < 0) {
                temp = 0x100 + temp;
            }
            result += temp << (8 * (bytes.length - 1 - i));
        }

        return result;
    }

    public static byte[] getBytes(final byte[] data, int offset, final int length) {
        if (offset + length > data.length) {
            return null;
        }

        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = data[offset++];
        }
        return result;
    }

    public static void setBytes(final byte[] dest, final byte[] data, final int offset,
                                int length) {
        if (offset + length > dest.length) {
            return;
        }
        for (int i = 0; i < length; i++) {
            dest[offset + i] = data[i];
        }
    }

    public static String byteToHexString(final byte inbyte) {
        String result = "0x";
        if (inbyte <= Byte.MAX_VALUE && inbyte >= 0) {
            if (inbyte < 16) {
                result += "0";
            }
            result += Integer.toHexString((int) inbyte);
        } else {
            result += Integer.toHexString(0x100 + inbyte);
        }
        return result;
    }

    static byte[] byteArrayListToBytes(final ArrayList list) {
        if (list == null) {
            return null;
        }
        Iterator iter = list.iterator();
        if (iter == null) {
            return null;
        }
        int length = 0;
        while (iter.hasNext()) {
            Object element = iter.next();
            if (!ByteArray.class.isInstance(element)) {
                return null;
            }
            ByteArray ba = (ByteArray) iter.next();
            length += ba.length();
        }
        if (length == 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        iter = list.iterator();
        int offset = 0;
        while (iter.hasNext()) {
            ByteArray ba = (ByteArray) iter.next();
            setBytes(bytes, ba.getBytes(), offset, ba.length());
            offset += ba.length();
        }
        return bytes;
    }

    public static String dumpBytes(final byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            result += byteToHexString(bytes[i]).replace("0x", "");
            if (i == bytes.length - 1) {
                break;
            }
            result += ", ";
            if (i % 5 == 4) {
                result += "\n";
            }
        }
        return result;
    }

    public static boolean compareBytes(final byte[] a, final byte[] b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static GetResponse bytesToGetResponse(final byte[] incomingData) {
        GetResponse response = new GetResponse(incomingData[0]);
        for (int i = 3; i < incomingData.length; i++) {
            Header header = new Header(incomingData[i]);
            //switch type of headers;
            int hLen;
            switch (header.getId()) {
                case Header.CONNECTION_ID:
                case Header.LENGTH:
                    header.setValue(getBytes(incomingData, ++i, 4));
                    i += 3;
                    break;
                case Header.TYPE:
                case Header.NAME:
                case Header.END_OF_BODY:
                case Header.BODY:
                    hLen = bytesToInt(getBytes(incomingData, ++i, 2)) - 3;
                    i++;
                    header.setValue(getBytes(incomingData, ++i, hLen));
                    i += hLen - 1;
                    break;
                case Header.DESCRIPTION:
                case Header.HTTP:
                    continue; //TODO: Threat this headers!;
            }
            response.addHeader(header);
        }
        return response;
    }

    public static byte[] nameToBytes(final String name) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < name.length(); i++) {
            bos.write(0);
            bos.write((int) name.charAt(i));
        }
        byte[] b = bos.toByteArray();
        bos.reset();
        bos = null;
        return b;
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static byte[] prepareMoveByteArray(String oldPath, String newPath) throws IOException {
        byte[] newPathB = ByteUtils.nameToBytes(newPath), oldPathB = ByteUtils.nameToBytes(oldPath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(oldPathB.length + newPathB.length + 10);
        baos.write(new byte[]{0x34, 0x04, 0x6D, 0x6F, 0x76, 0x65});
        baos.write(0x35);
        baos.write(oldPathB.length);
        baos.write(oldPathB);
        baos.write(0x36);
        baos.write(newPathB.length);
        baos.write(newPathB);
        byte[] b = baos.toByteArray();
        baos.close();
        return b;
    }
}

// Object wrapper for byte array
class ByteArray {

    private byte[] bytes = null;

    public ByteArray(final byte[] inbytes) {
        bytes = inbytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int length() {
        if (bytes == null) {
            return 0;
        } else {
            return bytes.length;
        }
    }
}