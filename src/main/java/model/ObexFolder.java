package model;

import io.data.Header;
import utils.ByteUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import utils.FormatUtils;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public final class ObexFolder extends ObexObject {

    private final Map<String, ObexObject> subobjects = new TreeMap<String, ObexObject>();
    private static final OBEXFolderListingParser PARSER = new OBEXFolderListingParser();

    public ObexFolder(final ObexFolder parentFolder, final String filename) {
        super(parentFolder, filename);
    }

    public ObexFolder(final String filename) {
        super(ROOT_FOLDER, filename);
    }

    public void add(final ObexObject object) {
        subobjects.put(object.name, object);
    }

    public Map<String, ObexObject> getSubobjects() {
        return subobjects;
    }

    public ObexFolder getChildFolder(final String name) {
        ObexObject obj = subobjects.get(name);
        if (obj != null && obj instanceof ObexFolder) {
            return (ObexFolder) obj;
        }
        return null;
    }

    public ObexFile getChildFile(final String name) {
        ObexObject obj = subobjects.get(name);
        if (obj != null && obj instanceof ObexFolder) {
            return (ObexFile) obj;
        }
        return null;
    }

    public ObexFolder addFolder(String filename) {
        filename = filename.replace('/', ' ').trim();
        if (filename.isEmpty()) {
            return this;
        }
        ObexFolder folder = new ObexFolder(this, filename);
        add(folder);
        return folder;
    }

    public ObexFile addFile(String filename) {
        filename = filename.replace('/', ' ').trim();
        if (filename.isEmpty()) {
            return null;
        }
        ObexFile file = new ObexFile(filename);
        add(file);
        return file;
    }

    public String getListing() {
        StringBuilder builder = new StringBuilder();
        int folders = 0, files = 0;
        long fileSpace = 0;
        builder.append("\n Directory of ").append(getPath()).append("/\n\n").length();

        if (this != ROOT_FOLDER) {
            FormatUtils.listingFormat(builder, ".", getSizeString(), getTime(), getUserPerm(), getGroupPerm());
            FormatUtils.listingFormat(builder, "..", getParentFolder().getSizeString(), getParentFolder().getTime(), getParentFolder().getUserPerm(), getParentFolder().getGroupPerm());
        }
        for (Iterator<ObexObject> it = subobjects.values().iterator(); it.hasNext();) {
            ObexObject ob = it.next();
            builder.append(ob.toString());
            if (ob instanceof ObexFile) {
                files++;
                ObexFile file = (ObexFile) ob;
                fileSpace += ByteUtils.bytesToInt(file.getSize());
            } else {
                folders++;

            }
        }
        builder.append("                    ").append(files).append(" file(s) ").append(ByteUtils.humanReadableByteCount(fileSpace, true)).append("\n");
        builder.append("                    ").append(folders).append(" dir(s)\n\n");

        return builder.toString();
    }

    @Override
    protected void threatHeader(final Header header) {
        switch (header.getId()) {
            case Header.NAME:
                setName(header.getValue());
            case Header.BODY:
                try {
                    setContents(header.getValue());
                } catch (IOException ex) {
                }
                break;
            case Header.END_OF_BODY:
                try {
                    setContents(header.getValue());
                } catch (IOException ex) {
                }
                setReady();
                break;
        }
    }

    @Override
    protected void onReady() {
        try {
            PARSER.parse(getContents(), this);
        } catch (Exception ex) {
            Logger.getLogger(ObexFolder.class.getName()).log(Level.SEVERE, "There was an error parsing the folder listing, please try again.");
        }
    }

    @Override
    protected void onReset() {
        subobjects.clear();
    }

    @Override
    public String getSizeString() {
        return "<DIR>";
    }

    @Override
    public boolean equals(Object compobj) {
        if (compobj instanceof ObexFolder) {
            ObexFolder compfol = (ObexFolder) compobj;
            return compfol.getPath().equalsIgnoreCase(this.getPath());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + (this.getPath() != null ? this.getPath().hashCode() : 0);
        return hash;
    }
}

class OBEXFolderListingParser extends DefaultHandler {

    private ObexFolder folder;
    private XMLReader xml;
    private File file;

    OBEXFolderListingParser() {
        try {
            xml = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            init();
        } catch (SAXException ex) {
            Logger.getLogger(OBEXFolderListingParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OBEXFolderListingParser.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(OBEXFolderListingParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ObexFolder parse(final byte[] folderListing, final ObexFolder folder) throws IOException, SAXException {
        this.folder = folder;
        ByteArrayInputStream bis = new ByteArrayInputStream(folderListing);
        InputSource inps = new InputSource(bis);
        inps.setSystemId(file.toURI().toURL().toExternalForm());
        xml.parse(inps);
        return folder;
    }

    private void init() throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(false);
        spf.setFeature("http://xml.org/sax/features/validation", false);
        SAXParser sax = spf.newSAXParser();
        xml = sax.getXMLReader();
        xml.setFeature("http://xml.org/sax/features/validation", false);
        xml.setContentHandler(this);

        try {
            file = new File("obex-folder-listing.dtd");
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        if (this.folder == null) {
            this.folder = ObexFolder.ROOT_FOLDER;
        }
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
        ObexObject cObj = null;
        String oName = attributes.getValue("name");
        if (qName.equalsIgnoreCase("folder")) {
            cObj = folder.getChildFolder(oName);
            cObj = cObj == null ? new ObexFolder(folder, oName) : cObj;
        } else if (qName.equalsIgnoreCase("file")) {
            ObexFile cFile = folder.getChildFile(oName);
            cFile = cFile == null ? new ObexFile(folder, oName) : cFile;
            cFile.setSize(Integer.parseInt(attributes.getValue("size")));
            cObj = cFile;
        }
        if (cObj == null) {
            return;
        }
        cObj.setTime(attributes.getValue("modified"));
        cObj.setUserPerm(attributes.getValue("user-perm"));
        cObj.setGroupPerm(attributes.getValue("group-perm"));
        folder.add(cObj);
    }
}
