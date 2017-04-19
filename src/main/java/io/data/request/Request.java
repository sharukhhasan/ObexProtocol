package io.data.request;

import io.data.Packet;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public abstract class Request extends Packet {

    public static final byte CONNECT = (byte) 0x80;
    public static final byte DISCONNECT = (byte) 0x81;
    public static final byte PUT = (byte) 0x02;
    public static final byte GET = (byte) 0x03;
    public static final byte SETPATH = (byte) 0x85;
    public static final byte SETPATH2 = (byte) 0x86;
    public static final byte SESSION = (byte) 0x87;
    public static final byte ABORT = (byte) 0xFF;
    public static final byte FINAL = (byte) 0x80;

    protected byte opCode;

    public void setType(final byte type) {
        opCode = type;
    }

    public byte getType() {
        return opCode;
    }

    public void setFinal() {
        opCode |= FINAL;
    }

    public boolean isFinal() {
        return ((opCode & FINAL) == FINAL);
    }

    @Override
    public byte[] getHeaderValue(final byte headerType) {
        return headers.getHeaderValue(headerType);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (obj instanceof Request) {

            Request req = (Request) obj;

            return opCode == req.getType();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 43 * hash + this.opCode;
        return hash;
    }
}