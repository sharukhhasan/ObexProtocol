package io.data.response;

import io.data.Packet;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public abstract class Response extends Packet {

    public static final byte SUCCESS = (byte) 0x20;
    public static final byte CONTINUE = (byte) 0x10;
    public static final byte CREATED = (byte) 0x21;
    public static final byte BADREQUEST = (byte) 0x40;
    public static final byte FINAL = (byte) 0x80;

    protected byte responseCode;

    public byte getType() {
        return responseCode;
    }

    public void setType(final byte atype) {
        responseCode = atype;
    }

    public void setFinal() {
        responseCode |= FINAL;
    }

    public boolean isFinal() {
        return ((responseCode & FINAL) == FINAL);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (obj instanceof Response) {
            Response resp = (Response) obj;
            return responseCode == resp.getType();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 41 * hash + this.responseCode;
        return hash;
    }
}
