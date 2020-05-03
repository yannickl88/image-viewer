package nl.yannickl88.imageview;

import nl.yannickl88.imageview.controller.LibraryController;
import nl.yannickl88.imageview.controller.SetupController;
import nl.yannickl88.imageview.model.Model;
import nl.yannickl88.imageview.model.library.Library;
import nl.yannickl88.imageview.view.LibraryView;
import nl.yannickl88.imageview.view.SetupView;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length == 1) {
            File file = new File(args[0]);

            if (file.exists()) {
                openLibraryView(Library.open(file));
                return;
            }
        }

        new SetupController(new SetupView(), Main::openLibraryView);
    }

    private static void openLibraryView(Library library) {
        Model model = new Model(library);

        new LibraryController(
                model,
                new LibraryView(model.isPersistent()),
                Main::openLibraryView
        );
    }
}
