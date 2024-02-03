package editor;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class EditorTest {

    @Test
    public void editorInitializationWithValidFile() throws IOException{
        String[] args = {"validFile.txt"};
        Editor.main(args);
        assertNotNull(Editor.filename);
    }

    @Test(expected = IOException.class)
    public void editorInitializationWithInvalidFile() throws IOException {
        String[] args = {"invalidFile.txt"};
        Editor.main(args);
    }

    @Test
    public void handleEnterKeyAtEndOfContent() {
        Editor.cursorY = Editor.content.size();
        Editor.handleEnterKey();
        assertEquals(Editor.content.size(), Editor.cursorY + 1);
    }

    @Test
    public void handleEnterKeyInMiddleOfContent() {
        Editor.cursorY = Editor.content.size() / 2;
        Editor.handleEnterKey();
        assertEquals(Editor.content.size(), Editor.cursorY + 1);
    }

    @Test
    public void insertCharInEmptyContent() {
        Editor.insertChar('a');
        assertEquals("a", Editor.content.get(0));
    }

    @Test
    public void insertCharInNonEmptyContent() {
        Editor.content.add("Hello");
        Editor.insertChar('a');
        assertEquals("aHello", Editor.content.get(0));
    }

    @Test
    public void deleteCharInNonEmptyContent() {
        Editor.content.add("Hello");
        Editor.cursorY = 0;
        Editor.cursorX = 1;
        Editor.deleteChar();
        assertEquals("ello", Editor.content.get(0));
    }

    @Test
    public void deleteCharInEmptyContent() {
        Editor.deleteChar();
        assertTrue(Editor.content.isEmpty());
    }

    @Test
    public void moveCursorToNonEmptyContent() {
        Editor.content.add("Hello");
        Editor.cursorX = 2;
        Editor.moveCursor(Editor.ARROW_RIGHT);
        assertEquals(3, Editor.cursorX);
    }

    @Test
    public void moveCursorToEmptyContent() {
        Editor.content.add("");
        Editor.cursorX = 0;
        Editor.moveCursor(Editor.ARROW_RIGHT);
        assertEquals(0, Editor.cursorX);
    }

    @Test
    public void handleEnterKeyInEmptyContent() {
        Editor.cursorY = Editor.content.size();
        Editor.handleEnterKey();
        assertEquals(1, Editor.content.size());
    }


    @Test
    public void handleEnterKeyInNonEmptyContent() {
        Editor.content.add("Hello");
        Editor.cursorX = Editor.content.get(0).length() / 2;
        Editor.cursorY = 0;
        Editor.handleEnterKey();
        assertEquals("He", Editor.content.get(0));
        assertEquals("llo", Editor.content.get(1));
    }

    @Test
    public void deleteCharAtStartOfContent() {
        Editor.content.add("Hello");
        Editor.cursorX = 0;
        Editor.deleteChar();
        assertEquals("Hello", Editor.content.get(0));
    }
}