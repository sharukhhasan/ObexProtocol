package model;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public enum ObexDevice {

    TC65(new byte[]{'A', 'T', '^', 'S', 'Q', 'W', 'E', '=', '0', '\r'},
            new byte[]{'A', 'T', '^', 'S', 'Q', 'W', 'E', '=', '3', '\r'},
            new byte[]{(byte) 0x6B, (byte) 0x01, (byte) 0xCB, (byte) 0x31, (byte) 0x41, (byte) 0x06, (byte) 0x11, (byte) 0xD4, (byte) 0x9A, (byte) 0x77, (byte) 0x00, (byte) 0x50, (byte) 0xDA, (byte) 0x3F, (byte) 0x47, (byte) 0x1f},
            new byte[]{'x', '-', 'o', 'b', 'e', 'x', '/', 'f', 'o', 'l', 'd', 'e', 'r', '-', 'l', 'i', 's', 't', 'i', 'n', 'g'}, "a:"),

    DEFAULT(new byte[]{'A', 'T', '^', 'S', 'Q', 'W', 'E', '=', '0', '\r'},
            new byte[]{'A', 'T', '^', 'S', 'Q', 'W', 'E', '=', '3', '\r'},
            new byte[]{(byte) 0x6B, (byte) 0x01, (byte) 0xCB, (byte) 0x31, (byte) 0x41, (byte) 0x06, (byte) 0x11, (byte) 0xD4, (byte) 0x9A, (byte) 0x77, (byte) 0x00, (byte) 0x50, (byte) 0xDA, (byte) 0x3F, (byte) 0x47, (byte) 0x1f},
            new byte[]{'x', '-', 'o', 'b', 'e', 'x', '/', 'f', 'o', 'l', 'd', 'e', 'r', '-', 'l', 'i', 's', 't', 'i', 'n', 'g'}, "a:");

    public static final byte[] CMD_CHECK = new byte[]{'A', 'T', '\r'};
    public static final byte[] ECHO_OFF = new byte[]{'A', 'T', 'E', '\r'};
    public static final byte[] ECHO_ON = new byte[]{'A', 'T', 'E', '1', '\r'};
    public static final byte[] TEST_DEVICE = new byte[]{'A', 'T', 'I', '\r'};
    public static final byte[] UNDO = new byte[]{'A', 'T', 'Z', '\r'};

    private final byte[] obexCheck, obexOpen, fsUuid, lsName;
    private final String rootFolder;


    private ObexDevice(byte[] obexCheck, byte[] obexOpen, byte[] fsUuid, byte[] lsName, String rootFolder) {
        this.obexCheck = obexCheck;
        this.obexOpen = obexOpen;
        this.fsUuid = fsUuid;
        this.lsName = lsName;
        this.rootFolder = rootFolder;
    }


    public byte[] getFlowControl(final byte flowControlMode) {
        return new byte[]{'A', 'T', '\\', 'Q', flowControlMode, '\r'};
    }

    public byte[] getObexCheck() {
        return obexCheck;
    }

    public byte[] getObexOpen() {
        return obexOpen;
    }

    public byte[] getFsUuid() {
        return fsUuid;
    }

    public byte[] getLsName() {
        return lsName;
    }

    public String getRootFolder() {
        return rootFolder;
    }
}
