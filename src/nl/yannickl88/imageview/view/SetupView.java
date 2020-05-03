package nl.yannickl88.imageview.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

public class SetupView extends JFrame {
    private MenuHandler menuHandler;

    public interface FolderSelectionHandler {
        void onSelect(File file);
    }

    public interface FileSelectionHandler {
        void onSelect(File file);
    }

    public interface MenuHandler {
        void onExit();
        void onSelectFolder();
        void onOpenFile();
    }

    public SetupView() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        JMenuItem menuFileClose = new JMenuItem("Quit", KeyEvent.VK_Q);
        menuFileClose.addActionListener(e -> menuHandler.onExit());
        menu.add(menuFileClose);

        menuBar.add(menu);

        setJMenuBar(menuBar);

        setTitle("Library Set-up Wizard");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(50, 0, 50, 0));
        mainPanel.setPreferredSize(new Dimension(600, 150));

        JLabel label = new JLabel("In order to start, first select the root folder of your collection or open an existing collection.");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(label);

        JButton folderButton = new JButton("Select Folder");
        folderButton.addActionListener(e -> menuHandler.onSelectFolder());
        folderButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton openButton = new JButton("Open Existing");
        openButton.addActionListener(e -> menuHandler.onOpenFile());
        openButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(folderButton);
        buttonPanel.add(openButton);

        mainPanel.add(buttonPanel);

        add(mainPanel);
    }

    public void setMenuHandler(MenuHandler handler) {
        menuHandler = handler;
    }

    public void openFolderChooser(FolderSelectionHandler handler) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Choose a folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            handler.onSelect(chooser.getSelectedFile());
        }
    }

    public void openFileChooser(FileSelectionHandler handler) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Choose a collection");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("Image collection", "icol"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            handler.onSelect(chooser.getSelectedFile());
        }
    }

    public void open() {
        this.pack();
        this.setVisible(true);
    }

    public void close() {
        this.setVisible(false);
        this.dispose();
    }
}
