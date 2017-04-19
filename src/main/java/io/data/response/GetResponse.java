package io.data.response;

import io.data.Header;
import io.data.HeaderSet;
import utils.ByteUtils;

import java.util.Iterator;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class GetResponse extends Response {

    private static final int HEADER_OFFSET = 3;

    public GetResponse(byte atype) {
        responseCode = atype;
        headeroffset = HEADER_OFFSET;
        PacketLength = 3;
    }

    public GetResponse(byte[] data) {
        rawdata = data;
        headeroffset = HEADER_OFFSET;
        parseRawData(data);
    }

    public GetResponse(byte atype, HeaderSet inHeaders) {
        responseCode = atype;
        headeroffset = HEADER_OFFSET;
        PacketLength = 3;

        if (inHeaders == null)
            return;
        Iterator iter = inHeaders.iterator();
        if (iter == null)
            return;
        while (iter.hasNext()) {
            Header header = (Header)iter.next();
            if (header != null) {
                headers.add(header);
                PacketLength += header.getLength();
            }
        }
    }

    protected void fillPacketFields() {
        if (rawdata == null) {
            return;
        }
        rawdata[0] = responseCode;
        byte[] tmpBytes = ByteUtils.intToBytes(PacketLength, 2);
        ByteUtils.setBytes(rawdata, tmpBytes, 1, 2);
    }

    protected void parsePacketFields(byte[] data) {
        responseCode = data[0];
    }

    protected String packetFieldsToString() {
        String result = "GetResponse:\n";
        result += "responseCode: " + ByteUtils.byteToHexString(responseCode) + "\n";
        result += "Packet Length: " + PacketLength + "\n";
        return result;
    }
}