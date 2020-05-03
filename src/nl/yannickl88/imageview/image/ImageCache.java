package nl.yannickl88.imageview.image;

import nl.yannickl88.imageview.model.Image;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Cache based on the Image class and an associated Object.
 */
public class ImageCache<T> {
    private final HashMap<Image, T> imageCache;
    private final LinkedList<Image> queue;

    public ImageCache() {
        imageCache = new HashMap<>();
        queue = new LinkedList<>();
    }

    public boolean has(Image image) {
        return imageCache.containsKey(image);
    }

    public T get(Image image) {
        return imageCache.get(image);
    }

    public void put(Image image, T drawable) {
        imageCache.put(image, drawable);
        queue.add(image);

        while (queue.size() > 12) {
            imageCache.remove(queue.remove());
        }
    }
}
