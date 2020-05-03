package nl.yannickl88.imageview.model.library;

import nl.yannickl88.imageview.model.Image;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class Library {
    private final String name;
    private final File root;
    private final ArrayList<Image> images;
    private final ArrayList<LibraryChangeListener> listeners;
    private final HashSet<String> labels;
    private final ReentrantLock lock;
    private File config;
    private boolean isLoaded = false;

    public interface LibraryChangeListener {
        void onLibraryChange(List<Image> images);
    }

    public static class LibraryLoader extends Thread {
        private final Library library;
        private final NodeList images;
        private final XPath xPath;
        private final String rootFolder;

        public LibraryLoader(Library library, NodeList images, XPath xPath, String rootFolder) {
            super();

            this.library = library;
            this.images = images;
            this.xPath = xPath;
            this.rootFolder = rootFolder;
        }

        @Override
        public void run() {
            for (int i = 0; i < images.getLength(); i++) {
                NamedNodeMap attributes = images.item(i).getAttributes();
                HashSet<String> labels = new HashSet<>();

                try {
                    NodeList labelNodes = (NodeList) xPath.compile("./label").evaluate(images.item(i), XPathConstants.NODESET);
                    for (int j = 0; j < labelNodes.getLength(); j++) {
                        labels.add(labelNodes.item(j).getTextContent());
                    }
                } catch (XPathExpressionException ignored) {
                }

                Image.Metadata metadata = new Image.Metadata(
                        Integer.parseInt(attributes.getNamedItem("width").getTextContent()),
                        Integer.parseInt(attributes.getNamedItem("height").getTextContent()),
                        Paths.get(rootFolder, attributes.getNamedItem("src").getTextContent()).toString(),
                        Long.parseLong(attributes.getNamedItem("ctime").getTextContent()),
                        labels
                );

                try {
                    library.addSilent(new Image(
                            attributes.getNamedItem("thumb").getTextContent(),
                            metadata
                    ));

                    // every 50 images, notify of any changes
                    if (i % 50 == 49) {
                        library.notifyLibraryChange();
                    }
                } catch (IOException ignored) {
                }
            }

            // When we are done, also notify
            library.notifyLibraryChange();
            library.recalculateLabels();

            library.isLoaded = true;
        }
    }

    public static Library init(File libraryPath) {
        Library library = new Library("root", libraryPath, null);
        library.isLoaded = true;

        return library;
    }

    public static Library open(File config) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(config);

            XPath xPath = XPathFactory.newInstance().newXPath();

            // set the root path
            String rootFolder = ((String) xPath.compile("//config/root").evaluate(document, XPathConstants.STRING));
            Library library = new Library("root", new File(rootFolder), config);

            // load all images
            NodeList images = (NodeList) xPath.compile("//images/image").evaluate(document, XPathConstants.NODESET);

            LibraryLoader loader = new LibraryLoader(library, images, xPath, rootFolder);
            loader.start();

            return library;
        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void save() {
        if (null == config) {
            return;
        }

        lock.lock();

        try {
            try {
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

                Element root = document.createElement("library");
                document.appendChild(root);

                // config element
                Element config = document.createElement("config");
                Element configRoot = document.createElement("root");
                configRoot.setTextContent(this.root.getAbsolutePath());

                config.appendChild(configRoot);
                root.appendChild(config);

                // images element
                Element images = document.createElement("images");

                // Store the items in the order they are shown so when loaded it makes more sense.
                ArrayList<Image> items = new ArrayList<>(this.images);
                items.sort((o1, o2) -> Long.compare(o2.metadata.ctime, o1.metadata.ctime));

                for (Image i : items) {
                    Element image = document.createElement("image");

                    image.setAttribute("src", i.metadata.name);
                    image.setAttribute("ctime", String.valueOf(i.metadata.ctime));
                    image.setAttribute("width", String.valueOf(i.metadata.width));
                    image.setAttribute("height", String.valueOf(i.metadata.height));
                    image.setAttribute("thumb", i.thumbData);

                    for (String l : i.metadata.labels) {
                        Element label = document.createElement("label");
                        label.setTextContent(l);

                        image.appendChild(label);
                    }

                    images.appendChild(image);
                }

                root.appendChild(images);

                // Write
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                DOMSource domSource = new DOMSource(document);
                StreamResult streamResult = new StreamResult(this.config);

                transformer.transform(domSource, streamResult);
            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    public Library(String name, File root, File config) {
        this.name = name;
        this.root = root;
        this.config = config;

        listeners = new ArrayList<>();
        images = new ArrayList<>();
        labels = new HashSet<>();

        lock = new ReentrantLock();
    }

    public void addChangeListener(LibraryChangeListener listener) {
        listeners.add(listener);
    }

    public List<Image> getImages() {
        lock.lock();
        try {
            return images;
        } finally {
            lock.unlock();
        }
    }

    public File getDir() {
        return root;
    }

    public boolean isPersistent() {
        return null != this.config;
    }

    public void setConfigFile(File file) {
        this.config = file;

        // Force a save
        this.save();
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public boolean contains(String name) {
        lock.lock();

        try {
            for (Image i : images) {
                if (i.metadata.path.equals(name)) {
                    return true;
                }
            }

            return false;
        } finally {
            lock.unlock();
        }
    }

    public void remove(Image image) {
        lock.lock();
        try {
            images.remove(image);
        } finally {
            lock.unlock();
        }

        notifyLibraryChange();
    }

    public void delete(Image image) {
        lock.lock();
        try {
            File f = new File(image.metadata.path);
            if (f.delete()) {
                images.remove(image);
            }
        } finally {
            lock.unlock();
        }

        notifyLibraryChange();
    }

    public void add(Image image) {
        addSilent(image);

        notifyLibraryChange();
    }

    private void addSilent(Image image) {
        lock.lock();
        try {
            images.add(image);
            labels.addAll(image.metadata.labels);
        } finally {
            lock.unlock();
        }

        image.addChangeListener((i) -> {
            this.recalculateLabels();
            save();
        });
    }

    public Set<String> getAllLabels() {
        return labels;
    }

    public void recalculateLabels() {
        HashSet<String> newLabels = new HashSet<>();
        lock.lock();
        try {
            for (Image i : images) {
                newLabels.addAll(i.metadata.labels);
            }

            // are they the same?
            if (newLabels.containsAll(labels) && labels.containsAll(newLabels)) {
                return;
            }

            labels.clear();
            labels.addAll(newLabels);
        } finally {
            lock.unlock();
        }
    }

    public void dispose() {
        listeners.clear();
    }

    private void notifyLibraryChange() {
        for (LibraryChangeListener l : listeners) {
            l.onLibraryChange(images);
        }
    }
}
