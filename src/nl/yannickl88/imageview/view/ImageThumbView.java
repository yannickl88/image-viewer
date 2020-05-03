package nl.yannickl88.imageview.view;

import nl.yannickl88.imageview.model.Image;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ImageThumbView extends JPanel {
    public static final int SIZE = Image.THUMB_SIZE + 4;

    private final Image image;
    private boolean hovered = false;
    private boolean inSelectRange = false;

    public interface SelectionHandler {
        void onClick(Image image);
    }

    public ImageThumbView(Image image, SelectionHandler handler) {
        super();
        this.image = image;

        image.addChangeListener(image1 -> repaint());
        setPreferredSize(new Dimension(SIZE, SIZE));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != 1) {
                    return;
                }

                hovered = false;
                handler.onClick(image);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int offsetX = (int) (getWidth() / 2.0 - SIZE / 2.0);
                int offsetY = (int) (getHeight() / 2.0 - SIZE / 2.0);

                boolean inRange = e.getX() > offsetX && e.getX() < offsetX + 25 && e.getY() > offsetY && e.getY() < offsetY + 25;

                if (inRange != inSelectRange) {
                    inSelectRange = inRange;
                    repaint();
                }
            }
        });
    }

    public Image getImage() {
        return image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        int offsetX = (int) (getWidth() / 2.0 - SIZE / 2.0);
        int offsetY = (int) (getHeight() / 2.0 - SIZE / 2.0);

        if (hovered) {
            g2d.setColor(new Color(183, 208, 219));
            g2d.fillRect(offsetX, offsetY, SIZE, SIZE);
        }

        g2d.drawImage(
                image.thumb,
                (Image.THUMB_SIZE - image.thumb.getWidth()) / 2 + 2 + offsetX,
                (Image.THUMB_SIZE - image.thumb.getHeight()) / 2 + 2 + offsetY,
                this
        );

        if (image.metadata.labels.size() > 0 && hovered) {
            Polygon labelPoints = new Polygon();

            labelPoints.addPoint(SIZE - 5 + offsetX, SIZE - 5 + offsetY);
            labelPoints.addPoint(SIZE - 17 + offsetX, SIZE - 5 + offsetY);
            labelPoints.addPoint(SIZE - 20 + offsetX, SIZE - 10 + offsetY);
            labelPoints.addPoint(SIZE - 17 + offsetX, SIZE - 15 + offsetY);
            labelPoints.addPoint(SIZE - 5 + offsetX, SIZE - 15 + offsetY);

            g2d.setColor(new Color(62, 71, 75));
            g2d.fillPolygon(labelPoints);

            g2d.setColor(new Color(201, 211, 212));
            g2d.setFont(new Font(g2d.getFont().getFontName(), Font.PLAIN, 9));

            int count = image.metadata.labels.size();

            if (count > 9) {
                g2d.drawString("9", SIZE - 15 + offsetX, SIZE - 7 + offsetY);

                g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, 7));
                g2d.drawString("+", SIZE - 10 + offsetX, SIZE - 9 + offsetY);
            } else {
                g2d.drawString(String.valueOf(count), SIZE - 14 + offsetX, SIZE - 7 + offsetY);
            }
        }
    }
}
