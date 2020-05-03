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

public class Image {
    public static final int THUMB_SIZE = 150;

    public static Image fromFile(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        BufferedImage thumb = createThumb(image);

        return new Image(thumb, new Metadata(image.getWidth(), image.getHeight(), file.getAbsolutePath(), file.lastModified(), new HashSet<>()));
    }

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

    public final String thumbData;
    public final BoundMetadata metadata;
    public final BufferedImage thumb;
    private final ArrayList<ImageChangeListener> listeners;

    public interface ImageChangeListener {
        void onChange(Image image);
    }

    public Image(String thumbData, Metadata metadata) throws IOException {
        this(fromBase64String(thumbData), thumbData, metadata);
    }

    public Image(BufferedImage thumb, Metadata metadata) throws IOException {
        this(thumb, toBase64String(thumb), metadata);
    }

    private Image(BufferedImage thumb, String thumbData, Metadata metadata) {
        this.thumbData = thumbData;
        this.thumb = thumb;
        this.metadata = new BoundMetadata(this, metadata);

        listeners = new ArrayList<>();
    }

    public void addChangeListener(ImageChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ImageChangeListener listener) {
        listeners.remove(listener);
    }

    private static BufferedImage fromBase64String(String encodedString) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(encodedString)));
    }

    private static String toBase64String(BufferedImage image) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", stream);

        return Base64.getEncoder().encodeToString(stream.toByteArray());
    }

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
