package nl.yannickl88.imageview.view.util;

import nl.yannickl88.imageview.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;

public class StaticDrawableImage implements DrawableImage {
    private final BufferedImage drawable;

    public StaticDrawableImage(File file) throws IOException {
        this.drawable = ImageIO.read(file);
    }

    public StaticDrawableImage(BufferedImage image) {
        this.drawable = image;
    }

    @Override
    public void render(Graphics2D g2d, int x, int y, int width, int height, int totalWidth, int totalHeight, ImageObserver observer) {
        g2d.drawImage(this.drawable, x, y, width, height, observer);
    }

    @Override
    public void start() {

    }

    @Override
    public void dispose() {

    }
}
