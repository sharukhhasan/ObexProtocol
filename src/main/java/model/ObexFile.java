package model;

import io.data.Header;
import io.data.HeaderSet;
import utils.ByteUtils;
import utils.FormatUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public final class ObexFile extends ObexObject {

    private byte[] size = null;
    private InputStream inputStream;

    public ObexFile(final ObexFolder parentFolder, final String filename) {
        super(parentFolder, filename);
    }

    public ObexFile(final String filename) {
        super(ROOT_FOLDER, filename);
    }

    public HeaderSet getHeaderSet() {
        HeaderSet hs = new HeaderSet();

        Header nameheader = new Header(Header.NAME);
        nameheader.setValue(getBinaryName());
        hs.add(nameheader);

        Header lenght = new Header(Header.LENGTH);
        lenght.setValue(getSize());
        hs.add(lenght);

        Header time = new Header(Header.TIME);
        time.setValue(FormatUtils.getTime(getTime()).getBytes());
        hs.add(time);

        return hs;
    }

    @Override
    protected void setContents(final byte[] contents) throws IOException {
        setSize(contents.length);
        super.setContents(contents);
    }

    public byte[] getSize() {
        if (size == null) {
            setSize(getContents().length);
        }
        return size;
    }

    public void setSize(final int size) {
        this.size = ByteUtils.intToBytes(size, 4);
    }

    public InputStream getInputStream() {
        if (inputStream == null) {
            inputStream = new ByteArrayInputStream(this.getContents());
        }
        return inputStream;
    }

    public void setInputStream(final InputStream inputStream) {
        try {
            if (inputStream != null) {
                setSize(inputStream.available());
            }
        } catch (IOException ex) {
            //TODO: Log errors.
        }
        this.inputStream = inputStream;
    }

    @Override
    public String getSizeString() {
        return ByteUtils.humanReadableByteCount(ByteUtils.bytesToInt(getSize()), true);
    }

    @Override
    public boolean equals(final Object compobj) {
        if (super.equals(compobj) && compobj instanceof ObexFile) {
            ObexFile file = (ObexFile) compobj;
            return (file.size == this.size);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + Arrays.hashCode(this.size);
        return hash;
    }

    @Override
    protected void threatHeader(final Header header) {
        switch (header.getId()) {
            case Header.LENGTH:
                this.size = header.getValue();
                break;
            case Header.NAME:
                setName(header.getValue());
                break;
            case Header.BODY:
                try {
                    setContents(header.getValue());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                break;
            case Header.END_OF_BODY:
                try {
                    setContents(header.getValue());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                setReady();
                break;
            default:
                System.out.println("strange header: " + ByteUtils.dumpBytes(header.toBytes()));
        }
    }

    @Override
    protected void onReady() {
        this.setSize(getContents().length);
    }
}
