package editor;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

class WindowSize {
    private final int row;
    private final int col;

    public WindowSize(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }
}

interface Terminal {
    void enableRawMode();

    void disableRawMode();

    WindowSize getWindowSize();

    //record WindowSize(int row, int col) {}
}

/*


 */

