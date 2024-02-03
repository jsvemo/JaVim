# JavimTextEditor
JavimTextEditor is a text editor application that runs in the terminal. It is implemented in Java and uses Maven for dependency management.

## Features
- Cross-platform support (MacOS, Unix, Windows)
- Uses the JNA library for native access

## Installation
1. Clone the repository:
   git clone https://github.com/jsvemo/TerminalTextzEditor.git
2. Navigate to the project directory:
   cd TerminalTextzEditor
3. Compile the project with Maven:
   mvn compile

   
## Usage
Run the application with the following command:
java -cp ./src/main/resources/jna-5.13.0.jar:./src/main/java/editor.Editor ./src/main/resources/AL2.0

If you want to recompile the project, use the following command: 
javac -cp ./src/main/resources/jna-5.13.0.jar ./src/main/java/editor/*.java
java -cp ./src/main/resources/jna-5.13.0.jar:./src/main/java/editor.Editor ./src/main/resources/AL2.0

## Program Keybindings
- `Ctrl + Q` - Quit the application
- `Ctrl + S` - Save and Exit the file
- `Ctrl + F` - Find all usages in the file, searches recursively from the current cursor position.

# Movement
- `UP` - Move the cursor up one line.
- `DOWN` - Move the cursor down one line.
- `LEFT` - Move the cursor left one character.
- `RIGHT` - Move the cursor right one character.
- `HOME` - Jump to the beginning of the line.
- `END` - Jump to the end of the line.
- `PAGE UP` - Jump a page up, the amount of lines is determined by the terminal size.
- `PAGE DOWN` - Jump a page down, the amount of lines is determined by the terminal size.


Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
## License
[MIT](https://choosealicense.com/licenses/mit/)
