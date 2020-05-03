package nl.yannickl88.imageview.search;

import nl.yannickl88.imageview.model.Image;

public class SearchMatcher {
    public static boolean matches(Image image, String query) {
        for (String l : image.metadata.labels) {
            if (l.contains(query)) {
                return true;
            }
        }

        return false;
    }
}
