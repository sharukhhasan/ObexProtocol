package io.data.response;

import io.data.Header;
import io.data.HeaderSet;
import utils.ByteUtils;

import java.util.Iterator;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class AbortResponse extends Response {

    private static final int HEADER_OFFSET = 3;

    public AbortResponse(byte atype) {
        responseCode = atype;
        headerOffset = HEADER_OFFSET;
        packetLength = 3;
    }

    public AbortResponse(byte[] data) {
        rawData = data;
        headerOffset = HEADER_OFFSET;
        parseRawData(data);
    }

    public AbortResponse(byte atype, HeaderSet inHeaders) {
        responseCode = atype;
        headerOffset = HEADER_OFFSET;
        packetLength = 3;

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

    public void fillPacketFields() {
        if (rawData == null) {
            return;
        }
        rawData[0] = responseCode;
        byte[] tmpBytes = ByteUtils.intToBytes(packetLength, 2);
        ByteUtils.setBytes(rawData, tmpBytes, 1, 2);
    }

    protected void parsePacketFields(byte[] data) {
        responseCode = data[0];
    }

    protected String packetFieldsToString() {
        String result = "AbortResponse:\n";
        result += "respcode: " + ByteUtils.byteToHexString(responseCode) + "\n";
        result += "Packet Length: " + packetLength + "\n";
        return result;
    }

}
