package nl.yannickl88.imageview.controller;

import nl.yannickl88.imageview.image.DuplicateImageChecker;
import nl.yannickl88.imageview.model.Image;
import nl.yannickl88.imageview.model.Model;
import nl.yannickl88.imageview.view.DuplicateImagesView;

import java.util.ArrayList;

public class DuplicateImageController {
    private final DuplicateImageChecker checker;
    private final ArrayList<DuplicateImageChecker.Duplicate> removedDuplicates;

    public DuplicateImageController(Model model, DuplicateImagesView view) {
        removedDuplicates = new ArrayList<>();

        checker = new DuplicateImageChecker();
        checker.addProgressListener(new DuplicateImageChecker.ProgressListener() {
            @Override
            public void onStart() {
                view.setStarted(true);
            }

            @Override
            public void onProgress(double completion) {
                view.setProgress(completion);
            }

            @Override
            public void onComplete() {
                view.setStarted(false);
                view.setProgress(1.0);
            }

            @Override
            public void onResultChange() {
                ArrayList<DuplicateImageChecker.Duplicate> results = new ArrayList<>(checker.getResults());
                results.removeAll(removedDuplicates);

                view.updateResults(results);
            }
        });

        view.setHandler(new DuplicateImagesView.ActionHandler() {
            @Override
            public void onStart() {
                checker.checkImages(model.getAllImages());
            }

            @Override
            public void onMerge(DuplicateImageChecker.Duplicate duplicate) {
                // keep the oldest, remove the rest.
                Image oldest = null;

                for (Image i : duplicate.duplicates) {
                    if (oldest == null || oldest.metadata.ctime < i.metadata.ctime) {
                        oldest = i;
                    }
                }

                for (Image i : duplicate.duplicates) {
                    if (oldest == i) {
                        continue;
                    }

                    model.deleteImage(i);
                }

                removedDuplicates.add(duplicate);

                ArrayList<DuplicateImageChecker.Duplicate> results = new ArrayList<>(checker.getResults());
                results.removeAll(removedDuplicates);

                view.updateResults(results);
            }
        });

        view.open();
    }
}
