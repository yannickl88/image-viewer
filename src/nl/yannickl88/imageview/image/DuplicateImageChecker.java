package nl.yannickl88.imageview.image;

import nl.yannickl88.imageview.model.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checker class for seeing if there are duplicate images.
 */
public class DuplicateImageChecker {
    private static final float SIMILARITY_THRESHOLD = 0.01f;
    private final ArrayList<ProgressListener> listeners;
    private final ArrayList<Duplicate> foundDuplicates;
    private boolean running = false;

    public interface ProgressListener {
        /**
         * Triggers when starting a check.
         */
        void onStart();

        /**
         * Triggers when progress has been made. The {@code completion} is a value between 0 and 1, where 1 indicates
         * 100% of the tasks have been completed.
         */
        void onProgress(double completion);

        /**
         * Triggers when all images are checked.
         */
        void onComplete();

        /**
         * Triggers when there is an (intermediate) result.
         */
        void onResultChange();
    }

    /**
     * Wrapper class which represents a group of images which have been found to be duplicates.
     */
    public static class Duplicate {
        public final Set<Image> duplicates;

        public Duplicate(HashSet<Image> duplicates) {
            this.duplicates = duplicates;
        }
    }

    /**
     * Checker class for asynchronous checking.
     */
    private class Checker extends Thread {
        private final List<Image> images;

        private Checker(List<Image> images) {
            this.images = images;
        }

        @Override
        public void run() {
            notifyOfStart();
            running = true;
            foundDuplicates.clear();

            ImageCache<BufferedImage> cache = new ImageCache<>();
            int n = images.size();

            for (int i = 0; i < n; i++) {
                Image image = images.get(i);
                HashSet<Image> group = new HashSet<>();
                group.add(image);

                for (int j = i + 1; j < n; j++) {
                    Image other = images.get(j);
                    BufferedImage imageA, imageB;

                    if (cache.has(image)) {
                        imageA = cache.get(image);
                    } else {
                        imageA = getNormalizedImage(image.thumb);
                        cache.put(image, imageA);
                    }

                    if (cache.has(other)) {
                        imageB = cache.get(other);
                    } else {
                        imageB = getNormalizedImage(other.thumb);
                        cache.put(other, imageB);
                    }

                    if (DuplicateImageChecker.imagesAreTheSame(imageA, imageB)) {
                        group.add(other);
                    }
                }

                if (group.size() > 1) {
                    foundDuplicates.add(new Duplicate(group));
                    notifyOfResultChange();
                }

                notifyOfProgress(i / (double) n);
            }
            running = false;
            notifyOfComplete();
        }
    }

    public DuplicateImageChecker() {
        super();

        listeners = new ArrayList<>();
        foundDuplicates = new ArrayList<>();
    }

    /**
     * Register a progress listener for the checker.
     */
    public void addProgressListener(ProgressListener listener) {
        listeners.add(listener);
    }

    /**
     * Start a check for duplicates.
     *
     * NOTE: while another check is in progress, this method does nothing.
     */
    public void checkImages(List<Image> images) {
        if (!running) {
            Checker checker = new Checker(images);
            checker.start();
        }
    }

    /**
     * Return a list of duplicates which have been found by all the done checks.
     */
    public List<Duplicate> getResults() {
        return foundDuplicates;
    }

    /**
     * Resolution of the image to use.
     */
    private static final int RESOLUTION = 25;

    /**
     * Check if two images are the same based on the pixel color differences.
     */
    private static boolean imagesAreTheSame(BufferedImage a, BufferedImage b) {
        float runningTotal = 0.0f;
        for (int i = 0; i < RESOLUTION; i++) {
            for (int j = 0; j < RESOLUTION; j++) {
                Color colorA = new Color(a.getRGB(i, j));
                Color colorB = new Color(b.getRGB(i, j));

                float differenceRed = Math.abs(colorA.getRed() - colorB.getRed()) / 255.0f;
                float differenceGreen = Math.abs(colorA.getGreen() - colorB.getGreen()) / 255.0f;
                float differenceBlue = Math.abs(colorA.getBlue() - colorB.getBlue()) / 255.0f;

                float differenceForThisPixel = (differenceRed + differenceGreen + differenceBlue) / 3.0f;
                runningTotal += differenceForThisPixel;

                // Early return
                if ((runningTotal / (RESOLUTION * RESOLUTION)) >= SIMILARITY_THRESHOLD) {
                    return false;
                }
            }
        }
        return (runningTotal / (RESOLUTION * RESOLUTION)) < SIMILARITY_THRESHOLD;
    }

    /**
     * Return an image normalized for the resolution of the checker.
     *
     * see DuplicateImageChecker.RESOLUTION;
     */
    private static BufferedImage getNormalizedImage(BufferedImage image) {
        BufferedImage normalized = new BufferedImage(RESOLUTION, RESOLUTION, BufferedImage.TYPE_INT_RGB);
        Graphics gB = normalized.getGraphics();
        gB.drawImage(image.getScaledInstance(RESOLUTION, RESOLUTION, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        gB.dispose();

        return normalized;
    }

    /**
     * Notify all registered ProgressListener for the start of a check.
     */
    private synchronized void notifyOfStart() {
        for (ProgressListener l : listeners) {
            l.onStart();
        }
    }

    /**
     * Notify all registered ProgressListener for progress.
     */
    private synchronized void notifyOfProgress(double completion) {
        for (ProgressListener l : listeners) {
            l.onProgress(completion);
        }
    }

    /**
     * Notify all registered ProgressListener for the completion of the check.
     */
    private synchronized void notifyOfComplete() {
        for (ProgressListener l : listeners) {
            l.onComplete();
        }
    }

    /**
     * Notify all registered ProgressListener for change in the results.
     */
    private synchronized void notifyOfResultChange() {
        for (ProgressListener l : listeners) {
            l.onResultChange();
        }
    }
}
