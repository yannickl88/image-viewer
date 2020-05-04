package nl.yannickl88.imageview.model;

import nl.yannickl88.imageview.model.library.Library;
import nl.yannickl88.imageview.model.library.LibraryWatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class Model {
    private final Library library;
    private final ArrayList<ModelChangeListener> listeners;
    private final ReentrantLock lock;
    private LibraryWatcher watcher;
    private final ArrayList<Image> images = new ArrayList<>();

    public interface ModelChangeListener {
        void onLibraryChange(List<Image> images);
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

            notifyLibraryChange(newImages);
        });
    }

    public void addChangeListener(ModelChangeListener listener) {
        lock.lock();

        try {
            listeners.add(listener);
        } finally {
            lock.unlock();
        }
    }

    public File getLibraryDir() {
        return library.getDir();
    }

    public void deleteImage(Image image) {
        library.delete(image);
    }

    public List<Image> getAllImages() {
        return new ArrayList<>(images);
    }

    public Set<String> getLabels() {
        return library.getAllLabels();
    }

    public boolean isPersistent() {
        return library.isPersistent();
    }

    public void setConfigFile(File file) {
        library.setConfigFile(file);
    }

    public void startWatcher() {
        watcher = new LibraryWatcher(this.library);
        watcher.addChangeListener(this::notifyLibraryWatcherStatusChange);
        watcher.start();
    }

    private synchronized void notifyLibraryChange(List<Image> images) {
        for (ModelChangeListener l : listeners) {
            l.onLibraryChange(images);
        }
    }

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
