package nl.yannickl88.imageview.view;

import nl.yannickl88.imageview.logging.Logger;
import nl.yannickl88.imageview.model.Image;
import nl.yannickl88.imageview.view.util.AnimatedImage;
import nl.yannickl88.imageview.view.util.DrawableImage;
import nl.yannickl88.imageview.view.util.StaticDrawableImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class ImageView extends JPanel implements Image.ImageChangeListener {
    private static final ZoomMode defaultZoomMode = ZoomMode.FIT;
    private Image image;
    private DrawableImage drawableImage;
    private ZoomMode zoomMode = defaultZoomMode;
    private ViewMode viewMode = ViewMode.HIDDEN, hoverMode = ViewMode.HIDDEN;
    private NavigationMode navigationMode = NavigationMode.HAS_NONE;

    public enum ZoomMode {
        FIT, FILL, ACTUAL
    }

    public enum ViewMode {
        HIDDEN, DETAILS
    }

    public enum NavigationMode {
        HAS_ALL, HAS_ONLY_NEXT, HAS_ONLY_PREV, HAS_NONE
    }

    public interface NavigationHandler {
        void onPrevious();
        void onNext();
        void onClose();
    }

    private class LoadImageTask implements Runnable {
        private final Image image;

        public LoadImageTask(Image image) {
            this.image = image;
        }

        @Override
        public void run() {
            try {
                DrawableImage drawable;

                if (image.metadata.path.endsWith(".gif")) {
                    drawable = new AnimatedImage(new File(image.metadata.path), () -> {
                        if (SwingUtilities.isEventDispatchThread()) {
                            repaint();
                        } else {
                            SwingUtilities.invokeLater(ImageView.this::repaint);
                        }
                    });
                } else {
                    drawable = new StaticDrawableImage(new File(image.metadata.path));
                }

                setAndValidateDrawableImage(image, drawable);
            } catch (Throwable e) {
                Logger.log(e);
            }
        }
    }

    public ImageView(NavigationHandler handler) {
        super();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != 1) {
                    return;
                }

                if (e.getY() < 100 && e.getX() > getWidth() - 100) {
                    handler.onClose();
                } else if (e.getX() < 100) {
                    handler.onPrevious();
                } else if (e.getX() > getWidth() - 100) {
                    handler.onNext();
                } else {
                    toggleViewMode();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == 4) {
                    handler.onClose();
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (e.getX() < 100 || e.getX() > getWidth() - 100) {
                    if (hoverMode != ViewMode.DETAILS) {
                        hoverMode = ViewMode.DETAILS;
                        repaint();
                    }
                } else {
                    if (hoverMode != ViewMode.HIDDEN) {
                        hoverMode = ViewMode.HIDDEN;
                        repaint();
                    }
                }

            }
        });
    }

    public void setImage(Image image) {
        if (image == null) {
            hoverMode = ViewMode.HIDDEN;
        }

        if (null != this.image) {
            this.image.removeChangeListener(this);
        }

        this.image = image;

        zoomMode = defaultZoomMode;

        if (null != image) {
            this.image.addChangeListener(this);
        }

        this.updateImageDrawable();
    }

    public void setZoomMode(ZoomMode mode) {
        zoomMode = mode;

        repaint();
    }

    public void toggleZoomMode() {
        ZoomMode mode;

        if (zoomMode == ZoomMode.FIT) {
            mode = ZoomMode.ACTUAL;
        } else if (zoomMode == ZoomMode.ACTUAL) {
            mode = ZoomMode.FILL;
        } else {
            mode = ZoomMode.FIT;
        }

        setZoomMode(mode);
    }

    public void setViewMode(ViewMode mode) {
        viewMode = mode;

        repaint();
    }

    public void toggleViewMode() {
        ViewMode mode;

        if (viewMode == ViewMode.DETAILS) {
            mode = ViewMode.HIDDEN;
        } else {
            mode = ViewMode.DETAILS;
        }

        setViewMode(mode);
    }

    public void setNavigationMode(NavigationMode mode) {
        navigationMode = mode;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        if (null != drawableImage) {
            double ratio = (double) image.metadata.width / (double) image.metadata.height;
            int width, height;

            if (zoomMode == ZoomMode.ACTUAL) {
                width = image.metadata.width;
                height = image.metadata.height;
            } else {
                width = getWidth();
                height = getHeight();
            }

            if (zoomMode != ZoomMode.ACTUAL) {
                if (ratio < 1.0) {
                    width = (int) (ratio * height);
                } else {
                    height = (int) (width / ratio);
                }

                if (zoomMode == ZoomMode.FIT) {
                    if (width > getWidth()) {
                        width = getWidth();
                        height = (int) (width / ratio);
                    }

                    if (height > getHeight()) {
                        height = getHeight();
                        width = (int) (ratio * height);
                    }
                }
            }

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            drawableImage.render(
                    g2d,
                    (this.getWidth() - width) / 2,
                    (this.getHeight() - height) / 2,
                    width,
                    height,
                    getWidth(),
                    getHeight(),
                    this
            );
        }
        if (viewMode == ViewMode.DETAILS && null != image) {
            g2d.setColor(new Color(0, 0, 0, 191));
            g2d.fillRect(0, getHeight() - 30, getWidth(), 30);

            int offset = getWidth() - 10;
            int height = getHeight() - 10;

            List<String> labels = image.metadata.labels;

            for (int i = labels.size() - 1; i >= 0; i--) {
                String label = labels.get(i);
                int labelWidth = g.getFontMetrics().stringWidth(label);
                Polygon labelPoints = new Polygon();

                labelPoints.addPoint(offset + 4, height + 4);
                labelPoints.addPoint(offset - labelWidth - 3, height + 4);
                labelPoints.addPoint(offset - labelWidth - 9, height - 3);
                labelPoints.addPoint(offset - labelWidth - 10, height - 5);
                labelPoints.addPoint(offset - labelWidth - 9, height - 7);
                labelPoints.addPoint(offset - labelWidth - 3, height - 13);
                labelPoints.addPoint(offset + 4, height - 13);

                g2d.setColor(new Color(62, 71, 75));
                g2d.fillPolygon(labelPoints);

                g2d.setColor(new Color(43, 46, 49));
                g2d.drawPolygon(labelPoints);

                g2d.setColor(new Color(255, 255, 255));
                g2d.drawString(label, offset - labelWidth, height);

                offset -= labelWidth + 20;
            }

            g2d.setColor(new Color(255, 255, 255));
            g2d.drawString(String.format("%d x %d px", image.metadata.width, image.metadata.height), 10, height);
        }

        if (hoverMode == ViewMode.DETAILS) {
            g2d.setColor(new Color(91, 95, 101));
            g2d.setStroke(new BasicStroke(5));

            // Draw X
            g2d.drawLine(getWidth() - 50, 50, getWidth() - 25, 25);
            g2d.drawLine(getWidth() - 50, 25, getWidth() - 25, 50);

            // Draw <
            int halfWay = getHeight() / 2;

            if (navigationMode == NavigationMode.HAS_ALL || navigationMode == NavigationMode.HAS_ONLY_PREV) {
                g2d.drawPolyline(new int[]{50, 25, 50}, new int[]{halfWay - 50, halfWay, halfWay + 50}, 3);
            }
            if (navigationMode == NavigationMode.HAS_ALL || navigationMode == NavigationMode.HAS_ONLY_NEXT) {
                g2d.drawPolyline(new int[]{getWidth() - 50, getWidth() - 25, getWidth() - 50}, new int[]{halfWay - 50, halfWay, halfWay + 50}, 3);
            }
        }
    }

    @Override
    public void onChange(Image image) {
        repaint();
    }

    private void updateImageDrawable() {
        if (null != image) {
            setDrawableImage(new StaticDrawableImage(image.thumb));
            (new Thread(new LoadImageTask(image))).start();
        } else {
            setDrawableImage(null);
        }
    }

    private void setDrawableImage(DrawableImage image) {
        if (null != drawableImage) {
            drawableImage.dispose();
        }

        drawableImage = image;

        if (null != drawableImage) {
            drawableImage.start();
        }

        if (SwingUtilities.isEventDispatchThread()) {
            repaint();
        } else {
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    private void setAndValidateDrawableImage(Image image, DrawableImage drawableImage) {
        if (image != this.image) {
            return;
        }

        if (SwingUtilities.isEventDispatchThread()) {
            setDrawableImage(drawableImage);
        } else {
            SwingUtilities.invokeLater(() -> setDrawableImage(drawableImage));
        }
    }
}
