package nl.yannickl88.imageview.view.util;

import java.awt.*;
import java.awt.image.ImageObserver;

public interface DrawableImage {
    void render(Graphics2D g2d, int x, int y, int width, int height, int totalWidth, int totalHeight, ImageObserver observer);
    void start();
    void dispose();
}
