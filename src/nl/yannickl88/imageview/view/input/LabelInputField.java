package nl.yannickl88.imageview.view.input;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Labels input field. This validates the input to make sure only a list of labels are allowed.
 *
 * Format for a label is: {@code [a-z0-9]+}.
 */
public class LabelInputField extends JTextArea {
    private final Set<String> choices;

    public interface ActionHandler {
        void onCommit(Set<String> labels);
        void onCancel();
    }

    private class CompletionTask implements Runnable {
        private final String completion;
        private final int position;

        CompletionTask(String completion, int position) {
            this.completion = completion;
            this.position = position;
        }

        public void run() {
            if (completion.length() == 0) {
                return;
            }

            insert(completion, position);
            setCaretPosition(position + completion.length());
            moveCaretPosition(position);
        }
    }

    private class EndOfWordTask implements Runnable {
        private int position;
        private final boolean addSpace;

        EndOfWordTask(int position, boolean addSpace) {
            this.position = position;
            this.addSpace = addSpace;
        }

        public void run() {
            if (addSpace) {
                insert(" ", position);
                position++;
            }
            moveCaretPosition(position);
        }
    }

    public LabelInputField(List<String> labels, Set<String> choices, ActionHandler handler) {
        super();
        this.choices = choices;

        String text = String.join(" ", labels);

        if (text.length() > 0) {
            text += " ";
        }

        setText(text);
        setLineWrap(true);
        setWrapStyleWord(true);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 10) {
                    HashSet<String> labels = new HashSet<>(Arrays.asList(getText().split(" +")));
                    labels.removeIf(s -> s.trim().length() == 0);

                    handler.onCommit(labels);
                } else if(e.getKeyCode() == 9) {
                    String content = getText();

                    int nextIndex = content.indexOf(' ', getCaretPosition());

                    if (nextIndex == -1) {
                        nextIndex = content.length();
                    }

                    SwingUtilities.invokeLater(new EndOfWordTask(nextIndex, nextIndex == content.length()));
                } else if (e.getKeyCode() == 27) {
                    handler.onCancel();
                }
            }
        });

        AbstractDocument document = (AbstractDocument) getDocument();

        document.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent ev) {
                if (ev.getLength() != 1) {
                    return;
                }

                int pos = ev.getOffset();
                String content;
                try {
                    content = getText(0, pos + 1);
                } catch (BadLocationException e) {
                    return;
                }

                int startOfWord = content.lastIndexOf(' ', pos);
                if (pos - startOfWord < 1) {
                    return;
                }

                String match = getLabelSuggestion(content.substring(startOfWord + 1).toLowerCase());

                if (null != match) {
                    String completion = match.substring(pos - startOfWord);
                    SwingUtilities.invokeLater(new CompletionTask(completion, pos + 1));
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {

            }

            @Override
            public void changedUpdate(DocumentEvent e) {

            }
        });
        document.setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text.matches("^[a-z0-9- ]+$")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string.matches("^[a-z0-9- ]+$")) {
                    super.insertString(fb, offset, string, attr);
                }
            }
        });
    }

    public String getLabelSuggestion(String prefix) {
        String suggestion = null;

        for (String l : choices) {
            if (l.startsWith(prefix)) {
                if (null != suggestion) {
                    return null; // Must be multiple
                }

                suggestion = l;
            }
        }

        return suggestion;
    }
}
