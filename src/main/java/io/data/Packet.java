package io.data;

import java.util.Arrays;
import java.util.Iterator;

import utils.ByteUtils;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public abstract class Packet {

    public static final int MAXPACKETLENGTH = 64 * 1024 - 1;

    protected int PacketLength = 0;
    protected HeaderSet<Header> headers = new HeaderSet<Header>();
    protected byte[] rawdata = null;

    // Offset of the optional headers if there is any
    protected int headeroffset;

    public void setPacketLength(final int length) {
        PacketLength = length;
        if (rawdata != null) {
            byte[] pl = ByteUtils.intToBytes(PacketLength, 2);
            rawdata[1] = pl[0];
            rawdata[2] = pl[1];
        }
    }

    public int getPacketLength() {
        return PacketLength;
    }

    public void addHeader(final Header header) {
        headers.add(header);
        PacketLength += header.getLength();
    }

    public void removeHeaders() {
        if (headers != null) {
            headers.clear();
        }
    }

    public Iterator<Header> getHeaders() {
        if (headers != null) {
            return headers.iterator();
        }
        return null;
    }

    public void removeHeader(final Header header) {
        headers.remove(header);
        PacketLength -= header.getLength();
    }

    public byte[] getHeaderValue(final byte headerType) {
        return headers.getHeaderValue(headerType);
    }

    public abstract byte getType();

    public abstract void setType(byte type);

    public byte[] toBytes() {
        if (rawdata == null) {
            rawdata = new byte[PacketLength];

            fillPacketFields();
        }

        int index = headeroffset;
        byte[] tmpBytes;
        if (headers != null) {
            // Be sure ConnectionID header always be the first header
            Header connectionID = headers.getHeader(Header.CONNECTION_ID);
            if (connectionID != null) {
                tmpBytes = connectionID.toBytes();
                for (int i = 0; i < 5; i++) {
                    rawdata[index++] = tmpBytes[i];
                }
            }

            Iterator iter = headers.iterator();
            while (iter != null && iter.hasNext()) {
                tmpBytes = ((Header) iter.next()).toBytes();
                /* skip the ConnectionID header */
                if (tmpBytes[0] == Header.CONNECTION_ID) {
                    continue;
                }

                for (int i = 0; i < tmpBytes.length; i++) {
                    rawdata[index++] = tmpBytes[i];
                }
            }
        }
        return rawdata;
    }

    protected abstract void fillPacketFields();

    protected void parseRawData(final byte[] data) {
        int offset = headeroffset;
        byte[] tmpArray = null;

        // Get actual length instead of parsing from the raw data
        PacketLength = data.length;
        parsePacketFields(ByteUtils.getBytes(data, 0, headeroffset));

        while (offset < PacketLength) {
            int headerLength = Header.getHeaderLength(data[offset]);
            if (headerLength == -1) {
                tmpArray = ByteUtils.getBytes(data, offset + 1, 2);
                headerLength = ByteUtils.bytesToInt(tmpArray);
            }

            Header header = new Header(ByteUtils.getBytes(data, offset, headerLength));
            headers.add(header);
            offset += headerLength;
        }
    }

    protected abstract void parsePacketFields(final byte[] data);

    protected int getHeadersLength() {
        if (headers == null) {
            return 0;
        }
        Iterator iter = headers.iterator();
        if (iter == null) {
            return 0;
        }

        int length = 0;
        while (iter.hasNext()) {
            length += ((Header) iter.next()).getLength();
        }
        return length;
    }

    public String toString() {
        String result = "*************************************************\n";
        result += packetFieldsToString();
        result += "\n";
        if (headers != null) {
            result += "Headers:\n";
            result += headers.toString();
        }

        result += "*************************************************\n";
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Packet)) {
            return false;
        }

        Packet pkt = (Packet) obj;
        if (PacketLength != pkt.getPacketLength()) {
            return false;
        }

        if (headers == null) {
            return (pkt.getHeaders() == null);
        }

        Iterator thisIter = headers.iterator();
        Iterator pktIter = pkt.getHeaders();

        if (thisIter == null && pktIter == null) {
            return true;
        }
        if (thisIter == null && pktIter != null
                || thisIter != null && pktIter == null) {
            return false;
        }

        while (thisIter.hasNext()) {
            if (!pktIter.hasNext()) {
                // In the case pktIter has fewer elements than thisIter
                return false;
            }
            Header thisHeader = (Header) thisIter.next();
            Header pktHeader = (Header) pktIter.next();
            if (!thisHeader.equals(pktHeader)) {
                return false;
            }
        }

        if (pktIter.hasNext()) {
            // In the case pktIer has more elements than thisIter
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Arrays.hashCode(this.rawdata);
        return hash;
    }

    protected abstract String packetFieldsToString();
}
