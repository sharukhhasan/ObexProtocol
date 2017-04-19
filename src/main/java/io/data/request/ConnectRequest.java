package io.data.request;

import io.data.Header;
import io.data.HeaderSet;
import io.data.Packet;
import utils.ByteUtils;

import java.util.Iterator;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class ConnectRequest extends Request {

    // Default OBEX Version Number 1.3
    public static final byte DEFAULT_VERSION_NUMBER = 0x13;

    private static final int HEADER_OFFSET = 7;

    protected byte versionNumber;
    protected byte flags = (byte)0;
    protected int maxPacketLength = Packet.MAXPACKETLENGTH;

    public ConnectRequest() {
        opCode = Request.CONNECT;
        headerOffset = HEADER_OFFSET;
        packetLength = 7;
        versionNumber = DEFAULT_VERSION_NUMBER;
    }

    public ConnectRequest(byte[] data) {
        opCode = Request.CONNECT;
        headerOffset = HEADER_OFFSET;
        rawData = data;
        parseRawData(rawData);
    }

    public ConnectRequest(HeaderSet inHeaders) {
        opCode = Request.CONNECT;
        headerOffset = HEADER_OFFSET;
        versionNumber = DEFAULT_VERSION_NUMBER;
        packetLength = 7;

        if (inHeaders == null) {
            return;
        }

        Iterator headerIter = inHeaders.iterator();
        if (headerIter == null) {
            return;
        }

        while (headerIter.hasNext()) {
            Header header = (Header)headerIter.next();
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

    public void setFlags(byte inflags) {
        flags = inflags;
    }

    public byte getFlags() {
        return flags;
    }

    public void setMaxPacketLenght(int length) {
        maxPacketLength = length;
    }

    public int getMaxPacketLength() {
        return maxPacketLength;
    }

    protected void fillPacketFields() {
        if (rawData == null) {
            return;
        }

        rawData[0] = opCode;
        byte[] tmpBytes = ByteUtils.intToBytes(packetLength, 2);
        ByteUtils.setBytes(rawData, tmpBytes, 1, 2);
        rawData[3] = versionNumber;
        rawData[4] = flags;
        tmpBytes = ByteUtils.intToBytes(maxPacketLength, 2);
        ByteUtils.setBytes(rawData, tmpBytes, 5, 2);
    }

    protected void parsePacketFields(byte[] data) {
        opCode = data[0];
        versionNumber = data[3];
        flags = data[4];
        byte[] tmpArray = ByteUtils.getBytes(data, 5, 2);
        maxPacketLength = ByteUtils.bytesToInt(tmpArray);
    }


    public String packetFieldsToString() {
        String result = "ConnectRequest:\n";
        result += "opcode: " + ByteUtils.byteToHexString(opCode) + "\n";
        result += "Packet Length: " + packetLength + "\n";
        result += "OBEX Version Number: " + ByteUtils.byteToHexString(versionNumber) + "\n";
        result += "Flags: " + ByteUtils.byteToHexString(flags) + "\n";
        result += "Max OBEX Packet Length: " + maxPacketLength + "\n";
        return result;
    }
}