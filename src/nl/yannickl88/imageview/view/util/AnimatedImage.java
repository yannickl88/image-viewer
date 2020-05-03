package nl.yannickl88.imageview.view.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class AnimatedImage implements DrawableImage {
    private final AnimationHandler handler;
    private GifDecoder decoder = null;
    private AnimationTimer timer = null;

    public interface AnimationHandler {
        void onUpdate();
    }

    private class AnimationTimer extends Thread {
        private static final int PLAYBACK_SPEED = 1000 / 30;

        private final GifDecoder decoder;
        private final int totalDelay;
        private boolean running = true;
        private long counter = 0;
        private int currentFrame = 0;

        public AnimationTimer(GifDecoder decoder) {
            this.decoder = decoder;

            int totalDelay = 0;
            for (int i = 0; i < decoder.getFrameCount(); i++) {
                totalDelay += decoder.getDelay(i);
            }

            this.totalDelay = totalDelay;
        }

        @Override
        public void run() {
            while (running) {
                counter = (counter + PLAYBACK_SPEED) % (totalDelay + 1);

                int frame = getFrameAtCurrentCounter();

                if (frame != currentFrame) {
                    currentFrame = frame;
                    handler.onUpdate();
                }

                try {
                    sleep(PLAYBACK_SPEED);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }

        public BufferedImage getDrawable() {
            return decoder.getFrame(currentFrame);
        }

        public int getFrameAtCurrentCounter() {
            int totalDelay = 0;
            for (int i = 0; i < decoder.getFrameCount(); i++) {
                if (totalDelay + decoder.getDelay(i) >= counter) {
                    return i;
                }
                totalDelay += decoder.getDelay(i);
            }

            return 0;
        }

        public void dispose() {
            running = false;
        }

        public double getProgress() {
            return counter / (double) totalDelay;
        }
    }

    public AnimatedImage(File file, AnimationHandler handler) {
        GifDecoder decoder = new GifDecoder();
        try {
            if (0 == decoder.read(new FileInputStream(file))) {
                this.decoder = decoder;
            }
        } catch (FileNotFoundException ignored) {
        }
        this.handler = handler;
    }

    @Override
    public void render(Graphics2D g2d, int x, int y, int width, int height, int totalWidth, int totalHeight, ImageObserver observer) {
        if (null != timer) {
            g2d.drawImage(timer.getDrawable(), x, y, width, height, observer);

            // Draw a timeline bar
            g2d.setColor(new Color(87, 133, 158, 128));
            g2d.fillRect(0, totalHeight - 5 , (int) Math.round(totalWidth * timer.getProgress()), 5);
        }
    }

    @Override
    public void start() {
        if (null != decoder) {
            timer = new AnimationTimer(decoder);
            timer.start();
        }
    }

    @Override
    public void dispose() {
        if (null != timer) {
            timer.dispose();

            try {
                timer.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
