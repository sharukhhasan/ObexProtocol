package io;

import event.ConnectionModeListener;
import event.DataEventListener;
import io.data.Header;
import io.data.Packet;
import io.data.request.*;
import io.data.response.*;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import model.*;
import utils.ByteUtils;
import utils.FormatUtils;
import utils.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class ObexClient {

    public final static String version = "2.4 beta";
    private final static int TIMEOUT = 30000;
    private final static Logger logger = Logger.getLogger("com.lhf.jobexftp");;
    private final ObexEventListener eventListener = new ObexEventListener();
    private final OBEXData incomingData = new OBEXData();
    private final Object holder = new Object();
    private ObexFolder currentFolder = ObexObject.ROOT_FOLDER;
    private ObexDevice device = ObexDevice.DEFAULT;
    private int maxPacketLenght = 600;
    private boolean hasIncomingPacket;
    private boolean connected;
    private final ATConnection conn;

    public ObexClient(final ATConnection conn) {
        this.conn = conn;
    }

    public boolean connect() throws IOException, SerialPortException, SerialPortTimeoutException {
        if (connected) {
            return connected;
        }
        this.device = conn.getDevice();
        logger.log(Level.FINEST, "Connecting");
        connected = false;
        if (conn.getConnMode() != ATConnection.MODE_DATA) {
            conn.setConnMode(ATConnection.MODE_DATA);
        }

        this.conn.addConnectionModeListener(eventListener);
        this.conn.addDataEventListener(eventListener);
        currentFolder = ObexObject.ROOT_FOLDER;
        synchronized (this) {
            getCurrentFolder().setName(device.getRootFolder());
            Request req = new ConnectRequest();
            Header target = new Header(Header.TARGET);
            target.setValue(device.getFsUuid());
            req.addHeader(target);
            sendPacketAndWait(req, 500);
            if (hasIncomingPacket) {
                ConnectResponse response = new ConnectResponse(incomingData.pullData());
                setMaxPacketLenght(response.getMaxPacketLength());
                connected = FormatUtils.threatResponse(response);
                if (connected) {
                    logger.log(Level.FINE, "Connection established");
                }
            }
            return connected;
        }
    }

    public boolean disconnect() throws IOException, SerialPortException {

        if (!connected) {
            return !connected;
        }
        synchronized (this) {
            logger.log(Level.FINEST, "Disconnecting");
            sendPacketAndWait(new DisconnectRequest(), 500);
            if (hasIncomingPacket) {
                DisconnectResponse response = new DisconnectResponse(incomingData.pullData());
                connected = !FormatUtils.threatResponse(response);
                if (!connected) {
                    logger.log(Level.FINE, "Disconnected");
                    this.conn.removeDataEventListener(eventListener);
                    this.conn.removeConnectionModeListener(eventListener);
                }
            }
            return !connected;
        }
    }

    public boolean abort() throws IOException, SerialPortException {
        logger.log(Level.FINER, "Aborting");
        boolean r = false;
        AbortRequest req = new AbortRequest();
        sendPacketAndWait(req, 1000);
        if (hasIncomingPacket) {
            AbortResponse response = new AbortResponse(incomingData.pullData());
            r = FormatUtils.threatResponse(response);
        }
        return r;
    }

    public boolean eraseDisk() throws IOException, SerialPortException {
        synchronized (this) {
            logger.log(Level.FINEST, "Erasing disk.");
            PutRequest req = new PutRequest();
            req.setFinal();
            Header app = new Header(Header.APP_PARAMETERS);
            app.setValue(new byte[]{(byte) 0x31, (byte) 0x00});
            req.addHeader(app);
            return FormatUtils.threatResponse(put(req));
        }
    }

    public long getDiskSpace() throws IOException, SerialPortException {
        synchronized (this) {
            PutRequest req = new PutRequest();
            req.setFinal();
            Header app = new Header(Header.APP_PARAMETERS);
            app.setValue(new byte[]{0x32, 0x01, 0x01});
            req.addHeader(app);
            PutResponse res = put(req);
            return ByteUtils.bytesToLong(res.getHeaderValue(Header.APP_PARAMETERS));
        }
    }

    public long getFreeSpace() throws IOException, SerialPortException {
        synchronized (this) {
            PutRequest req = new PutRequest();
            req.setFinal();
            Header app = new Header(Header.APP_PARAMETERS);
            app.setValue(new byte[]{0x32, 0x01, 0x02});
            req.addHeader(app);
            PutResponse res = put(req);
            return ByteUtils.bytesToLong(res.getHeaderValue(Header.APP_PARAMETERS));
        }
    }

    public boolean removeObject(final ObexObject file) throws IOException, SerialPortException {
        return removeObject(file.getBinaryName());
    }

    public boolean removeObject(final byte[] filename) throws IOException, SerialPortException {
        synchronized (this) {
            logger.log(Level.FINEST, "Removing object {0}", new String(filename));

            PutRequest req = new PutRequest();
            req.setFinal();

            Header name = new Header(Header.NAME);
            name.setValue(filename);
            req.addHeader(name);

            Response res = put(req);
            return FormatUtils.threatResponse(res);
        }
    }

    public boolean setObjectPerm(ObexObject object, String userPerm, String groupPerm) throws IOException, SerialPortException {
        synchronized (this) {

            PutRequest req = new PutRequest();
            req.setFinal();

            Header name = new Header(Header.NAME);
            name.setValue(object.getBinaryName());
            req.addHeader(name);

            Header app = new Header(Header.APP_PARAMETERS);
            app.setValue(FormatUtils.buildPerm(userPerm, groupPerm));
            req.addHeader(app);

            Response res = put(req);
            return FormatUtils.threatResponse(res);
        }
    }

    public boolean moveObject(ObexObject object, String newPath) throws IOException, SerialPortException {
        return moveObject(object.getPath(), newPath);
    }

    public boolean moveObject(String oldPath, String newPath) throws IOException, SerialPortException {
        synchronized (this) {
            PutRequest req = new PutRequest();
            req.setFinal();
            Header app = new Header(Header.APP_PARAMETERS);
            app.setValue(ByteUtils.prepareMoveByteArray(oldPath, newPath));
            req.addHeader(app);
            Response res = put(req);
            return FormatUtils.threatResponse(res);
        }

    }

    public boolean changeDirectory(final ObexObject object, final boolean create) throws IOException, SerialPortException {
        String path;
        if (object instanceof ObexFolder) {
            if (object == getCurrentFolder()) {
                return true;
            }
            path = object.getPath();
        } else {
            if (object.getParentFolder() == getCurrentFolder()) {
                return true;
            }
            path = object.getParentFolder().getPath();
        }
        return changeDirectory(path, create);

    }

    public boolean changeDirectory(String path, final boolean create) throws IOException, SerialPortException {
        boolean success = true;
        path = PathUtils.preparePath(path);
        if (path.startsWith("/")) {
            path = PathUtils.getRelativePath(path, getCurrentFolder().getPath());
        }
        String currentpath = PathUtils.preparePath(getCurrentFolder().getPath());
        if (currentpath.equalsIgnoreCase(path)) {
            return true;
        }
        String pathList[] = path.split("/");
        for (int i = 0; i < pathList.length; i++) {
            success = success && setFolder(pathList[i], create);
        }
        return success;
    }

    public ObexFile readFile(final String filename) throws IOException, SerialPortException {
        return readFile(new ObexFile(getCurrentFolder(), filename));
    }

    public ObexFile readFile(final ObexFile file) throws IOException, SerialPortException {
        synchronized (this) {
            GetRequest request = new GetRequest();
            request.setFinal();
            Header name = new Header(Header.NAME);
            name.setValue(file.getBinaryName());
            request.addHeader(name);
            file.addResponses(getAll(request));
            return file;
        }
    }

    public ObexFolder loadFolderListing() throws IOException, SerialPortException {
        synchronized (this) {
            GetRequest request = new GetRequest();
            request.setFinal();
            Header type = new Header(Header.TYPE);
            type.setValue(device.getLsName());
            request.addHeader(type);
            getCurrentFolder().addResponses(getAll(request));
            return getCurrentFolder();
        }
    }

    public boolean writeFile(final ObexFile file) throws IOException, SerialPortException {
        synchronized (this) {
            PutRequest req = new PutRequest(file.getHeaderSet());
            PutResponse res = new PutResponse(Response.BADREQUEST);
            Header body;
            InputStream is = file.getInputStream();
            int toRead;
            do {
                int size = (maxPacketLenght - 40) - req.getPacketLength();
                int ava = is.available();
                if (ava < size + 3) {
                    toRead = ava;
                    body = new Header(Header.END_OF_BODY);
                    req.setFinal();
                } else {
                    toRead = size - 3;
                    body = new Header(Header.BODY);
                }
                byte[] b = new byte[toRead];
                is.read(b);
                body.setValue(b);
                req.addHeader(body);
                res = put(req);
                req = new PutRequest();

            } while (res != null && (res.getType() & 0x7F) == Response.CONTINUE);
            is.close();
            file.setInputStream(null);
            return res != null && (res.getType() & 0x7F) == Response.SUCCESS;
        }
    }

    private PutResponse put(final PutRequest req) throws IOException, SerialPortException {
        sendPacketAndWait(req, TIMEOUT);
        if (hasIncomingPacket) {
            return new PutResponse(incomingData.pullData());
        }
        return null;
    }

    private boolean setPath(final String folder, final boolean create) throws IOException, SerialPortException {
        synchronized (this) {
            boolean success = false;
            SetPathRequest req = new SetPathRequest();
            if (folder != null) {
                if (folder.isEmpty()) {
                    return true;
                }
                logger.log(Level.FINER, "Setting path to {0}", folder);
                Header name = new Header(Header.NAME);
                name.setValue(ByteUtils.nameToBytes(folder));
                req.addHeader(name);
                req.setFlags(create ? 0x00 : (byte) 0x02);
            } else {
                logger.log(Level.FINER, "Setting path to parent folder.");
                req.setFlags((byte) 0x03);
            }
            sendPacketAndWait(req, 500);
            if (hasIncomingPacket) {
                if (incomingData.pullData()[0] == (byte) 0xA0) {
                    logger.log(Level.FINEST, "Path setted.");
                    success = true;
                }
            }
            return success;
        }
    }

    private GetResponse get(final GetRequest request) throws IOException, SerialPortException {
        sendPacketAndWait(request, TIMEOUT);
        if (hasIncomingPacket) {
            return new GetResponse(incomingData.pullData());
        }
        return null;
    }

    private GetResponse[] getAll(GetRequest request) throws IOException, SerialPortException {
        logger.log(Level.FINER, "Get operation to perform.");
        ArrayList<GetResponse> arrayList = new ArrayList<GetResponse>();

        GetResponse response = new GetResponse(Response.BADREQUEST);
        do {
            arrayList.add(response = get(request));
            request = new GetRequest();
        } while (response!=null && (response.getType() & 0x7F) == Response.CONTINUE);

        GetResponse[] responses = new GetResponse[arrayList.size()];
        responses = arrayList.toArray(responses);
        return responses;
    }

    private synchronized void sendPacket(final Packet pkt) throws IOException, SerialPortException {
        logger.log(Level.FINEST, "Sending {0}", ByteUtils.dumpBytes(pkt.toBytes()));
        conn.writeAll(pkt.toBytes());
    }

    private void sendPacketAndWait(final Packet pkt, final int timeout) throws IOException, SerialPortException {
        synchronized (holder) {
            sendPacket(pkt);
            try {
                hasIncomingPacket = false;
                logger.log(Level.FINEST, "Waiting response for {0} ms.", timeout);
                holder.wait(timeout);
            } catch (InterruptedException iE) {
            }
        }
    }

    private boolean setFolder(String folderName, final boolean create) throws IOException, SerialPortException {
        if ("..".equals(folderName)) {
            folderName = null;
        }
        boolean success = setPath(folderName, create);
        if (success) {
            if (folderName == null) {
                currentFolder = getCurrentFolder().getParentFolder();
            } else if (!"".equals(folderName)) {
                ObexFolder childFolder = getCurrentFolder().getChildFolder(folderName);
                currentFolder = childFolder == null ? new ObexFolder(getCurrentFolder(), folderName) : childFolder;
            }

        }

        logger.log(Level.FINE, "Now in folder {0}", getCurrentFolder().getPath());

        return success;
    }

    public ObexFolder getCurrentFolder() {
        if (currentFolder == null) {
            currentFolder = ObexObject.ROOT_FOLDER;
        }
        return currentFolder;
    }

    private class ObexEventListener implements DataEventListener, ConnectionModeListener {

        public void DataEvent(final byte[] event) {
            synchronized (holder) {
                incomingData.pushData(event);
                hasIncomingPacket = incomingData.isReady();
                if (hasIncomingPacket) {
                    holder.notifyAll();
                }
            }
        }

        public void update(final int mode, final boolean changed) {
            if (mode != ATConnection.MODE_DATA && !changed) {
                try {
                    logger.log(Level.WARNING, "Datamode to close unexpectedly mode was:"+ mode + " change was "+ changed);
                    try {
                        disconnect();
                    } catch (SerialPortException ex) {
                        Logger.getLogger(ObexClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ObexClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public int getMaxPacketLenght() {
        return maxPacketLenght;
    }

    public void setMaxPacketLenght(final int maxPacketLenght) {
        this.maxPacketLenght = maxPacketLenght;
    }

    public boolean isConnected() {
        return connected;
    }
}

// Class used for controlling the incoming data when it cames broken
class OBEXData {

    private byte[] data = null;

    OBEXData() {}

    public int getPacketLenght() {
        return ByteUtils.bytesToInt(ByteUtils.getBytes(data, 1, 2));
    }

    public int getDataLenght() {
        return data.length;
    }

    public void pushData(final byte[] newData) {
        Logger.getLogger("com.lhf.jobexftp").log(Level.FINEST, "Pushed data {0}", ByteUtils.dumpBytes(newData));
        if (data == null) {
            data = newData;
        } else {
            byte[] temp = new byte[data.length + newData.length];
            ByteUtils.setBytes(temp, data, 0, data.length);
            ByteUtils.setBytes(temp, newData, data.length, newData.length);
            data = temp;
        }
    }

    public boolean isReady() {
        return data != null && getDataLenght() == getPacketLenght();
    }

    public byte[] pullData() {
        byte[] temp = data;
        data = null;
        return temp;
    }
}
