package nl.yannickl88.imageview.view;

import nl.yannickl88.imageview.image.DuplicateImageChecker;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DuplicateImagesView extends JDialog {
    private final JProgressBar progressBar;
    private final JButton startButton;
    private final JPanel items;
    private ActionHandler handler;

    public interface ActionHandler {
        void onStart();
        void onMerge(DuplicateImageChecker.Duplicate images);
    }

    public DuplicateImagesView(Frame owner) {
        super(owner, "Duplicate Images", true);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 500));

        JPanel topPanel = new JPanel(new BorderLayout());

        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        startButton = new JButton("Start");
        startButton.addActionListener(e -> handler.onStart());

        topPanel.add(progressBar, BorderLayout.CENTER);
        topPanel.add(startButton, BorderLayout.EAST);

        items = new JPanel(new GridLayout(0, 1));

        add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(items);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void setHandler(ActionHandler handler) {
        this.handler = handler;
    }

    public void open() {
        pack();
        setVisible(true);
    }

    public void setProgress(double completion) {
        int percentage = (int) Math.round(completion * 100);
        progressBar.setValue(percentage);
        progressBar.setString(String.format("%d%%", percentage));
    }

    public void setStarted(boolean started) {
        this.startButton.setEnabled(!started);
    }

    public void updateResults(List<DuplicateImageChecker.Duplicate> results) {
        items.removeAll();

        for (DuplicateImageChecker.Duplicate r : results) {
            JPanel row = new JPanel(new BorderLayout());
            JButton button = new JButton("Merge");
            button.addActionListener(e -> handler.onMerge(r));
            row.add(button, BorderLayout.WEST);
            row.add(new DuplicatesView(r.duplicates), BorderLayout.CENTER);

            items.add(row);
        }

        items.revalidate();
        revalidate();
        repaint();
    }
}
