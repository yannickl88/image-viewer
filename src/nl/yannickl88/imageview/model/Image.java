package nl.yannickl88.imageview.model;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

/**
 * Data wrapper for an image in the library. This contains a thumbnail and some metadata.
 */
public class Image {
    public static final int THUMB_SIZE = 150;

    /**
     * Metadata for an image. This contains information about width, height, modification time, original file path and
     * labels for an image.
     */
    public static class Metadata {
        public final int width, height;
        public final String path;
        public final Set<String> labels;
        public final long ctime;

        public Metadata(int width, int height, String path, long ctime, Set<String> labels) {
            this.path = path;
            this.ctime = ctime;
            this.labels = labels;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Metadata which has been bound to the Image for notifying of any changes made to the metadata.
     * @see Metadata
     */
    public static class BoundMetadata {
        public final int width, height;
        public final String name;
        public final String path;
        public final List<String> labels;
        public final long ctime;
        private final Image image;

        private BoundMetadata(Image image, Metadata metadata) {
            this.image = image;

            width = metadata.width;
            height = metadata.height;
            path = metadata.path;
            ctime = metadata.ctime;
            name = Paths.get(path).getFileName().toString();

            labels = new ArrayList<>();

            updateLabels(metadata.labels);
        }

        /**
         * Set the labels for the image.
         */
        public void setLabels(Set<String> labels) {
            updateLabels(labels);

            image.notifyOfImageChange();
        }

        private void updateLabels(Set<String> labels) {
            this.labels.clear();
            this.labels.addAll(labels);
            this.labels.sort(String::compareTo);
        }
    }

    /**
     * Return a resized version of the image for the thumbnail.
     */
    private static BufferedImage createThumb(BufferedImage image) {
        double ratio = (double) image.getWidth() / (double) image.getHeight();
        int width = THUMB_SIZE, height = THUMB_SIZE;

        if (ratio < 1.0) {
            width = (int) (ratio * height);
        } else {
            height = (int) (width / ratio);
        }

        java.awt.Image resized = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);

        BufferedImage thumb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = thumb.getGraphics();

        g.drawImage(resized, 0, 0, null);
        g.dispose();

        return thumb;
    }

    /**
     * Decode a base64image string into a BufferedImage.
     */
    private static BufferedImage fromBase64String(String encodedString) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedString)));
    }

    /**
     * Encode a BufferedImage string into a base64image.
     */
    private static String toBase64String(BufferedImage image) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", stream);

        return Base64.getEncoder().encodeToString(stream.toByteArray());
    }

    public final String thumbData;
    public final BoundMetadata metadata;
    public final BufferedImage thumb;
    private final ArrayList<ImageChangeListener> listeners;

    public interface ImageChangeListener {
        void onChange(Image image);
    }

    /**
     * Create an Image from a file.
     */
    public Image(File file) throws IOException {
        this(file, ImageIO.read(file));
    }

    /**
     * Intermediate step for creating images from a file.
     */
    private Image(File file, BufferedImage image) throws IOException {
        this(createThumb(image), new Metadata(image.getWidth(), image.getHeight(), file.getAbsolutePath(), file.lastModified(), new HashSet<>()));
    }

    /**
     * Create an image from the base64encoded thumbnail data and the metadata.
     */
    public Image(String thumbData, Metadata metadata) throws IOException {
        this(fromBase64String(thumbData), thumbData, metadata);
    }

    /**
     * Create an image from the BufferedImage thumbnail data and the metadata.
     */
    public Image(BufferedImage thumb, Metadata metadata) throws IOException {
        this(thumb, toBase64String(thumb), metadata);
    }

    private Image(BufferedImage thumb, String thumbData, Metadata metadata) {
        this.thumbData = thumbData;
        this.thumb = thumb;
        this.metadata = new BoundMetadata(this, metadata);

        listeners = new ArrayList<>();
    }

    /**
     * Register a change listener for this image.
     */
    public void addChangeListener(ImageChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove any registered change listener for this image.
     */
    public void removeChangeListener(ImageChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all registered ImageChangeListener for changes to the image.
     */
    private void notifyOfImageChange() {
        for (ImageChangeListener l : listeners) {
            l.onChange(this);
        }
    }

    @Override
    public int hashCode() {
        return metadata.path.hashCode();
    }
}
