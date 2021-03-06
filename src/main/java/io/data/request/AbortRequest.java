package io.data.request;

import io.data.Header;
import io.data.HeaderSet;
import utils.ByteUtils;

import java.util.Iterator;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class AbortRequest extends Request {

    private static final int HEADER_OFFSET = 3;

    public AbortRequest() {
        opCode = Request.ABORT;
        headerOffset = HEADER_OFFSET;
        packetLength = 3;
    }

    public AbortRequest(byte[] data) {
        opCode = Request.ABORT;
        headerOffset = HEADER_OFFSET;
        rawData = data;
        parseRawData(data);
    }

    public AbortRequest(HeaderSet inHeaders) {
        opCode = Request.ABORT;
        headerOffset = HEADER_OFFSET;
        packetLength = 3;

        if (inHeaders == null) {
            return;
        }

        Iterator iter = inHeaders.iterator();
        while (iter != null && iter.hasNext()) {
            Header header = (Header)iter.next();
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
        String result = "ConnectRequest:\n";
        result += "opcode: " + ByteUtils.byteToHexString(opCode) + "\n";
        result += "Packet Length: " + packetLength + "\n";
        return result;
    }
}
