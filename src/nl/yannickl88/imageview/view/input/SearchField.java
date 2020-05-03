package nl.yannickl88.imageview.view.input;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SearchField extends JTextField {
    public interface ActionHandler {
        void onSearch(String query);
        void onCancel();
    }

    public SearchField(ActionHandler handler) {
        super();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 10) {
                    handler.onSearch(getText());
                } else if (e.getKeyCode() == 27) {
                    clear();
                    handler.onCancel();
                }
            }
        });

        AbstractDocument document = (AbstractDocument) getDocument();
        document.setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text.matches("^[a-z0-9- ]+$")) {
                    super.replace(fb, offset, length, text, attrs);

                    handler.onSearch(getText());
                }
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string.matches("^[a-z0-9- ]+$")) {
                    super.insertString(fb, offset, string, attr);

                    handler.onSearch(getText());
                }
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                super.remove(fb, offset, length);
                handler.onSearch(getText());
            }
        });
    }

    public void clear() {
        Document doc = getDocument();
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ignored) {
        }
    }
}
