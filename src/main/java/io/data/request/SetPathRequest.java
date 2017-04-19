package io.data.request;

import io.data.Header;
import io.data.HeaderSet;
import io.data.Packet;
import utils.ByteUtils;

import java.util.Iterator;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class SetPathRequest extends Request {

    private static final int HEADER_OFFSET = 5;

    protected byte flags = (byte) 0;
    protected int MaxPacketLength = Packet.MAXPACKETLENGTH;

    public SetPathRequest() {
        opCode = Request.SETPATH ;
        headerOffset = HEADER_OFFSET;
        packetLength = 5;
    }

    public SetPathRequest(byte[] data) {
        opCode = Request.SETPATH ;
        headerOffset = HEADER_OFFSET;
        rawData = data;
        parseRawData(rawData);
    }

    public SetPathRequest(HeaderSet inHeaders) {
        opCode = Request.SETPATH ;
        headerOffset = HEADER_OFFSET;
        packetLength = 5;

        if (inHeaders == null) {
            return;
        }

        Iterator headerIter = inHeaders.iterator();
        if (headerIter == null) {
            return;
        }

        while (headerIter.hasNext()) {
            Header header = (Header) headerIter.next();
            if (header != null) {
                headers.add(header);
                packetLength += header.getLength();
            }
        }
    }

    public void setFlags(byte inflags) {
        flags = inflags;
    }

    public byte getFlags() {
        return flags;
    }

    public void setMaxPacketLenght(int length) {
        MaxPacketLength = length;
    }

    public int getMaxPacketLength() {
        return MaxPacketLength;
    }

    protected void fillPacketFields() {
        if (rawData == null) {
            return;
        }

        rawData[0] = opCode;
        byte[] tmpBytes = ByteUtils.intToBytes(packetLength, 2);
        ByteUtils.setBytes(rawData, tmpBytes, 1, 2);
        rawData[3] = flags;
        tmpBytes = ByteUtils.intToBytes(MaxPacketLength, 2);
        ByteUtils.setBytes(rawData, tmpBytes, 5, 2);
    }

    protected void parsePacketFields(byte[] data) {
        opCode = data[0];
        flags = data[3];

        byte[] tmpArray = ByteUtils.getBytes(data, 5, 2);
        MaxPacketLength = ByteUtils.bytesToInt(tmpArray);
    }

    public String packetFieldsToString() {
        String result = "ConnectRequest:\n";
        result += "opcode: " + ByteUtils.byteToHexString(opCode) + "\n";
        result += "Packet Length: " + packetLength + "\n";
        result += "Flags: " + ByteUtils.byteToHexString(flags) + "\n";
        result += "Max OBEX Packet Length: " + MaxPacketLength + "\n";
        return result;
    }
}
