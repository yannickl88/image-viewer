package nl.yannickl88.imageview.view;

import nl.yannickl88.imageview.model.Image;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class DuplicatesView extends JPanel {
    private final Set<Image> images;

    public DuplicatesView(Set<Image> images) {
        super();
        this.images = images;

        int width = 0;

        for (Image i : images) {
            width += i.thumb.getWidth() + 3;
        }

        this.setPreferredSize(new Dimension(width, Image.THUMB_SIZE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        int offset = 0;

        for (Image i : images) {
            g2d.drawImage(i.thumb, offset, 0, this);
            offset += i.thumb.getWidth() + 3;
        }
    }
}
