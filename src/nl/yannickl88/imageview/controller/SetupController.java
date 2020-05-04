package nl.yannickl88.imageview.controller;

import nl.yannickl88.imageview.model.library.Library;
import nl.yannickl88.imageview.view.SetupView;

public class SetupController {
    private final SetupView view;
    private final OpenHandler handler;

    public interface OpenHandler {
        void onOpen(Library library);
    }

    public SetupController(SetupView view, OpenHandler handler) {
        this.view = view;
        this.handler = handler;

        view.setMenuHandler(new SetupView.MenuHandler() {
            @Override
            public void onExit() {
                view.dispose();
                System.exit(0);
            }

            @Override
            public void onSelectFolder() {
                view.openFolderChooser(file -> closeAndOpenLibrary(Library.init(file)));
            }

            @Override
            public void onOpenFile() {
                view.openFileChooser(file -> closeAndOpenLibrary(Library.open(file)));
            }
        });

        view.open();
    }

    private void closeAndOpenLibrary(Library library) {
        view.close();

        // Open library view
        handler.onOpen(library);
    }
}
