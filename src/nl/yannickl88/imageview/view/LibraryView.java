package nl.yannickl88.imageview.view;

import nl.yannickl88.imageview.controller.LibraryController;
import nl.yannickl88.imageview.image.TransferableImage;
import nl.yannickl88.imageview.model.Image;
import nl.yannickl88.imageview.view.input.LabelInputField;
import nl.yannickl88.imageview.view.input.SearchField;
import nl.yannickl88.imageview.view.layout.ColumnLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class LibraryView extends JFrame implements ClipboardOwner {
    private final JPanel overviewPanel;
    private final JPanel mainPanel;
    private final JPanel searchPanel;
    private final ImageView imagePanel;
    private final HashMap<Image, ImageThumbView> thumbCache;
    private boolean isSaved;
    private boolean isFocused = false;
    private NavigationHandler navigationHandler;
    private MenuHandler menuHandler;
    private LabelView labelEditor;

    private final JLabel status;
    private final JMenuItem menuEditCopy, menuEditDelete, menuEditLabel;

    public interface LabelSelectHandler {
        void onSelect(Set<String> labels);
    }

    public interface NavigationHandler {
        void onFirst();
        void onPrevious();
        void onNext();
        void onLast();

        void onClose();
        void onSearch(String query);
        void onFocus(Image image);
        void onZoomToggle();
        void onExit();
    }

    public interface MenuHandler {
        void onNew();
        void onOpen();
        void onSave();
        void onLabelManage();
        void onFindDuplicates();
        void onDelete();
        void onCopy();
        void onLabel();
    }

    public interface SaveFileHandler {
        void onSave(File file);
    }

    public interface LabelChangeHandler {
        void onLabelRemove(String label);

        void onLabelRename(String oldLabel, String newLabel);
    }

    public interface NewFileHandler {
        void onOpen(File folder);
    }

    public interface OpenFileHandler {
        void onOpen(File file);
    }

    public interface DeleteImageHandler {
        void onDelete(Image image);
    }

    public LibraryView(boolean isSaved) {
        this.isSaved = isSaved;

        thumbCache = new HashMap<>();

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem menuFileNew = new JMenuItem("New", KeyEvent.VK_N);
        JMenuItem menuFileOpen = new JMenuItem("Open", KeyEvent.VK_O);
        JMenuItem menuFileSave = new JMenuItem("Save", KeyEvent.VK_S);
        JMenuItem menuFileClose = new JMenuItem("Quit", KeyEvent.VK_Q);

        menuFileNew.addActionListener(e -> menuHandler.onNew());
        menuFileOpen.addActionListener(e -> menuHandler.onOpen());
        menuFileSave.addActionListener(e -> menuHandler.onSave());
        menuFileClose.addActionListener(e -> navigationHandler.onExit());

        fileMenu.add(menuFileNew);
        fileMenu.add(menuFileOpen);
        fileMenu.add(menuFileSave);
        fileMenu.addSeparator();
        fileMenu.add(menuFileClose);

        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");

        menuEditCopy = new JMenuItem("Copy", KeyEvent.VK_C);
        menuEditCopy.addActionListener(e -> menuHandler.onCopy());

        menuEditDelete = new JMenuItem("Delete", KeyEvent.VK_D);
        menuEditDelete.addActionListener(e -> menuHandler.onDelete());

        menuEditLabel = new JMenuItem("Label", KeyEvent.VK_L);
        menuEditLabel.addActionListener(e -> menuHandler.onLabel());

        // Disable on default
        menuEditCopy.setEnabled(false);
        menuEditDelete.setEnabled(false);
        menuEditLabel.setEnabled(false);

        editMenu.add(menuEditCopy);
        editMenu.add(menuEditDelete);
        editMenu.addSeparator();
        editMenu.add(menuEditLabel);

        menuBar.add(editMenu);

        JMenu labelMenu = new JMenu("Labels");

        JMenuItem menuLabelsManage = new JMenuItem("Manage", KeyEvent.VK_M);
        menuLabelsManage.addActionListener(e -> menuHandler.onLabelManage());

        labelMenu.add(menuLabelsManage);

        menuBar.add(labelMenu);

        JMenu imagesMenu = new JMenu("Image");
        JMenuItem menuImagesDuplicates = new JMenuItem("Find All Duplicate", KeyEvent.VK_D);
        menuImagesDuplicates.addActionListener(e -> menuHandler.onFindDuplicates());

        imagesMenu.add(menuImagesDuplicates);

        menuBar.add(imagesMenu);

        setJMenuBar(menuBar);

        SearchField searchField = new SearchField(new SearchField.ActionHandler() {
            @Override
            public void onSearch(String query) {
                navigationHandler.onSearch(query);
            }

            @Override
            public void onCancel() {
                LibraryView.this.requestFocus();
                searchPanel.setVisible(false);
            }
        });
        JButton clearButton = new JButton("x");
        clearButton.addActionListener(e -> searchField.clear());

        searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(clearButton, BorderLayout.EAST);

        searchPanel.setVisible(false);

        overviewPanel = new JPanel();
        overviewPanel.setAutoscrolls(true);

        imagePanel = new ImageView(new ImageView.NavigationHandler() {
            @Override
            public void onPrevious() {
                navigationHandler.onPrevious();
            }

            @Override
            public void onNext() {
                navigationHandler.onNext();
            }

            @Override
            public void onClose() {
                navigationHandler.onClose();
            }
        });

        status = new JLabel("");
        status.setPreferredSize(new Dimension(800, 20));

        JScrollPane scrollPane = new JScrollPane(
                this.overviewPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                overviewPanel.revalidate();
            }
        });

        overviewPanel.setLayout(new ColumnLayout(scrollPane.getViewport(), ImageThumbView.SIZE));

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(status, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(searchPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 37) { // arrow left
                    navigationHandler.onPrevious();
                } else if (e.getKeyCode() == 39) { // arrow right
                    navigationHandler.onNext();
                } else if (isFocused && e.getKeyCode() == 90) { // Z
                    navigationHandler.onZoomToggle();
                } else if (e.getKeyCode() == 27) { // ESC
                    navigationHandler.onFocus(null);
                } else if (e.getKeyCode() == 36) { // HOME
                    navigationHandler.onFirst();
                } else if (e.getKeyCode() == 35) { // END
                    navigationHandler.onLast();
                } else if (e.getKeyCode() == 127) { // DEL
                    menuHandler.onDelete();
                }

                if (!isFocused && String.valueOf(e.getKeyChar()).matches("^[a-z0-9-]+$")) {
                    searchPanel.setVisible(true);
                    searchField.grabFocus();

                    // delegate the event
                    try {
                        (new Robot()).keyPress(e.getExtendedKeyCode());
                    } catch (AWTException ignored) {
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (isFocused && e.getKeyCode() == 76) { // L
                    menuHandler.onLabel();
                }
            }
        });

        setPreferredSize(new Dimension(800, 600));
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (LibraryView.this.isSaved) {
                    navigationHandler.onExit();
                    return;
                }

                int confirmed = JOptionPane.showConfirmDialog(
                        null,
                        "The collection is not yet saved, are you sure you want to exit? All data will be lost.",
                        "Are you sure?",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirmed == JOptionPane.YES_OPTION) {
                    navigationHandler.onExit();
                } else {
                    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                }
            }
        });

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void setNavigationHandler(NavigationHandler navigationHandler) {
        this.navigationHandler = navigationHandler;
    }

    public void setMenuHandler(MenuHandler menuHandler) {
        this.menuHandler = menuHandler;
    }

    public void setSaved(boolean saved) {
        this.isSaved = saved;
    }

    public void setImages(List<Image> images) {
        overviewPanel.removeAll();

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setImages(images));
            return;
        }

        for (Image i : images) {
            ImageThumbView view;
            if (thumbCache.containsKey(i)) {
                view = thumbCache.get(i);
            } else {
                view = new ImageThumbView(i, image -> {
                    LibraryView.this.requestFocus();

                    navigationHandler.onFocus(image);
                });

                thumbCache.put(i, view);
            }

            overviewPanel.add(view);
        }

        overviewPanel.revalidate();
    }

    public void setActiveImage(Image image) {
        if (null != image) {
            remove(mainPanel);
            add(imagePanel);

            if (thumbCache.containsKey(image)) {
                overviewPanel.scrollRectToVisible(thumbCache.get(image).getBounds());
            }
        } else {
            remove(imagePanel);
            add(mainPanel);
        }

        isFocused = null != image;

        menuEditCopy.setEnabled(isFocused);
        menuEditDelete.setEnabled(isFocused);
        menuEditLabel.setEnabled(isFocused);
        imagePanel.setImage(image);

        revalidate();
        repaint();
    }

    public void setNavigationMode(ImageView.NavigationMode mode) {
        imagePanel.setNavigationMode(mode);
    }

    public void setVisibleItems(Set<Image> images) {
        for (ImageThumbView i : thumbCache.values()) {
            i.setVisible(images.contains(i.getImage()));
        }
    }

    public void setAllVisible() {
        for (ImageThumbView i : thumbCache.values()) {
            i.setVisible(true);
        }
    }

    public void setAllLabels(Set<String> labels) {
        if (null != labelEditor) {
            labelEditor.setLabels(labels);
        }
    }

    public void setStatusText(String status) {
        this.status.setText(status);
    }

    public void setViewMode(ImageView.ViewMode mode) {
        imagePanel.setViewMode(mode);
    }

    public void toggleZoomMode() {
        imagePanel.toggleZoomMode();
    }

    public void open() {
        pack();
        setVisible(true);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public void openLabelSelector(List<String> labels, Set<String> choices, LabelSelectHandler handler) {
        JDialog dialog = new JDialog(this, "Edit labels", true);
        dialog.setLayout(new BorderLayout());
        dialog.setPreferredSize(new Dimension(300, 100));

        LabelInputField textArea = new LabelInputField(labels, choices, new LabelInputField.ActionHandler() {
            @Override
            public void onCommit(Set<String> labels) {
                handler.onSelect(labels);

                // Close the dialog
                onCancel();
            }

            @Override
            public void onCancel() {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });


        dialog.add(textArea, BorderLayout.CENTER);
        dialog.pack();
        dialog.setVisible(true);
    }

    public void openSaveChooser(SaveFileHandler handler) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save collection");
        chooser.setFileFilter(new FileNameExtensionFilter("Image collection", "icol"));

        int userSelection = chooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            handler.onSave(chooser.getSelectedFile());
        }
    }

    public void openLabelManager(Set<String> labels, LabelChangeHandler handler) {
        JDialog dialog = new JDialog(this, "Manage labels", true);
        dialog.add(labelEditor = new LabelView(labels, new LabelView.ActionHandler() {
            @Override
            public void onLabelRemove(String label) {
                handler.onLabelRemove(label);
            }

            @Override
            public void onLabelRename(String oldLabel, String newLabel) {
                handler.onLabelRename(oldLabel, newLabel);
            }
        }));
        dialog.pack();
        dialog.setVisible(true);
    }

    public void openFolderChooser(NewFileHandler handler) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Choose a folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            handler.onOpen(chooser.getSelectedFile());
        }
    }

    public void openFileChooser(OpenFileHandler handler) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Choose a collection");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("Image collection", "icol"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            handler.onOpen(chooser.getSelectedFile());
        }
    }

    public void openDeleteConfirm(Image image, DeleteImageHandler handler) {
        int confirmed = JOptionPane.showConfirmDialog(
                this,
                "Deleting a file cannot be undone, are you sure?",
                "Delete " + image.metadata.name,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirmed == JOptionPane.YES_OPTION) {
            handler.onDelete(image);
        }
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        System.out.println("lostOwnership");
    }

    public void copyImage(Image image) {
        Thread thread = new Thread(() -> {
            try {
                TransferableImage trans = new TransferableImage(ImageIO.read(new File(image.metadata.path)));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trans, LibraryView.this);
            } catch (IOException ignored) {
            }
        });
        thread.start();
    }
}
