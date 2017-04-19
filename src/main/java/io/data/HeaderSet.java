package io.data;

import java.util.ArrayList;
import java.util.Iterator;

import utils.ByteUtils;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public final class HeaderSet<T> extends ArrayList<Header> {
    private byte[] rawData;

    public HeaderSet() {
        super();
    }

    public HeaderSet(final byte[] bytes) {
        rawData = bytes;
        parseHeaders(rawData);
    }

    protected void parseHeaders(final byte[] data) {
        int bytesLeft = data.length;
        int offset = 0;
        int headerLength = 0;

        while (bytesLeft != 0) {
            byte tmpByte = data[offset];

            if (Header.hasLengthField(tmpByte)) {
                headerLength = ByteUtils.bytesToInt(ByteUtils.getBytes(data, offset + 1, 2));
            } else {
                if ((tmpByte & 0xC0) == 0xC0) {
                    headerLength = 2;
                } else if ((tmpByte & 0x80) == 0x80){
                    headerLength = 5;
                }
            }

            Header tmpHeader = new Header(ByteUtils.getBytes(data, offset, headerLength));
            this.add(tmpHeader);
            offset += headerLength;
            bytesLeft -= headerLength;
        }
    }

    public byte[] getHeaderValue(final byte id) {
        Header header = getHeader(id);

        if (header == null) {
            return null;
        } else {
            return header.getValue();
        }
    }

    public Header getHeader(final byte id) {
        Iterator iter = this.iterator();

        if (iter == null) {
            return null;
        }

        while (iter.hasNext()) {
            Header tmpHeader = (Header)iter.next();
            if (tmpHeader.getId() == id) {
                return tmpHeader;
            }
        }

        return null;
    }

    public int getTotalLength() {
        int length = 0;
        Iterator iter = this.iterator();

        while (iter != null && iter.hasNext()) {
            Header header = (Header)iter.next();
            length += header.getLength();
        }

        return length;
    }

    @Override
    public String toString() {
        String result = "";
        Iterator iter = this.iterator();

        while (iter != null && iter.hasNext()) {
            result += iter.next().toString();
            result += "\n";
        }

        return result;
    }
}
