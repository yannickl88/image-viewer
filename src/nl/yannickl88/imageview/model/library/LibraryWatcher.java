package nl.yannickl88.imageview.model.library;

import nl.yannickl88.imageview.model.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

public class LibraryWatcher extends Thread {
    private static final String[] SUPPORTED_EXTENSIONS = new String[]{".gif", ".jpg", ".jpeg", ".png"};

    private final Library library;
    private final ArrayList<LibraryWatcherListener> listeners;
    private boolean running = true;

    public interface LibraryWatcherListener {
        void onStatusChange(String status);
    }

    public LibraryWatcher(Library library) {
        this.library = library;
        listeners = new ArrayList<>();
    }

    public void addChangeListener(LibraryWatcherListener listener) {
        listeners.add(listener);
    }

    @Override
    public void run() {
        while (running) {
            if (library.isLoaded()) {
                HashSet<String> files = getAllFiles();

                ArrayList<Image> filesToRemove = new ArrayList<>();
                ArrayList<String> filesToAdd = new ArrayList<>();

                // Check if there is a file missing
                for (String f : files) {
                    if (!library.contains(f)) {
                        filesToAdd.add(f);
                    }
                }

                // Check if there is a file too many
                for (Image i : library.getImages()) {
                    if (!files.contains(i.metadata.path)) {
                        filesToRemove.add(i);
                    }
                }
                int addCount = filesToAdd.size();
                int deleteCount = filesToRemove.size();

                for (String f : filesToAdd) {
                    try {
                        library.add(new Image(new File(f)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    addCount--;

                    notifyStatusChange(String.format("Processing changes, found %d new items and %d to be removed", addCount, deleteCount));
                }
                for (Image f : filesToRemove) {
                    library.remove(f);

                    deleteCount--;

                    notifyStatusChange(String.format("Processing changes, found %d new items and %d to be removed", addCount, deleteCount));
                }

                if (filesToAdd.size() + filesToRemove.size() > 0) {
                    notifyStatusChange("Saving library");
                    library.save();
                }
                notifyStatusChange("");
            }

            try {
                for (int i = 0; i < 10; i++) {
                    if (running) {
                        sleep(100);
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void terminate() {
        running = false;
    }

    private HashSet<String> getAllFiles() {
        HashSet<String> files = new HashSet<>();
        File dir = library.getDir();

        for (String f : dir.list()) {
            if (!hasExtension(f)) {
                continue;
            }

            files.add(Paths.get(dir.getAbsolutePath(), f).toString());
        }

        return files;
    }

    private static boolean hasExtension(String file) {
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (file.toLowerCase().endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    private void notifyStatusChange(String status) {
        for (LibraryWatcherListener l : listeners) {
            l.onStatusChange(status);
        }
    }
}
