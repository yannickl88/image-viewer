package nl.yannickl88.imageview.search;

import nl.yannickl88.imageview.model.Image;

/**
 * Matcher class for checking if a query matches an image.
 */
public class SearchMatcher {
    /**
     * Check if the query matches for a given image.
     */
    public static boolean matches(Image image, String query) {
        for (String l : image.metadata.labels) {
            if (l.contains(query)) {
                return true;
            }
        }

        return false;
    }
}
