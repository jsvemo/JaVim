import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


public class Viewer {

    private static final int ARROW_UP = 1000;
    private static final int ARROW_DOWN = 1001;
    private static final int ARROW_RIGHT = 1002;
    private static final int ARROW_LEFT = 1003;
    private static final int HOME_KEY = 1004;
    private static final int END_KEY = 1005;
    private static final int PAGE_UP = 1006;
    private static final int PAGE_DOWN = 1007;
    private static final int DEL = 1008;
    private static final List<Integer> escChars = List.of(ARROW_UP, ARROW_DOWN, ARROW_RIGHT, ARROW_LEFT, HOME_KEY, END_KEY, DEL);
    private static String filename;
    private static int rows = 1;
    private static int cols = 1;
    private static int cursorX = 0;
    private static int cursorY = 0;
    private static int rowOffset = 0;
    private static int colOffset = 0;
    private static List<String> content = List.of();
    private static Terminal terminal = Platform.isMac() ? new MacOsTerminal() : new UnixTerminal();


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
            System.err.println("Usage: java Viewer <filename>");
            exit();
        } else {
            filename = args[0];
            Path path = Path.of(filename);
            if (Files.exists(path)){
                try (Stream<String> lineStream = Files.lines(path)) {
                    //lineStream.forEach(System.out::println);
                    content = lineStream.toList();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else{
                System.err.println("File not found: " + filename);
                exit();
            }
        }
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

    private static void printStatus(StringBuilder sb) {
        String message = "Showing rows: " + rowOffset +"-"+(rows+ rowOffset-1)
                + "  cols:" + colOffset +"-"+(cols+colOffset)
                + " of " + filename + " (X:" + cursorX + ", Y:"+cursorY + ")";
        //message.substring(colOffset, Math.min(colOffset+message.length(), cols));
        //int lengthToPrint = Math.min(Math.max(0, message.length() - colOffset -1), cols);
        //int lengthToPad = 1; //Math.max(0, cols - lengthToPrint);
        int lengthToPrint = Math.min(message.length(), cols);
        sb.append("\033[7m").append(message, 0, lengthToPrint).append("\033[0m");
        //(" ".repeat(colOffset)).append
        //sb.append("\033[7m").append(message, colOffset, colOffset+lengthToPrint).append("\033[0m"); //.append(" ".repeat(lengthToPad-1)).append("\033[0m");
                //.append(" ".repeat(Math.max(0, cols - message.length()))).append("\033[0m");
    }

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
                /*
                if(colOffset+cols < line.length()){
                    int end = Math.min(colOffset+cols, line.length())-1;
                    String lineSegmentOnScreen = line.substring(colOffset, end)+
                    sb.append();


                } else {
                    sb.append(" ".repeat(Math.max(0, cols)));
                } */
            } else if (rowOnScreen == content.size()) {
                //sb.append("EOF");
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
        switch (key){
            case 'q':
                exit();
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
            default:
                if (escChars.contains(key)){
                    moveCursor(key);
                }
                //System.out.print((char) key + " -> (" + key + ")\r\n");
        }
    }

    private static void exit() {
        System.out.print("\033[2J");
        System.out.print("\033[H");
        terminal.disableRawMode();
        System.exit(0);
    }

    private static void moveCursor(int key) {
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
            case DEL:
                break;
        }
    }

}

interface Terminal {
    void enableRawMode();
    void disableRawMode();
    WindowSize getWindowSize();
}
class UnixTerminal implements Terminal{
    private static LibC.Termios originalAttributes;
    @Override
    public void enableRawMode() {
        LibC.Termios termios = new LibC.Termios();
        int returnCode = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT_FD, termios);
        originalAttributes = LibC.Termios.of(termios);
        if(returnCode != 0){
            System.err.println("tcgetattr failed: " + returnCode);
            System.exit(returnCode);
        }

        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        termios.c_oflag &= ~(LibC.OPOST);
        termios.c_cc[LibC.VMIN] = 0;
        termios.c_cc[LibC.VTIME] = 1;
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, termios);
    }

    @Override
    public void disableRawMode() {
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalAttributes);
    }

    @Override
    public WindowSize getWindowSize() {
        final LibC.Winsize winsize = new LibC.Winsize();
        final int returnCode = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT_FD, LibC.TIOCGWINSZ, winsize);
        if(returnCode != 0){
            System.err.println("ioctl failed. returnCode: " + returnCode);
            System.exit(1);
        }
        return new WindowSize(winsize.rows, winsize.cols);
    }

    /**
     * For more information about the termios structure, see:
     * https://www.ibm.com/docs/en/zos/2.3.0?topic=functions-tcsetattr-set-attributes-terminal
     * https://www.gnu.org/software/libc/manual/html_node/Terminal-Modes.html
     * or run `man termios` in your terminal.
     * To get the value of IXON variable use 'grep -R IXON /usr/include'
     */
    interface LibC extends Library {

        //int STDIN_FILENO = 0, STDOUT_FILENO = 1, STDERR_FILENO = 2;
        int SYSTEM_OUT_FD = 0; // SYSTEM_OUT_FD
        //int TCSANOW = 0, TCSADRAIN = 1,
        int TCSAFLUSH = 2;
        int ISIG = 1, ICANON = 2, ECHO = 10, IXON = 2000, ICRNL = 400,
                IEXTEN = 100000, OPOST = 1, VMIN = 6, VTIME = 5, TIOCGWINSZ = 0x5413;
        LibC INSTANCE = Native.load("c", LibC.class);



        @Structure.FieldOrder(value = {"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
        class Termios extends Structure{
            public int c_iflag;
            public int c_oflag;
            public int c_cflag;
            public int c_lflag;
            public byte[] c_cc = new byte[19];

            public Termios(){
            }
            public static Termios of(Termios t){
                Termios termios = new Termios();
                termios.c_iflag = t.c_iflag;
                termios.c_oflag = t.c_oflag;
                termios.c_cflag = t.c_cflag;
                termios.c_lflag = t.c_lflag;
                termios.c_cc = t.c_cc.clone();
                return termios;
            }

            @Override
            public String toString() {
                return "Terminos{" +
                        "c_iflag=" + c_iflag +
                        ", c_oflag=" + c_oflag +
                        ", c_cflag=" + c_cflag +
                        ", c_lflag=" + c_lflag +
                        ", c_cc=" + Arrays.toString(c_cc) +
                        '}';
            }
        }
        @Structure.FieldOrder(value = {"rows", "cols", "xpixel", "ypixel"})
        class Winsize extends Structure {
            public short rows;
            public short cols;
            public short xpixel;
            public short ypixel;

            public Winsize(){

            }

            public static Winsize of(Winsize w){
                Winsize winsize = new Winsize();
                winsize.rows = w.rows;
                winsize.cols = w.cols;
                return winsize;
            }

            @Override
            public String toString() {
                return "Winsize{" +
                        "ws_row=" + rows +
                        ", ws_col=" + cols +
                        '}';
            }
        }

        int tcgetattr(int fd, Termios termios);

        int tcsetattr(int fd, int optional_actions, Termios termios);

        int ioctl(int fd, int request, Winsize winsize);

    }

}

class MacOsTerminal implements Terminal{
    private static LibC.Termios originalAttributes;
    @Override
    public void enableRawMode() {
        LibC.Termios termios = new LibC.Termios();
        int returnCode = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT_FD, termios);
        originalAttributes = LibC.Termios.of(termios);
        if(returnCode != 0){
            System.err.println("tcgetattr failed: " + returnCode);
            System.exit(returnCode);
        }

        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        termios.c_oflag &= ~(LibC.OPOST);
        termios.c_cc[LibC.VMIN] = 0;
        termios.c_cc[LibC.VTIME] = 1;
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, termios);
    }

    @Override
    public void disableRawMode() {
        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT_FD, LibC.TCSAFLUSH, originalAttributes);
    }

    @Override
    public WindowSize getWindowSize() {
        final LibC.Winsize winsize = new LibC.Winsize();
        final int returnCode = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT_FD, LibC.TIOCGWINSZ, winsize);
        if(returnCode != 0){
            System.err.println("ioctl failed. returnCode: " + returnCode);
            System.exit(1);
        }
        return new WindowSize(winsize.rows, winsize.cols);
    }

    /**
     * https://www.ibm.com/docs/en/zos/2.3.0?topic=functions-tcsetattr-set-attributes-terminal
     * https://www.gnu.org/software/libc/manual/html_node/Terminal-Modes.html
     * To get the value of IXON variable use 'grep -R IXON /usr/include'
     */
    interface LibC extends Library {

        //int STDIN_FILENO = 0, STDOUT_FILENO = 1, STDERR_FILENO = 2;
        int SYSTEM_OUT_FD = 0; // SYSTEM_OUT_FD
        //int TCSANOW = 0, TCSADRAIN = 1,
        int TCSAFLUSH = 2;
        int ISIG = 1, ICANON = 2, ECHO = 10, IXON = 2000, ICRNL = 400,
                IEXTEN = 100000, OPOST = 1, VMIN = 6, VTIME = 5, TIOCGWINSZ = 0x40087468;
        LibC INSTANCE = Native.load("c", LibC.class);



        @Structure.FieldOrder(value = {"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
        class Termios extends Structure{
            public long c_iflag;
            public long c_oflag;
            public long c_cflag;
            public long c_lflag;
            public byte[] c_cc = new byte[19];

            public Termios(){
            }
            public static Termios of(Termios t){
                Termios termios = new Termios();
                termios.c_iflag = t.c_iflag;
                termios.c_oflag = t.c_oflag;
                termios.c_cflag = t.c_cflag;
                termios.c_lflag = t.c_lflag;
                termios.c_cc = t.c_cc.clone();
                return termios;
            }

            @Override
            public String toString() {
                return "Terminos{" +
                        "c_iflag=" + c_iflag +
                        ", c_oflag=" + c_oflag +
                        ", c_cflag=" + c_cflag +
                        ", c_lflag=" + c_lflag +
                        ", c_cc=" + Arrays.toString(c_cc) +
                        '}';
            }
        }
        @Structure.FieldOrder(value = {"rows", "cols", "xpixel", "ypixel"})
        class Winsize extends Structure {
            public short rows;
            public short cols;
            public short xpixel;
            public short ypixel;

            public Winsize(){

            }

            public static Winsize of(Winsize w){
                Winsize winsize = new Winsize();
                winsize.rows = w.rows;
                winsize.cols = w.cols;
                return winsize;
            }

            @Override
            public String toString() {
                return "Winsize{" +
                        "ws_row=" + rows +
                        ", ws_col=" + cols +
                        '}';
            }
        }

        int tcgetattr(int fd, Termios termios);

        int tcsetattr(int fd, int optional_actions, Termios termios);

        int ioctl(int fd, int request, Winsize winsize);

    }

}

record WindowSize(int row, int col) {
}


