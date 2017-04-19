package io;

import event.ATEventListener;
import event.ConnectionModeListener;
import event.DataEventListener;
import jssc.*;
import model.ObexDevice;
import utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public class ATConnection {

    public static final byte FLOW_NONE = '0';
    public static final byte FLOW_XONXOFF = '1';
    public static final byte FLOW_RTSCTS = '3';
    public static final int MODE_DISCONNECTED = 0;
    public static final int MODE_AT = 1;
    public static final int MAX_TIMEOUT_RETRIES = 5;
    public static final int MODE_DATA = 2;

    private static final Logger LOGGER = Logger.getLogger("com.lhf.jobexftp");

    private final SerialPortEventListener eventListener = new ATSerialPortEventListener();
    private final ArrayList<ConnectionModeListener> connModeListners = new ArrayList<ConnectionModeListener>(10);
    private final ArrayList<DataEventListener> dataEventListners = new ArrayList<DataEventListener>(10);
    private final ArrayList<ATEventListener> atEventListners = new ArrayList<ATEventListener>(10);
    private final Object holder = new Object();

    private int connMode = MODE_DISCONNECTED;
    private int baudRate = 460800;
    private byte flowControl = FLOW_RTSCTS;
    private byte[] incomingData;
    private boolean hasIncomingPacket;
    private String comPortIdentifier;
    private SerialPort serialPort;
    private ObexDevice device;
    private int errors = 0;

    public ATConnection(final String connPortPath) {
        comPortIdentifier = connPortPath;
    }

    public synchronized void setConnMode(final int newConnMode) throws IOException, SerialPortException, SerialPortTimeoutException {
        if (connMode == newConnMode) { //nothing to do
            return;
        }
        LOGGER.log(Level.FINEST, "Switching from connection mode {0} to mode {1}.", new String[]{Integer.toString(connMode), Integer.toString(newConnMode)});
        notifyModeListeners(newConnMode, false); //Notify going to change.
        switch (connMode) {
            case MODE_DISCONNECTED:
                open();
                if (newConnMode == MODE_DATA) {
                    openDataMode();
                }
                break;
            case MODE_AT:
                if (newConnMode == MODE_DISCONNECTED) {
                    close();
                } else {
                    openDataMode();
                }
                break;
            case MODE_DATA:
                closeDataMode();
                if (newConnMode == MODE_DISCONNECTED) {
                    close();
                }
                break;
        }
        if (connMode == newConnMode) {
            errors = 0;
            notifyModeListeners(connMode, true); //Notify changed
        } else {
            errors++; //sometimes things can go wrong.
            if (errors > 3) {
                terminate();
                throw new IOException("I/O Error. Cannot communicate properly.");
            }
        }
    }

    public synchronized byte[] send(final byte[] b, final int timeout) throws IOException {
        if (connMode != MODE_AT) {
            LOGGER.log(Level.FINE, "Trying to send in wrong mode. Mode is {0}", connMode);
        }
        return sendPacket(b, timeout);
    }

    public void identifyDevice() throws IOException {
        String s = "";
        setDevice(ObexDevice.TC65);
        for (int m = 5; m > 0; m--) {
            s = new String(send("AT+CGMM\r".getBytes(), 500));
            if (s.indexOf("TC65i") > -1) {
                LOGGER.log(Level.FINE, "Found TC65i device.");
                setDevice(ObexDevice.TC65);
                return;
            } else if (s.indexOf("TC65") > -1) {
                LOGGER.log(Level.FINE, "Found TC65 device.");
                setDevice(ObexDevice.TC65);
                return;
            } else if (s.indexOf("XT75") > -1) {
                LOGGER.log(Level.FINE, "Found XT75 device will handle this like TC65.");
                setDevice(ObexDevice.TC65);
                return;
            } else if (s.indexOf("XT65") > -1) {
                LOGGER.log(Level.FINE, "Found XT65 device will handle this like TC65.");
                setDevice(ObexDevice.TC65);
                return;
            } else if (s.indexOf("AT+CGMM") > -1) {
                LOGGER.log(Level.WARNING, "Unexpected behavior, trying to fix.", s); //try restablize.
                send(new byte[]{'A', 'T', 'E', '\r'}, 50); //disable echo
                send(new byte[]{'A', 'T', '\r'}, 50);
                send(new byte[]{'A', 'T', '\r'}, 50);
            }
        }
        if (device == null) {
            throw new IOException("Device is in wrong mode or device not supported.");
        }
    }

    public boolean isAnswering() {
        try {
            if (send(getDevice().CMD_CHECK, 200).length > 0) {
                return true;
            }
        } catch (IOException ex) {
            Logger.getLogger(ATConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public void addConnectionModeListener(final ConnectionModeListener listener) {
        connModeListners.add(listener);
    }

    public void removeConnectionModeListener(final ConnectionModeListener listener) {
        connModeListners.remove(listener);
    }

    public void addDataEventListener(final DataEventListener listener) {
        dataEventListners.add(listener);
    }

    public void addATEventListener(final ATEventListener listener) {
        atEventListners.add(listener);
    }

    public void removeDataEventListener(final DataEventListener listener) {
        dataEventListners.remove(listener);
    }

    public void removeATEventListener(final ATEventListener listener) {
        atEventListners.remove(listener);
    }

    private synchronized byte[] sendPacket(final byte[] b, final int timeout) throws IOException {
        LOGGER.log(Level.FINER, "Send {0}", new String(b));
        synchronized (holder) {
            try {
                serialPort.writeBytes(b);

                hasIncomingPacket = false;
                holder.wait(timeout);
                //Thread.currentThread().sleep(100);
                for (int i = 0; i < MAX_TIMEOUT_RETRIES; i++) {

                    if (incomingData==null || (!new String(incomingData).contains("OK") && !new String(incomingData).contains("ERROR"))) {
                        LOGGER.log(Level.FINEST, "No valid answer from modem "
                                + "waiting another: [" + timeout + "ms].");
                        holder.wait(timeout);
                    }
                }
            } catch (InterruptedException iE) {
                System.out.println("**********************");
            } catch (SerialPortException ex) {
                Logger.getLogger(ATConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (!hasIncomingPacket) {
            incomingData = new byte[0];
        }
        LOGGER.log(Level.FINER, "Recieve {0}", new String(incomingData));
        return incomingData;
    }

    private synchronized void open() throws IOException, SerialPortException, SerialPortTimeoutException {
        LOGGER.log(Level.FINEST, "Configuring serial port");

        try {
            serialPort = new SerialPort(getComPortIdentifier());
            serialPort.openPort();
            serialPort.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            int mask = SerialPort.MASK_RXCHAR;
            serialPort.setEventsMask(mask);//Set mask
            serialPort.addEventListener(eventListener);
        } catch (SerialPortException ex) {
            Logger.getLogger(ATConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        connMode = MODE_AT;
        onOpen();
    }

    protected void onOpen() throws IOException, SerialPortException, SerialPortTimeoutException {
        estabilize();
        identifyDevice();
    }

    public void estabilize() throws IOException, SerialPortException, SerialPortTimeoutException {
        LOGGER.log(Level.FINEST, "Estabilizating I/O");
        serialPort.readBytes(serialPort.getInputBufferBytesCount(), 1000);

        if (sendPacket(ObexDevice.CMD_CHECK, 50).length < 1) {
            closeDataMode();
        }

        checkSend(new byte[]{'A', 'T', 'Z', '\r'}, 50); // reset settings
        checkSend(("AT+IPR=" + baudRate + "\r").getBytes(), 100);  //set baud rate
        checkSend(new byte[]{'A', 'T', 'E', '\r'}, 50); //disable echo
        checkSend(new byte[]{'A', 'T', 'E', '\r'}, 50); //disable echo try 2.
        checkSend(ObexDevice.CMD_CHECK, 50); // send check
    }

    private void checkSend(byte[] b, int timeout) throws IOException {
        if (new String(sendPacket(b, timeout)).contains("ERROR")) {
            throw new IOException("Device is in wrong mode or is not supported");
        }
    }

    protected void onClose() throws IOException {
        sendPacket(new byte[]{'A', 'T', 'Z', '\r'}, 50);
    }

    private void close() throws IOException, SerialPortException {
        try {
            onClose();
        } finally {
            terminate();
        }
    }

    public void terminate() throws SerialPortException {
        if (serialPort != null) {
            serialPort.removeEventListener();
        }
        if (serialPort != null) {
            serialPort.closePort();
        }
        connMode = MODE_DISCONNECTED;
    }

    private void closeDataMode() throws IOException {
        LOGGER.log(Level.FINEST, "Closing datamode.");
        try {
            Thread.sleep(1000);
            byte[] r = sendPacket(new byte[]{'+', '+', '+'}, 1000);
            connMode = (IOUtils.arrayContainsOK(r) ? MODE_AT : MODE_DATA);
        } catch (InterruptedException ex) {
        }
    }

    private void openDataMode() throws IOException {
        LOGGER.log(Level.FINEST, "Opening datamode.");
        send(getDevice().getFlowControl(flowControl), 500);
        if (!IOUtils.arrayContainsOK(send(getDevice().getObexCheck(), 500))) {
            connMode = MODE_AT;
            return;
        }
        boolean b = IOUtils.arrayContainsOK(send(getDevice().getObexOpen(), 2000));
        connMode = (b ? MODE_DATA : MODE_AT);
    }

    private void notifyModeListeners(final int newConnMode, boolean changed) {
        for (int i = 0; i < connModeListners.size(); i++) {
            connModeListners.get(i).update(newConnMode, changed);
        }
    }

    private void notifyDataEventListeners(final byte[] event) {
        for (Iterator<DataEventListener> it = dataEventListners.iterator(); it.hasNext();) {
            it.next().DataEvent(event);
        }
    }

    protected void notifyATEventListeners(final byte[] event) {
        for (Iterator<ATEventListener> it = atEventListners.iterator(); it.hasNext();) {
            it.next().ATEvent(event);
        }
    }

    public int getConnMode() {
        return connMode;
    }

    public void setBaudRate(final int baudRate) {
        this.baudRate = baudRate;
    }

    public void setFlowControl(final byte flowControl) {
        if (flowControl != FLOW_NONE && flowControl != FLOW_RTSCTS && flowControl != FLOW_XONXOFF) {
            throw new IllegalArgumentException("Unknown flowcontrol type");
        }
        this.flowControl = flowControl;

    }

    public byte getFlowControl() {
        return flowControl;
    }

    public ObexDevice getDevice() {
        return device;
    }

    public void setDevice(final ObexDevice device) {
        this.device = device;
    }

    public void writeAll (byte[] data) throws SerialPortException {
        serialPort.writeBytes(data);
    }

    public byte[] readAll() throws IOException, SerialPortException {
        byte[] incomingData;
        incomingData = serialPort.readBytes();
        return incomingData;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public String getComPortIdentifier() {
        return comPortIdentifier;
    }

    public void setComPortIdentifier(String comPortIdentifier) {
        this.comPortIdentifier = comPortIdentifier;
    }

    private final class ATSerialPortEventListener implements SerialPortEventListener {

        public void serialEvent(final SerialPortEvent spe) {
            synchronized (holder) {
                if (spe.isRXCHAR()) {
                    try {
                        incomingData = readAll();
                    } catch (Throwable ex) {
                        notifyModeListeners(connMode, false);
                        try {
                            terminate();
                        } catch (SerialPortException ex1) {
                            Logger.getLogger(ATConnection.class.getName()).log(Level.SEVERE, null, ex1);
                        }
                        connMode = MODE_DISCONNECTED;
                        notifyModeListeners(connMode, true);
                    }
                    hasIncomingPacket = true;
                    holder.notifyAll();
                    if (connMode == MODE_DATA) {
                        notifyDataEventListeners(incomingData);
                    } else if (connMode == MODE_AT) {
                        notifyATEventListeners(incomingData);
                    }
                }
            }
        }
    }
}
