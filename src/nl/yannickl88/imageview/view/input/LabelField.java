package nl.yannickl88.imageview.view.input;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.util.ArrayList;

/**
 * Label input field. This validates the input to make sure only the characters are allowed for labels.
 *
 * Format for a label is: {@code [a-z0-9]+}.
 */
public class LabelField extends JTextField {
    private final ArrayList<ChangeListener> listeners;
    private final String originalValue;

    public interface ChangeListener {
        void onChange();
    }

    public LabelField(String label) {
        super(label);

        originalValue = label;

        AbstractDocument document = (AbstractDocument) getDocument();
        document.setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text.matches("^[a-z-0-9]+$")) {
                    super.replace(fb, offset, length, text, attrs);

                    notifyOnChange();
                }
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string.matches("^[a-z-0-9]+$")) {
                    super.insertString(fb, offset, string, attr);

                    notifyOnChange();
                }
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                super.remove(fb, offset, length);
                notifyOnChange();
            }
        });
        listeners = new ArrayList<>();
    }

    public void addActionHandler(ChangeListener listener) {
        listeners.add(listener);
    }

    public boolean hasChanged() {
        return !originalValue.equals(getText());
    }

    private void notifyOnChange() {
        for (ChangeListener h : listeners) {
            h.onChange();
        }
    }
}
