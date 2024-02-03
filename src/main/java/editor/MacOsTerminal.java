package editor;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.util.Arrays;

public class MacOsTerminal implements Terminal {
    private static LibC.Termios originalAttributes;

    @Override
    public void enableRawMode() {
        LibC.Termios termios = new LibC.Termios();
        int returnCode = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT_FD, termios);
        originalAttributes = LibC.Termios.of(termios);
        if (returnCode != 0) {
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
        if (returnCode != 0) {
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
        class Termios extends Structure {
            public long c_iflag;
            public long c_oflag;
            public long c_cflag;
            public long c_lflag;
            public byte[] c_cc = new byte[19];

            public Termios() {
            }

            public static Termios of(Termios t) {
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

            public Winsize() {

            }

            public static Winsize of(Winsize w) {
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
