package model;

import io.data.Header;
import io.data.response.Response;
import utils.ByteUtils;
import utils.FormatUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public abstract class ObexObject {

    public static final ObexFolder ROOT_FOLDER = new ObexFolder(null, "a:");
    private final ByteArrayOutputStream contents = new ByteArrayOutputStream(600);
    private final ObexFolder parentFolder; //parent folder cannot be changed.
    private byte[] binaryName = {};
    String groupPerm = "", userPerm = "";
    private Date modified = null, time = null;
    private boolean endOfBody = false;
    private String path = null;
    protected String name = "";

    public String getPath() {
        if (path == null) {
            if (getParentFolder() == null) {
                path = "";
            } else {
                path = getParentFolder().getPath() + "/";
            }
        }
        return path + name;
    }

    public ObexObject(final ObexFolder parentFolder, final String name) {
        this.parentFolder = parentFolder;
        setName(name);
    }

    public void addResponse(final Response res) {
        if (res != null) {
            for (Iterator<Header> it = res.getHeaders(); it.hasNext();) {
                threatHeader(it.next());
            }
        }
    }

    public void addResponses(final Response[] res) {
        reset();
        for (int i = 0; i < res.length; i++) {
            addResponse(res[i]);
        }
    }

    protected void onReady() {
    }

    protected void onReset() {
    }

    protected abstract void threatHeader(final Header header);

    public byte[] getBinaryName() {
        return binaryName;
    }

    public String getName() {
        return name;
    }

    public boolean isReady() {
        return endOfBody;
    }

    public final void setName(final String name) {
        this.name = name;
        this.binaryName = ByteUtils.nameToBytes(name);
    }

    public final void setName(final byte[] name) {
        this.binaryName = name;
        this.name = FormatUtils.bytesToName(name);
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(final Date modified) {
        this.modified = modified;
    }

    public String getUserPerm() {
        return userPerm;
    }

    public void setUserPerm(final boolean read, final boolean write, final boolean delete) {
        this.userPerm = (read ? "R" : "") + (write ? "W" : "") + (delete ? "D" : "");
    }

    public void setUserPerm(final String value) {
        if (value == null) {
            return;
        }
        this.userPerm = value;
    }

    public String getGroupPerm() {
        return groupPerm;
    }

    public void setGroupPerm(final boolean read, final boolean write, final boolean delete) {
        this.groupPerm = (read ? "R" : "") + (write ? "W" : "") + (delete ? "D" : "");
    }

    public void setGroupPerm(final String value) {
        if (value == null) {
            return;
        }
        this.groupPerm = value;
    }

    public byte[] getContents() {
        return contents.toByteArray();
    }

    public final void reset() {
        this.contents.reset();
        onReset();
        endOfBody = false;
    }

    protected void setContents(final byte[] contents) throws IOException {
        if (endOfBody) {
            reset();
        }
        appendContent(contents);
    }

    private void appendContent(final byte[] content) throws IOException {
        this.contents.write(content);
    }

    public Date getTime() {
        if (time == null) {
            time = new Date();
        }
        return time;
    }

    public void setTime(final Date time) {
        this.time = time;
    }

    public void setTime(final String value) {
        if (value == null) {
            return;
        }
        this.time = FormatUtils.getTime(value);
    }

    public ObexFolder getParentFolder() {
        return parentFolder;
    }

    public final void setReady() {
        this.endOfBody = true;
        onReady();
    }

    @Override
    public boolean equals(final Object compobj) {
        if (compobj instanceof ObexObject) {
            ObexObject obj = (ObexObject) compobj;
            return (this.modified == obj.modified) && (this.time == obj.time) && ByteUtils.compareBytes(this.binaryName, obj.binaryName);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        FormatUtils.listingFormat(builder, name, getSizeString(), getTime(), getUserPerm(), getGroupPerm());
        return builder.toString();
    }

    public abstract String getSizeString();

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Arrays.hashCode(this.binaryName);
        hash = 29 * hash + (this.modified != null ? this.modified.hashCode() : 0);
        hash = 29 * hash + (this.time != null ? this.time.hashCode() : 0);
        return hash;
    }
}
