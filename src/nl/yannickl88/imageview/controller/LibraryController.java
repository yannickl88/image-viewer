package nl.yannickl88.imageview.controller;

import nl.yannickl88.imageview.image.TransferableImage;
import nl.yannickl88.imageview.model.Image;
import nl.yannickl88.imageview.model.Model;
import nl.yannickl88.imageview.model.library.Library;
import nl.yannickl88.imageview.search.SearchMatcher;
import nl.yannickl88.imageview.view.DuplicateImagesView;
import nl.yannickl88.imageview.view.ImageView;
import nl.yannickl88.imageview.view.LibraryView;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LibraryController implements ClipboardOwner {
    private final Model model;
    private final LibraryView view;
    private final OpenHandler handler;

    private Image activeImage = null;
    private final ArrayList<Image> visible = new ArrayList<>();

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        System.out.println("lostOwnership");
    }

    public interface OpenHandler {
        void onOpen(Library library);
    }

    public LibraryController(Model model, LibraryView view, OpenHandler handler) {
        this.model = model;
        this.view = view;
        this.handler = handler;

        updateApplicationTitle();

        this.view.setNavigationHandler(new LibraryView.NavigationHandler() {
            @Override
            public void onFirst() {
                firstActiveImage();
            }

            @Override
            public void onPrevious() {
                previousActiveImage();
            }

            @Override
            public void onNext() {
                nextActiveImage();
            }

            @Override
            public void onLast() {
                lastActiveImage();
            }

            @Override
            public void onClose() {
                setActiveImage(null);
            }

            @Override
            public void onSearch(String query) {
                updateMatchingImages(query);
            }

            @Override
            public void onFocus(Image image) {
                setActiveImage(image);
            }

            @Override
            public void onExit() {
                close();
            }

            @Override
            public void onZoomToggle() {
                if (null != activeImage) {
                    view.toggleZoomMode();
                }
            }
        });
        this.view.setMenuHandler(new LibraryView.MenuHandler() {
            @Override
            public void onNew() {
                view.openFolderChooser(folder -> closeAndOpenLibrary(Library.init(folder)));
            }

            @Override
            public void onOpen() {
                view.openFileChooser(file -> closeAndOpenLibrary(Library.open(file)));
            }

            @Override
            public void onSave() {
                save();
            }

            @Override
            public void onLabelManage() {
                view.openLabelManager(model.getLabels(), new LibraryView.LabelChangeHandler() {
                    @Override
                    public void onLabelRemove(String label) {
                        for (Image image : model.getAllImages()) {
                            HashSet<String> newLabels = new HashSet<>(image.metadata.labels);

                            if (newLabels.remove(label)) {
                                image.metadata.setLabels(newLabels);
                            }
                        }

                        view.setAllLabels(model.getLabels());
                    }

                    @Override
                    public void onLabelRename(String oldLabel, String newLabel) {
                        for (Image image : model.getAllImages()) {
                            HashSet<String> newLabels = new HashSet<>(image.metadata.labels);

                            if (newLabels.remove(oldLabel)) {
                                newLabels.add(newLabel);
                                image.metadata.setLabels(newLabels);
                            }
                        }

                        view.setAllLabels(model.getLabels());
                    }
                });
            }

            @Override
            public void onFindDuplicates() {
                new DuplicateImageController(model, new DuplicateImagesView(view));
            }

            @Override
            public void onDelete() {
                if (null != activeImage) {
                    view.openDeleteConfirm(activeImage, image -> {
                        List<Image> images = model.getAllImages();
                        int index = images.indexOf(image);

                        // Last one? back to the index
                        if (images.size() == 1) {
                            setActiveImage(null);
                        } else if (index + 1 < images.size()) { // Pick the next, if possible
                            setActiveImage(images.get(index + 1));
                        } else {
                            setActiveImage(images.get(index - 1)); // else the previous
                        }

                        model.deleteImage(image);
                    });
                }
            }

            @Override
            public void onCopy() {
                copyActiveImage();
            }

            @Override
            public void onLabel() {
                if (null != activeImage) {
                    view.setViewMode(ImageView.ViewMode.DETAILS);
                    view.openLabelSelector(
                            activeImage.metadata.labels,
                            model.getLabels(),
                            labels -> activeImage.metadata.setLabels(labels)
                    );
                }
            }
        });

        model.addChangeListener(new Model.ModelChangeListener() {
            @Override
            public void onLibraryChange(List<Image> images) {
                if (SwingUtilities.isEventDispatchThread()) {
                    updateImages(images);
                } else {
                    SwingUtilities.invokeLater(() -> updateImages(images));
                }
            }

            @Override
            public void onLibraryWatcherStatusChange(String status) {
                if (SwingUtilities.isEventDispatchThread()) {
                    updateApplicationStatus(status);
                } else {
                    SwingUtilities.invokeLater(() -> updateApplicationStatus(status));
                }
            }
        });

        view.setImages(model.getAllImages());
        model.startWatcher();
        view.open();
    }

    private void updateMatchingImages(String query) {
        visible.clear();
        query = query.trim();

        if (query.length() == 0) {
            view.setAllVisible();
            updateNavigationMode();
            return;
        }

        for (Image i : model.getAllImages()) {
            if (SearchMatcher.matches(i, query)) {
                visible.add(i);
            }
        }

        view.setVisibleItems(new HashSet<>(visible));
        updateNavigationMode();
    }

    private void updateImages(List<Image> images) {
        if (null != activeImage && !images.contains(activeImage)) {
            setActiveImage(null);
        }

        view.setImages(new ArrayList<>(images));
    }

    private void updateApplicationStatus(String status) {
        view.setStatusText(status);
    }

    private void setActiveImage(Image image) {
        activeImage = image;

        view.setActiveImage(image);

        updateNavigationMode();
        updateApplicationTitle();
    }

    private void updateNavigationMode() {
        ImageView.NavigationMode navigationMode = ImageView.NavigationMode.HAS_NONE;

        if (null != activeImage) {
            List<Image> images = getCurrentImageList();
            int index = images.indexOf(activeImage);

            if (index == 0) {
                navigationMode = ImageView.NavigationMode.HAS_ONLY_NEXT;
            } else if (index == images.size() - 1) {
                navigationMode = ImageView.NavigationMode.HAS_ONLY_PREV;
            } else {
                navigationMode = ImageView.NavigationMode.HAS_ALL;
            }
        }

        view.setNavigationMode(navigationMode);
    }

    private void updateApplicationTitle() {
        if (null != activeImage) {
            view.setTitle(String.format("Image library - %s", activeImage.metadata.path));
        } else {
            view.setTitle(String.format("Image library - %s", model.getLibraryDir().getAbsolutePath()));
        }
    }

    private void lastActiveImage() {
        if (null == activeImage) {
            return;
        }

        List<Image> images = getCurrentImageList();

        this.setActiveImage(images.get(images.size() - 1));
    }

    private void nextActiveImage() {
        if (null == activeImage) {
            return;
        }

        List<Image> images = getCurrentImageList();
        int index = images.indexOf(activeImage);

        if (index + 1 < images.size()) {
            this.setActiveImage(images.get(index + 1));
        }
    }

    private void previousActiveImage() {
        if (null == activeImage) {
            return;
        }

        List<Image> images = getCurrentImageList();
        int index = images.indexOf(activeImage);

        if (index > 0) {
            this.setActiveImage(images.get(index - 1));
        }
    }

    private void firstActiveImage() {
        if (null == activeImage) {
            return;
        }

        this.setActiveImage(getCurrentImageList().get(0));
    }

    private void copyActiveImage() {
        if (null == activeImage) {
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                TransferableImage trans = new TransferableImage(ImageIO.read(new File(activeImage.metadata.path)));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trans, LibraryController.this);
            } catch (IOException ignored) {
            }
        });
        thread.start();
    }

    public void save() {
        view.setSaved(true);

        if (model.isPersistent()) {
            return;
        }

        view.openSaveChooser(file -> {
            if (!file.getAbsolutePath().endsWith(".icol")) {
                file = new File(file.getAbsolutePath() + ".icol");
            }

            model.setConfigFile(file);
        });
    }

    public void close() {
        view.dispose();
        System.exit(0);
    }

    private List<Image> getCurrentImageList() {
        if (visible.size() == 0) {
            return model.getAllImages();
        }
        return visible;
    }

    private void closeAndOpenLibrary(Library library) {
        model.dispose();
        view.setVisible(false);
        view.dispose();

        // Open library view
        handler.onOpen(library);
    }
}
