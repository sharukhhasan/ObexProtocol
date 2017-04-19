package io.data.response;

import io.data.Header;
import io.data.HeaderSet;
import io.data.Packet;
import utils.ByteUtils;

import java.util.Iterator;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class ConnectResponse extends Response {

    public static final byte DEFAULT_VERSION_NUMBER = 0x13;

    private static final int HEADER_OFFSET = 7;

    protected byte versionNumber;
    protected byte flags = (byte)0;
    protected int maxPacketLength;

    public ConnectResponse(byte atype) {
        responseCode = atype;
        headerOffset = HEADER_OFFSET;
        versionNumber = DEFAULT_VERSION_NUMBER;
        maxPacketLength = Packet.MAXPACKETLENGTH;
        packetLength = 7;
    }

    public ConnectResponse(byte[] data) {
        rawData = data;
        headerOffset = HEADER_OFFSET;
        parseRawData(rawData);
    }

    public ConnectResponse(byte atype, HeaderSet inHeaders) {
        responseCode = atype;
        headerOffset = HEADER_OFFSET;
        versionNumber = DEFAULT_VERSION_NUMBER;
        maxPacketLength = Packet.MAXPACKETLENGTH;
        packetLength = 7;

        if (inHeaders == null)
            return;

        Iterator iter = inHeaders.iterator();
        if (iter == null)
            return;

        while (iter.hasNext()) {
            Header header = (Header)iter.next();
            if (header != null) {
                headers.add(header);
                packetLength += header.getLength();
            }
        }
    }

    public void setVersionNumber(byte number) {
        versionNumber = number;
    }

    public byte getVersionNumber() {
        return versionNumber;
    }

    public void setMaxPacketLength(int length) {
        maxPacketLength = length;
    }

    public int getMaxPacketLength() {
        return maxPacketLength;
    }

    protected void fillPacketFields() {
        if (rawData == null) {
            return;
        }
        rawData[0] = responseCode;
        byte[] tmpBytes = ByteUtils.intToBytes(packetLength, 2);
        ByteUtils.setBytes(rawData, tmpBytes, 1, 2);
        rawData[3] = versionNumber;
        rawData[4] = flags;
        tmpBytes = ByteUtils.intToBytes(maxPacketLength, 2);
        ByteUtils.setBytes(rawData, tmpBytes, 5, 2);
    }

    protected void parsePacketFields(byte[] data) {
        responseCode = data[0];
        versionNumber = data[3];
        flags = data[4];
        byte[] tmpArray = ByteUtils.getBytes(data, 5, 2);
        maxPacketLength = ByteUtils.bytesToInt(tmpArray);
    }

    protected String packetFieldsToString() {
        String result = "ConnectResponse:\n";
        result += "responseCode: " + ByteUtils.byteToHexString(responseCode) + "\n";
        result += "Packet Length: " + packetLength + "\n";
        result += "OBEX Version Number: " + ByteUtils.byteToHexString(versionNumber) + "\n";
        result += "Flags: " + ByteUtils.byteToHexString(flags) + "\n";
        result += "Max OBEX Packet Length: " + maxPacketLength + "\n";
        return result;
    }
}
