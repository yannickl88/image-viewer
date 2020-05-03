package nl.yannickl88.imageview.view;

import nl.yannickl88.imageview.view.input.LabelField;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Set;

public class LabelView extends JPanel {
    private final JPanel mainPanel;
    private final ActionHandler handler;

    public interface ActionHandler {
        void onLabelRemove(String label);
        void onLabelRename(String oldLabel, String newLabel);
    }

    public LabelView(Set<String> labels, ActionHandler handler) {
        super();
        this.handler = handler;

        setLayout(new BorderLayout());

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        setLabels(labels);

        mainPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setPreferredSize(new Dimension(400, 600));

        add(scrollPane, BorderLayout.CENTER);
    }

    public void setLabels(Set<String> labels) {
        ArrayList<String> sortedLabels = new ArrayList<>(labels);
        sortedLabels.sort(String::compareTo);

        mainPanel.removeAll();

        for (String l : sortedLabels) {
            JPanel labelPanel = new JPanel(new BorderLayout());
            JPanel actionPanel = new JPanel(new FlowLayout());

            LabelField label = new LabelField(l);
            JButton rename = new JButton("Rename");

            label.addActionHandler(() -> rename.setEnabled(label.hasChanged()));
            rename.addActionListener(e -> handler.onLabelRename(l, label.getText()));

            rename.setEnabled(false);
            actionPanel.add(rename);

            JButton delete = new JButton("Delete");
            delete.addActionListener(e -> handler.onLabelRemove(l));
            actionPanel.add(delete);

            labelPanel.add(label, BorderLayout.CENTER);
            labelPanel.add(actionPanel, BorderLayout.EAST);
            labelPanel.setMaximumSize(new Dimension(400, 35));

            mainPanel.add(labelPanel);
        }

        mainPanel.revalidate();
        revalidate();
        repaint();
    }
}
