package editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sun.jna.Platform.isMac;
import static com.sun.jna.Platform.isWindows;

public class Editor {
    private static final int ARROW_UP = 1000;
    private static final int ARROW_DOWN = 1001;
    static final int ARROW_RIGHT = 1002;
    private static final int ARROW_LEFT = 1003;
    private static final int HOME_KEY = 1004;
    private static final int END_KEY = 1005;
    private static final int PAGE_UP = 1006;
    private static final int PAGE_DOWN = 1007;
    private static final int DEL = 1008;
    private static final List<Integer> escMoveChars = List.of(ARROW_UP, ARROW_DOWN, ARROW_RIGHT, ARROW_LEFT, HOME_KEY, END_KEY, PAGE_DOWN, PAGE_UP);
    private static final int BACKSPACE = 127;
    static String filename;
    private static int rows = 1;
    private static int cols = 1;
    static int cursorX = 0;
    static int cursorY = 0;
    private static int rowOffset = 0;
    private static int colOffset = 0;
    static List<String> content = new ArrayList<>();

    private static Terminal terminal = isMac() ?  new MacOsTerminal() : isWindows() ?  new WindowsTerminal() :  new UnixTerminal();

    public static void main(String[] args) throws IOException {
        openFile(args);
        initScreen();

        while (true){
            scroll();
            refreshScreen();
            int key = readKey();
            handleKey(key);
        }
    }

    private static void scroll() {
        if (cursorY >= rows + rowOffset){
            rowOffset++;
        }
        if (cursorY < rowOffset){
            rowOffset--;
        }
        if (cursorX >= cols + colOffset){
            colOffset++;
        }
        if (cursorX < colOffset){
            colOffset--;
        }
    }

    private static void openFile(String[] args) {
        if (args.length != 1){
            System.err.println("Usage: java src.main.java.com.jsvemo.viewer.Viewer <filename>");
            exit();
        } else {
            filename = args[0];
            Path path = Path.of(filename);
            if (Files.exists(path)){
                try (Stream<String> lineStream = Files.lines(path)) {
                    content = lineStream.collect(Collectors.toCollection(ArrayList::new));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else{
                System.err.println("File not found: " + filename);
                exit();
            }
        }
    }

    private static void saveFile() {
        try {
            Files.write(Path.of(filename), content);
            setStatusMessage("File saved: " + filename);
        } catch (IOException e) {
            setStatusMessage("Could not save file " + filename + " : " + e.getMessage());
            e.printStackTrace();
        }
        exit();
    }

    private static void initScreen() {
        terminal.enableRawMode();
        WindowSize windSize = terminal.getWindowSize();
        rows = windSize.row()-1;
        cols = windSize.col();

    }
    private static void refreshScreen() {
        StringBuilder sb = new StringBuilder();
        //sb.append("\033[2J");
        MoveCursorToTopLeft(sb);
        printContent(sb);
        printStatus(sb);
        printCursor(sb);
        System.out.print(sb);
    }

    private static void MoveCursorToTopLeft(StringBuilder sb) {
        sb.append("\033[H");
    }

    private static void printCursor(StringBuilder sb) {
        sb.append(String.format("\033[%d;%dH", cursorY-rowOffset+1, cursorX-colOffset+1));
    }

    static String statusMessage;
    private static void printStatus(StringBuilder sb) {

        String message = statusMessage != null ? statusMessage : "Showing rows: " + rowOffset +"-"+(rows+ rowOffset-1)
                + "  cols:" + colOffset +"-"+(cols+colOffset)
                + " of " + filename + " (X:" + cursorX + ", Y:"+cursorY + ")";

        int lengthToPrint = Math.min(message.length(), cols);
        sb.append("\033[7m").append(message, 0, lengthToPrint).append(" ".repeat(Math.max(0, cols - message.length()))).append("\033[0m");

    }
    public static void setStatusMessage(String statusMessage){
        Editor.statusMessage = statusMessage;}


    private static void printContent(StringBuilder sb) {
        for (int i = 0; i < rows; i++) {
            int rowOnScreen = rowOffset +i;
            if (rowOnScreen < content.size()) {
                String line = content.get(rowOnScreen);
                int lengthToPrint = Math.max(0, line.length() - colOffset);
                lengthToPrint = Math.min(lengthToPrint, cols);
                if(lengthToPrint > 0){
                    sb.append(line,colOffset, colOffset+lengthToPrint);
                }
            } else if(rowOnScreen == content.size()) {
                sb.append("EOF");
            } else {
                sb.append("~");
            }
            sb.append("\033[K\r\n");
        }
    }

    private static int readKey() throws IOException {
        int key = System.in.read();
        if (key != 27) return key; // ESC

        int secondKey = System.in.read();
        //if (secondKey == '~') {return DEL;}
        if (secondKey == '[') {
            int thirdKey = System.in.read();
            switch (thirdKey) {
                case 'A':
                    return ARROW_UP;
                case 'B':
                    return ARROW_DOWN;
                case 'C':
                    return ARROW_RIGHT;
                case 'D':
                    return ARROW_LEFT;
                case 'H':
                    return HOME_KEY;
                case 'F':
                    return END_KEY;
                default:
                    int fourthKey = System.in.read();
                    if (thirdKey == '5' && fourthKey == '~') {
                        return PAGE_UP;
                    } else if (thirdKey == '6' && fourthKey == '~') {
                        return PAGE_DOWN;
                    } else if (thirdKey == '3' && fourthKey == '~') {
                        return DEL;
                    } else {
                        return key;
                    }
            }
        } else if(secondKey == 'O'){
                int thirdKey = System.in.read();
                switch (thirdKey){
                    case 'H':
                        return HOME_KEY;
                    case 'F':
                        return END_KEY;
                    default:
                        return key;
                }
        } else {
            return key;
        }
    }

    private static void handleKey(int key) {
        if (cursorX < 0){
            cursorX = 0;
        }else if (cursorY < 0){
            cursorY = 0;
        }else if (cursorY >= content.size()){
            cursorY = content.size()-1;
        }else if (cursorX > content.get(cursorY).length()){
            cursorX = content.get(cursorY).length();
        }

        if (key== ctrl('q')){
            exit();
        } else if (key == ctrl('f')) {
            find();
        } else if (key == ctrl('s')){
            saveFile();
        } else if (escMoveChars.contains(key)){
            moveCursor(key);
        } else if (key == '\r'){
            handleEnterKey();
        } else if (key == BACKSPACE || key == DEL || key == ctrl('h')){
            if (key == DEL){
                moveCursor(ARROW_RIGHT);
            }
            deleteChar();
        } else if (!Character.isISOControl(key) && key < 128){
            insertChar(key);
        }
    }
    static void handleEnterKey() {
        if (cursorX == 0){
            content.add(cursorY, "");
        } else if (cursorX == content.get(cursorY).length()){
            if (cursorY +1 < content.size()){
                content.add(cursorY+1, "");
            } else {
                content.add("");
            }
            content.add(cursorY+1, "");
        } else {
            content.add(cursorY+1, content.get(cursorY).substring(cursorX));
            content.set(cursorY, content.get(cursorY).substring(0, cursorX));
        }
        moveCursor(ARROW_DOWN);
        moveCursor(HOME_KEY);
    }

    static void insertChar(int key) {
        if (cursorY == content.size()){
            content.add("");
        }
        String line = content.get(cursorY);
        content.set(cursorY, line.substring(0, cursorX) + (char)key + line.substring(cursorX));
        cursorX++;
    }
    static void deleteChar() {
        if (cursorY == content.size()){
            return;
        }
        if (cursorX == 0 && cursorY == 0){
            return;
        }
        String line = content.get(cursorY);
        if (cursorX > 0){
            content.set(cursorY, line.substring(0, cursorX-1) + line.substring(cursorX));
            cursorX--;
        } else {
            cursorX = content.get(cursorY-1).length();
            content.set(cursorY-1, content.get(cursorY-1) + line);
            content.remove(cursorY);
            cursorY--;
        }
    }

    private static int ctrl(char ch){
        return ch & 0x1f;
    }

    enum SearchDirection {
        FORWARD, BACKWARD}
    static SearchDirection searchDirection = SearchDirection.FORWARD;
    static int lastMatch = -1;

    private static void find(){
        prompt("Find all occurences of %s (Use ESC/Arrows/Enter)", (query, lastKey) -> {
            if (query == null || query.isBlank()){
                return;
            }

            if (lastKey == ARROW_LEFT || lastKey == ARROW_UP){
                searchDirection = SearchDirection.BACKWARD;
            } else if(lastKey == ARROW_DOWN || lastKey == ARROW_RIGHT){
                searchDirection = SearchDirection.FORWARD;
            } else {
                searchDirection = SearchDirection.FORWARD;
                lastMatch = -1;
            }

            int currentIndex = lastMatch;

            for (int i = 0; i < content.size(); i++){
                currentIndex += searchDirection == SearchDirection.FORWARD ? 1 : -1;
                if (currentIndex == content.size()){
                    currentIndex = 0;
                } else if (currentIndex == -1){
                    currentIndex = content.size()-1;
                }
                String currentLine = content.get(currentIndex);
                int match = currentLine.indexOf(query);
                if (match != -1){
                    lastMatch = currentIndex;
                    cursorY = currentIndex;
                    cursorX = match;
                    rowOffset = Math.max(0, cursorY - rows / 2);
                    colOffset = Math.max(0, cursorX - cols / 2);
                    break;
                }
            }
        });
    }

    private static void prompt(String message, BiConsumer<String, Integer> consumer){

        StringBuilder input = new StringBuilder();
        while (true){
            try {
                setStatusMessage(input.length() !=0 ? input.toString() : message);
                refreshScreen();
                int key = readKey();

                if(key == '\033' || key == '\r') {
                    setStatusMessage(null);
                    return;
                } else if (key == DEL || key == BACKSPACE || key == ctrl('h')){
                    if(input.length() !=  0){
                        input.deleteCharAt(input.length()-1);
                    }
                } else if (!Character.isISOControl(key) && key < 128) {
                input.append((char) key);
                }
                consumer.accept(input.toString(), key);
            }
            catch (IOException e){
                throw new RuntimeException(e);
            }
        }
    }


    private static void exit() {
        System.out.print("\033[2J");
        System.out.print("\033[H");
        terminal.disableRawMode();
        System.exit(0);
    }

    static void moveCursor(int key) {
        switch (key){
            case ARROW_UP:
                if (cursorY > 0) {
                    cursorY--;
                    if (cursorX > content.get(cursorY).length() - 1){
                        cursorX = content.get(cursorY).length();
                        colOffset = Math.max(0, cursorX - cols);
                    }
                }
                break;
            case ARROW_DOWN:
                if (cursorY < content.size()-1) {
                    cursorY++;
                    if (cursorX > content.get(cursorY).length()){
                        cursorX = content.get(cursorY).length();
                        colOffset = Math.max(0,cursorX - cols);
                    }
                }
                break;
            case ARROW_RIGHT:
                if (cursorX < content.get(cursorY).length()){
                    cursorX++;
                    break;
                }
                break;
            case ARROW_LEFT:
                if (cursorX > 0) {
                    cursorX--;
                    break;
                } else if (cursorX == 0 && cursorY != 0){
                    cursorY--;
                    cursorX = content.get(cursorY).length();
                    colOffset = Math.max(0, cursorX - cols);
                    break;
                }
                break;
            case HOME_KEY:
                cursorX = 0;
                colOffset = 0;
                break;
            case END_KEY:
                cursorX = content.get(cursorY).length();
                colOffset = Math.max(0, cursorX - cols);
                break;
            case PAGE_UP:
                if(rowOffset >= rows){
                    rowOffset -=rows;
                }else if(rowOffset > 0){
                    rowOffset = 0;
                }
                cursorY = rowOffset;
                cursorX = Math.min(cursorX, content.get(cursorY).length());
                break;
            case PAGE_DOWN:
                if(rowOffset +rows+rows < content.size()) {
                    rowOffset += rows;
                } else if(rowOffset +rows < content.size()){
                    rowOffset = content.size()-rows;
                }
                cursorY = rowOffset + rows -1;
                cursorX = Math.min(cursorX, content.get(cursorY).length());
                break;

        }
    }

}





