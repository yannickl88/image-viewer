package nl.yannickl88.imageview.model;

import nl.yannickl88.imageview.model.library.Library;
import nl.yannickl88.imageview.model.library.LibraryWatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Application model which handles all the interaction with the library.
 */
public class Model {
    private final Library library;
    private final ArrayList<ModelChangeListener> listeners;
    private final ReentrantLock lock;
    private LibraryWatcher watcher;
    private final ArrayList<Image> images = new ArrayList<>();

    public interface ModelChangeListener {
        /**
         * Triggers when an images is added, removed or updated in the library.
         */
        void onLibraryChange(List<Image> images);

        /**
         * Triggers when the library watcher changes status.
         */
        void onLibraryWatcherStatusChange(String status);
    }

    public Model(Library library) {
        this.library = library;
        lock = new ReentrantLock();
        listeners = new ArrayList<>();

        this.library.addChangeListener(newImages -> {
            lock.lock();

            try {
                this.images.clear();
                this.images.addAll(newImages);
                this.images.sort((o1, o2) -> Long.compare(o2.metadata.ctime, o1.metadata.ctime));
            } finally {
                lock.unlock();
            }

            notifyLibraryChange(this.images);
        });
    }

    /**
     * Register a change listener for the model.
     */
    public void addChangeListener(ModelChangeListener listener) {
        lock.lock();

        try {
            listeners.add(listener);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the directory the library is associated with on the file system.
     */
    public File getLibraryDir() {
        return library.getDir();
    }

    /**
     * Delete an image from the library and from disk.
     *
     * NOTE: also the file is permanently removed.
     */
    public void deleteImage(Image image) {
        library.delete(image);
    }

    /**
     * Return all images in the library in a sorted order.
     */
    public List<Image> getAllImages() {
        return new ArrayList<>(images);
    }

    /**
     * Return all labels used by any of the images in the library.
     */
    public Set<String> getLabels() {
        return library.getAllLabels();
    }

    /**
     * Check if the library is saved.
     */
    public boolean isPersistent() {
        return library.isPersistent();
    }

    /**
     * Set the file to use for saving the library.
     */
    public void setConfigFile(File file) {
        library.setConfigFile(file);
    }

    /**
     * Start the watcher for library changes.
     */
    public void startWatcher() {
        if (null != watcher) {
            return; // Watcher already started.
        }

        watcher = new LibraryWatcher(this.library);
        watcher.addChangeListener(this::notifyLibraryWatcherStatusChange);
        watcher.start();
    }

    /**
     * Notify all registered ModelChangeListeners for changes in the library.
     */
    private void notifyLibraryChange(List<Image> images) {
        lock.lock();

        try {
            for (ModelChangeListener l : listeners) {
                l.onLibraryChange(images);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Notify all registered ModelChangeListeners for changes in the library.
     */
    private void notifyLibraryWatcherStatusChange(String status) {
        lock.lock();

        try {
            for (ModelChangeListener l : listeners) {
                l.onLibraryWatcherStatusChange(status);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Dispose of the model. This cleans up any watchers running.
     */
    public void dispose() {
        if (null != watcher) {
            watcher.terminate();

            try {
                watcher.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        library.save();
        library.dispose();
        listeners.clear();
    }
}
