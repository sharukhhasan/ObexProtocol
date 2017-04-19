package io.data.request;

import io.data.Header;
import io.data.HeaderSet;
import utils.ByteUtils;

import java.util.Iterator;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class PutRequest extends Request {

    private static final int HEADEROFFSET = 3;

    public PutRequest() {
        opCode = Request.PUT;
        headerOffset = HEADEROFFSET;
        packetLength = 3;
    }

    public PutRequest(byte[] data) {
        opCode = Request.PUT;
        headerOffset = HEADEROFFSET;
        rawData = data;
        parseRawData(data);
    }

    public PutRequest(HeaderSet inHeaders) {
        opCode = Request.PUT;
        headerOffset = HEADEROFFSET;
        packetLength = 3;

        if (inHeaders == null) {
            return;
        }

        Iterator iter = inHeaders.iterator();
        while (iter != null && iter.hasNext()) {
            Header header = (Header) iter.next();
            if (header != null) {
                headers.add(header);
                packetLength += header.getLength();
            }
        }
    }

    protected void fillPacketFields() {
        if (rawData == null) {
            return;
        }

        rawData[0] = opCode;
        byte[] tmpBytes = ByteUtils.intToBytes(packetLength, 2);
        ByteUtils.setBytes(rawData, tmpBytes, 1, 2);
    }

    protected void parsePacketFields(byte[] data) {
        opCode = data[0];
    }

    protected String packetFieldsToString() {
        String result = "PutRequest:\n";
        result += "opcode: " + ByteUtils.byteToHexString(opCode) + "\n";
        result += "Packet Length: " + packetLength + "\n";
        return result;
    }
}
