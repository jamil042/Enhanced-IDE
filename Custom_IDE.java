import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.geometry.Orientation;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import java.io.BufferedReader;
import javafx.scene.image.Image;
import javafx.stage.StageStyle;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import java.io.*;
import java.util.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import javafx.scene.control.TextArea;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;

public class  Main extends Application {

    private CodeArea codeArea;
    private TextArea consoleArea;
    private Process runningProcess;
    private boolean isWaitingForInput = false;
    private ListView<String> fileListView;
    private String currentFileName = "Untitled1";
    private Map<String, String> fileContentMap = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        Image logoImage = new Image(getClass().getResourceAsStream("Enhenced_IDE_Pic.jpg"));
        ImageView logoView = new ImageView(logoImage);
        logoView.setFitWidth(800);
        logoView.setFitHeight(600);
        logoView.setPreserveRatio(true);

        StackPane splashLayout = new StackPane(logoView);
        splashLayout.setStyle("-fx-background-color: white;");
        Scene splashScene = new Scene(splashLayout, 800, 400);
        splashStage.setScene(splashScene);

        centerStageOnScreen(splashStage);

        splashStage.show();
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                splashStage.close();
                BorderPane root = new BorderPane();

                MenuBar menuBar = createMenuBar(primaryStage);

                codeArea = createCodeArea();
                fileListView = new ListView<>();
                fileListView.setPrefWidth(80);
                fileListView.setMinWidth(60);
                fileListView.getItems().add(currentFileName);
                fileContentMap.put(currentFileName, "");
                initializeFileListView();
                VBox leftPane = new VBox(new Label("Files"), fileListView);
                leftPane.setPrefWidth(90);
                leftPane.setMinWidth(70);
                SplitPane centerSplitPane = createSplitPane();

                SplitPane mainSplitPane = new SplitPane();
                mainSplitPane.setOrientation(Orientation.HORIZONTAL);
                mainSplitPane.getItems().addAll(leftPane, centerSplitPane);
                mainSplitPane.setDividerPositions(0.02);
                root.setTop(menuBar);
                root.setCenter(mainSplitPane);
                Image Icon = new Image(getClass().getResourceAsStream("Project Logo.jpg"));

                Scene scene = new Scene(root, 800, 600);
                primaryStage.setMaximized(true);
                primaryStage.setScene(scene);
                primaryStage.getIcons().add(Icon);
                primaryStage.setTitle("Enhanced IDE");
                primaryStage.show();
            });
        }).start();
    }

    private void centerStageOnScreen(Stage stage) {
        double centerX = 370;
        double centerY = 250;
        stage.setX(centerX);
        stage.setY(centerY);
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem newFile = new MenuItem("New");
        MenuItem closeFile = new MenuItem("Close");
        fileMenu.getItems().addAll(newFile, openFile, saveFile, closeFile);

        Menu runMenu = new Menu("Run");
        MenuItem runCode = new MenuItem("Run");
        runMenu.getItems().add(runCode);

        Menu debugMenu = new Menu("Debug");
        MenuItem debugCode = new MenuItem("Debug");
        debugMenu.getItems().add(debugCode);

        Menu toolsMenu = new Menu("Tools");
        MenuItem formatCode = new MenuItem("Code Template");
        MenuItem analyzeCode = new MenuItem("Analyze Code");
        MenuItem deleteCode = new MenuItem("Clear Code");
        toolsMenu.getItems().addAll(formatCode, analyzeCode, deleteCode);

        Menu settingMenu = new Menu("Setting");
        Menu themeMenu = new Menu("Theme");
        MenuItem lightMode = new MenuItem("Light Mode");
        MenuItem darkMode = new MenuItem("Dark Mode");
        themeMenu.getItems().addAll(lightMode, darkMode);
        settingMenu.getItems().add(themeMenu);

        Menu helpMenu = new Menu("Help");
        MenuItem aboutApp = new MenuItem("About");
        MenuItem documentation = new MenuItem("Documentation");
        helpMenu.getItems().addAll(documentation, aboutApp);

        menuBar.getMenus().addAll(fileMenu, runMenu, debugMenu, toolsMenu, settingMenu, helpMenu);

        openFile.setOnAction(e -> openFile(primaryStage));
        newFile.setOnAction(e -> newFile(primaryStage));
        saveFile.setOnAction(e -> saveFile(primaryStage));
        closeFile.setOnAction(e -> closeFile(primaryStage));
        runCode.setOnAction(e -> runCode());
        debugCode.setOnAction(e -> debugCode());
        deleteCode.setOnAction(e -> deleteCode());
        aboutApp.setOnAction(e -> detailsAboutIDE());
        formatCode.setOnAction(e -> codeFormat());
        analyzeCode.setOnAction(e -> codeAnalyze());
        documentation.setOnAction(e -> appDocumentation());
        lightMode.setOnAction(e -> applyLightMode());
        darkMode.setOnAction(e -> applyDarkMode());

        return menuBar;
    }

    private CodeArea createCodeArea() {
        CodeArea codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.textProperty().addListener((obs, oldText, newText) -> applySyntaxHighlighting(newText));
        codeArea.setStyle(
                "-fx-font-family: 'Consolas', 'Menlo', 'monospace'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-text-fill: #D4D4D4;"
        );
        codeArea.getStyleClass().add("light-mode");
        codeArea.getStylesheets().add(getClass().getResource("/Main.css").toExternalForm());
        return codeArea;
    }


    private TextArea createConsoleArea() {
        TextArea consoleArea = new TextArea();
        consoleArea.setEditable(true);
        consoleArea.setStyle(
                "-fx-control-inner-background: #1E1E1E; " +
                        "-fx-text-fill: #D4D4D4; " +
                        "-fx-font-family: 'Consolas', 'Menlo', 'monospace'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 5px; " +
                        "-fx-border-color: #333333; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 3px;"
        );
        consoleArea.setPrefHeight(200);

        consoleArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume(); // Prevent adding a newline
                if (isWaitingForInput) {
                    handleConsoleInput(consoleArea);
                } else {
                    appendToConsole("\nError: The program is not waiting for input.\n> ");
                }
            }
        });

        return consoleArea;
    }

    private void handleConsoleInput(TextArea consoleArea) {
        String text = consoleArea.getText();
        String[] lines = text.split("\n");
        String input = lines[lines.length - 1].substring(2);

        if (isWaitingForInput && runningProcess != null && runningProcess.isAlive()) {
            sendInputToProgram(input);
            isWaitingForInput = false;
        } else {
            appendToConsole("\nError: The program is not waiting for input.\n> ");
        }
    }
    private boolean isInputRequired() {
        return codeArea.getText().contains("cin");
    }

    private String getConsoleInput() {
        // Wait for user input in the console area
        String input = consoleArea.getText().trim();
        if (input.startsWith("> ")) {
            input = input.substring(2); // Remove the prompt
        }
        return input.isEmpty() ? null : input;
    }

    private void clearConsoleInput() {
        Platform.runLater(() -> {
            consoleArea.clear();
        });
    }

    private void appendToConsole(String text) {
        Platform.runLater(() -> {
            consoleArea.appendText(text + "\n");
            consoleArea.positionCaret(consoleArea.getText().length());
        });
    }


    private void sendInputToProgram(String input) {
        try (BufferedWriter processInput = new BufferedWriter(new OutputStreamWriter(runningProcess.getOutputStream()))) {
            processInput.write(input + "\n");
            processInput.flush();
        } catch (IOException e) {
            appendToConsole("\nError sending input to program: " + e.getMessage());
        }
    }

    private SplitPane createSplitPane() {
        consoleArea = createConsoleArea();
        codeArea = createCodeArea();
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(codeArea, consoleArea);
        splitPane.setDividerPositions(0.7);

        return splitPane;
    }

    private void initializeFileListView() {
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(currentFileName)) {
                fileContentMap.put(currentFileName, codeArea.getText());

                currentFileName = newVal;
                String content = fileContentMap.getOrDefault(currentFileName, "");
                codeArea.replaceText(content);
            }
        });
    }

    private void openFile(Stage stage) {
        if (hasUnsavedChanges()) {
            promptSaveChanges(stage);
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("C++ Files", "*.cpp"));
        fileChooser.setTitle("Open File");
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            currentFileName = file.getName();
            if (!fileListView.getItems().contains(currentFileName)) {
                fileListView.getItems().add(currentFileName);
            }
            fileListView.getSelectionModel().select(currentFileName);

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                fileContentMap.put(currentFileName, content.toString());
                codeArea.replaceText(content.toString());
            } catch (IOException e) {
                showError("Error opening file", e.getMessage());
            }
        }
    }

    private void saveFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("C++ Files", "*.cpp"));

        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            if (!file.getName().endsWith(".cpp")) {
                file = new File(file.getAbsolutePath() + ".cpp");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                String content = codeArea.getText();
                writer.write(content);

                String oldFileName = currentFileName;
                currentFileName = file.getName();

                fileListView.getItems().remove(oldFileName);
                fileContentMap.remove(oldFileName);

                fileListView.getItems().add(currentFileName);
                fileContentMap.put(currentFileName, content);

                fileListView.getSelectionModel().select(currentFileName);
            } catch (IOException e) {
                showError("Error saving file", e.getMessage());
            }
        }
    }

    private void newFile(Stage stage) {
        codeArea.clear();
        currentFileName = "Untitled" + (fileListView.getItems().size() + 1);
        fileListView.getItems().add(currentFileName);
        fileContentMap.put(currentFileName, "");
        fileListView.getSelectionModel().select(currentFileName);
    }

    private void closeFile(Stage stage) {
        if (hasUnsavedChanges()) {
            promptSaveChanges(stage);
        }

        fileContentMap.put(currentFileName, codeArea.getText());

        int currentFileIndex = fileListView.getItems().indexOf(currentFileName);

        fileListView.getItems().remove(currentFileName);
        fileContentMap.remove(currentFileName);

        if (fileListView.getItems().isEmpty()) {
            currentFileName = "Untitled1";
            fileListView.getItems().add(currentFileName);
            fileContentMap.put(currentFileName, "");
            codeArea.replaceText("");
        } else {
            int previousFileIndex = Math.max(0, currentFileIndex - 1);
            currentFileName = fileListView.getItems().get(previousFileIndex);

            String previousFileContent = fileContentMap.getOrDefault(currentFileName, "");
            codeArea.replaceText(previousFileContent);
        }

        fileListView.getSelectionModel().select(currentFileName);
    }

    private boolean hasUnsavedChanges() {
        String savedContent = fileContentMap.get(currentFileName);
        String currentContent = codeArea.getText();
        return savedContent == null || !currentContent.equals(savedContent);
    }

    private void promptSaveChanges(Stage stage) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Do you want to save them before proceeding?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            saveFile(stage);
        }
    }


    private void runCode() {
        new Thread(() -> {
            try {
                String code = codeArea.getText();
                boolean requiresInput = isInputRequired();

                File tempFile = File.createTempFile("CustomIDE", ".cpp");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                    writer.write(code);
                }

                ProcessBuilder compileProcess = new ProcessBuilder("g++", tempFile.getAbsolutePath(), "-o", tempFile.getParent() + "/temp");
                Process compile = compileProcess.start();
                compile.waitFor();

                if (compile.exitValue() == 0) {
                    appendToConsole("\nCompilation Successful!\n");
                    ProcessBuilder runProcess = new ProcessBuilder(tempFile.getParent() + "/temp");
                    runProcess.redirectErrorStream(true);
                    runningProcess = runProcess.start();

                    Thread outputThread = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(runningProcess.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                appendToConsole(line); // Display output in the console

                                // Check if the program is waiting for input
                                if (requiresInput && (line.contains(":") || line.contains(">"))) {
                                    isWaitingForInput = true;
                                }
                            }
                        } catch (IOException e) {
                            appendToConsole("\nError reading program output: " + e.getMessage());
                        }
                    });
                    outputThread.start();

                    Thread inputThread = new Thread(() -> {
                        try (BufferedWriter processInput = new BufferedWriter(new OutputStreamWriter(runningProcess.getOutputStream()))) {
                            while (runningProcess != null && runningProcess.isAlive()) { // Check if runningProcess is not null
                                if (isWaitingForInput) {
                                    String userInput = getConsoleInput(); // Get input from the console area
                                    if (userInput != null && !userInput.isEmpty()) {
                                        processInput.write(userInput + "\n"); // Send input to the program
                                        processInput.flush(); // Flush the output stream
                                        isWaitingForInput = false; // Reset input state
                                        clearConsoleInput(); // Clear the input after sending
                                    }
                                }
                            }
                        } catch (IOException e) {
                            appendToConsole("\nError sending input to program: " + e.getMessage());
                        }
                    });
                    inputThread.start();

                    runningProcess.waitFor();
                    outputThread.join();
                    inputThread.join();
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(compile.getErrorStream()));
                    StringBuilder errorMessage = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorMessage.append(errorLine).append("\n");
                    }
                    appendToConsole("\nCompilation failed:\n" + errorMessage.toString());
                }

                // Clean up temporary files
                tempFile.delete();
                File tempBinary = new File(tempFile.getParent() + "/temp");
                if (tempBinary.exists()) {
                    tempBinary.delete();
                }
            } catch (Exception e) {
                appendToConsole("\nError running code: " + e.getMessage());
            } finally {
                if (runningProcess != null) {
                    runningProcess.destroy();
                }
                runningProcess = null;
                isWaitingForInput = false;
            }
        }).start();
    }

    private void applyLightMode() {
        codeArea.getStyleClass().remove("dark-mode");
        if (!codeArea.getStyleClass().contains("light-mode")) {
            codeArea.getStyleClass().add("light-mode");
        }
    }

    private void applyDarkMode() {
        codeArea.getStyleClass().remove("light-mode");
        if (!codeArea.getStyleClass().contains("dark-mode")) {
            codeArea.getStyleClass().add("dark-mode");
        }
    }


    private void debugCode() {
        new Thread(() -> {
            try {
                String code = codeArea.getText();

                File tempFile = File.createTempFile("CustomIDE", ".cpp");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                    writer.write(code);
                }

                // Compile the code with debug symbols
                ProcessBuilder compileProcess = new ProcessBuilder("g++", "-g", tempFile.getAbsolutePath(), "-o", tempFile.getParent() + "/temp");
                Process compile = compileProcess.start();
                compile.waitFor();

                if (compile.exitValue() == 0) {
                    // Start the debugger (GDB)
                    ProcessBuilder debugProcess = new ProcessBuilder("gdb", tempFile.getParent() + "/temp");
                    debugProcess.redirectErrorStream(true);

                    Process debug = debugProcess.start();

                    // Write debug commands to the process
                    try (BufferedWriter debugInput = new BufferedWriter(new OutputStreamWriter(debug.getOutputStream()))) {
                        debugInput.write("break main\n"); // Set a breakpoint at main
                        debugInput.write("run\n");        // Run the program
                        debugInput.write(getConsoleInput() + "\n"); // Additional user commands if needed
                        debugInput.flush();
                    }

                    // Read debugger output
                    BufferedReader reader = new BufferedReader(new InputStreamReader(debug.getInputStream()));
                    StringBuilder debugOutput = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        debugOutput.append(line).append("\n");
                    }

                    // Display the debugger output in the console area
                    appendToConsole("Debugger Output:\n" + debugOutput.toString());
                } else {
                    // Display compilation errors
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(compile.getErrorStream()));
                    StringBuilder errorMessage = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorMessage.append(line).append("\n");
                    }
                    appendToConsole("Compilation failed:\n" + errorMessage.toString());
                }

                // Clean up temporary files
                tempFile.delete();
                File tempBinary = new File(tempFile.getParent() + "/temp");
                if (tempBinary.exists()) {
                    tempBinary.delete();
                }
            } catch (Exception e) {
                appendToConsole("Error debugging code: " + e.getMessage());
            }
        }).start();
    }


    private void deleteCode() {
        codeArea.clear();
        consoleArea.clear();
        appendToConsole("Code and console areas have been cleared.");
    }

    private void detailsAboutIDE() {
        Alert detailsAlert = new Alert(Alert.AlertType.INFORMATION);
        detailsAlert.setTitle("About Enhanced IDE");
        detailsAlert.setHeaderText("Enhanced IDE - Version 1.0");
        detailsAlert.setContentText("This is an enhanced IDE built using JavaFX.\n\n"
                + "Features:\n"
                + "- Code editing with syntax highlighting\n"
                + "- Run and debug C++ code\n"
                + "- Input and output visualization\n"
                + "- File operations (Open, Save, New)\n\n"
                + "Developed by: Taz , Jamil , Rihin");
        detailsAlert.showAndWait();
    }

    private void codeAnalyze() {
        String code = codeArea.getText();

        // Basic metrics
        int lines = code.split("\n").length;
        int words = code.split("\\s+").length;
        int characters = code.length();

        Set<String> variablesSet = new HashSet<>();
        Set<String> functionsSet = new HashSet<>();
        Set<String> classesSet = new HashSet<>();
        Set<String> dataStructuresSet = new HashSet<>();
        Set<String> algorithmsSet = new HashSet<>();

        Pattern variablePattern = Pattern.compile("\\b(int|double|float|char|string|long|short|bool|auto)\\s+(\\w+)\\b(?!\\s*\\()");

        Pattern classPattern = Pattern.compile("\\b(class|struct)\\s+(\\w+)");
        Pattern functionPattern = Pattern.compile("\\b(\\w+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:\\{|\\n)");
        Pattern dataStructurePattern = Pattern.compile("\\b(vector|list|deque|set|map|unordered_map|unordered_set|stack|queue|priority_queue|array|bitset|forward_list|shared_ptr|unique_ptr|weak_ptr|tuple|pair)\\b");
        Pattern algorithmPattern = Pattern.compile("\\b(sort|find|binary_search|count|accumulate|reverse|shuffle|lower_bound|upper_bound|merge|quick_sort|dijkstra|kruskal|prim|floyd_warshall)\\b");

        Matcher variableMatcher = variablePattern.matcher(code);
        StringBuilder variables = new StringBuilder("Variables Created:\n");
        while (variableMatcher.find()) {
            String variable = variableMatcher.group(2); // Capture variable name
            if (variablesSet.add(variable)) {
                variables.append(variable).append("\n");
            }
        }
        if (variablesSet.isEmpty()) {
            variables.append("None\n");
        }

        Matcher classMatcher = classPattern.matcher(code);
        StringBuilder classes = new StringBuilder("Classes/Structs Defined:\n");
        while (classMatcher.find()) {
            String className = classMatcher.group(2);
            if (classesSet.add(className)) {
                classes.append(className).append("\n");
            }
        }
        if (classesSet.isEmpty()) {
            classes.append("None\n");
        }

        Matcher functionMatcher = functionPattern.matcher(code);
        StringBuilder functions = new StringBuilder("Functions Defined:\n");
        while (functionMatcher.find()) {
            String functionName = functionMatcher.group(2); // Capture function name
            if (functionsSet.add(functionName)) {
                functions.append(functionName).append("\n");
            }
        }
        if (functionsSet.isEmpty()) {
            functions.append("None\n");
        }

        Matcher dataStructureMatcher = dataStructurePattern.matcher(code);
        StringBuilder dataStructures = new StringBuilder("Data Structures Used:\n");
        while (dataStructureMatcher.find()) {
            String dataStructure = dataStructureMatcher.group();
            if (dataStructuresSet.add(dataStructure)) {
                dataStructures.append(dataStructure).append("\n");
            }
        }
        if (dataStructuresSet.isEmpty()) {
            dataStructures.append("None\n");
        }

        Matcher algorithmMatcher = algorithmPattern.matcher(code);
        StringBuilder algorithms = new StringBuilder("Algorithms Detected:\n");
        while (algorithmMatcher.find()) {
            String algorithm = algorithmMatcher.group();
            if (algorithmsSet.add(algorithm)) {
                algorithms.append(algorithm).append("\n");
            }
        }
        if (algorithmsSet.isEmpty()) {
            algorithms.append("None\n");
        }

        String feedback = "Feedback:\n"
                + "1. Ensure variable names are meaningful and self-explanatory.\n"
                + "2. Use comments to describe complex logic.\n"
                + "3. Follow consistent indentation and spacing for readability.\n"
                + "4. Avoid deeply nested loops; consider breaking into smaller functions.\n"
                + "5. Use appropriate data structures and algorithms for efficient code.\n"
                + "6. Avoid global variables; prefer local scope where possible.\n"
                + "7. Use const-correctness for variables and functions where applicable.\n";

        String analysis = String.format(
                "Code Analysis:\nLines: %d\nWords: %d\nCharacters: %d\n\n%s\n%s\n%s\n%s\n%s\n\n%s",
                lines, words, characters, variables.toString(), classes.toString(), functions.toString(),
                dataStructures.toString(), algorithms.toString(), feedback);

        appendToConsole(analysis);
    }



    private void codeFormat() {
        String code = codeArea.getText();
        StringBuilder formattedCode = new StringBuilder();

        Pattern classNamePattern = Pattern.compile("\\bclass\\s+(\\w+)\\s*\\{");
        Matcher classMatcher = classNamePattern.matcher(code);

        while (classMatcher.find()) {
            String className = classMatcher.group(1);
            System.out.println("Class name: " + className);

            Pattern classPattern = Pattern.compile(
                    "class\\s+" + className + "\\s*\\{[\\s\\S]*?\\}",
                    Pattern.MULTILINE
            );
            Matcher classDefMatcher = classPattern.matcher(code);
            while (classDefMatcher.find()) {
                String classDefinition = classDefMatcher.group();

                String formattedClass = classDefinition.replaceAll(
                        "(\\b(?:" + className + "|int|string|double|float|void|bool|\\w+)\\s+\\w+\\s*\\([^)]*\\)\\s*\\{)[\\s\\S]*?(\\})",
                        "$1\n    // Logic cleared\n$2"
                );
                formattedCode.append(formattedClass).append("\n\n");
            }
        }
        Pattern functionPattern = Pattern.compile(
                "\\b(?:int|string|double|float|void|bool|\\w+)\\s+\\w+\\s*\\([^)]*\\)\\s*\\{[\\s\\S]*?\\}",
                Pattern.MULTILINE
        );
        Matcher functionMatcher = functionPattern.matcher(code);
        while (functionMatcher.find()) {
            String functionDefinition = functionMatcher.group();

            String formattedFunction = functionDefinition.replaceAll(
                    "\\{[\\s\\S]*?\\}",
                    "{\n    // Logic cleared\n}"
            );
            formattedCode.append(formattedFunction).append("\n\n");
        }
        codeArea.replaceText(String.valueOf(formattedCode));
        appendToConsole("Code formatted successfully!");
    }


    private void appDocumentation() {
        TextArea docTextArea = new TextArea();
        docTextArea.setText(
                "Welcome to Enhanced IDE!\n\n" +
                        "Features:\n" +
                        "1. Code Editing: Write your C++ code with syntax highlighting.\n" +
                        "2. File Operations: Open, save, or create new files directly in the IDE.\n" +
                        "3. Run Code: Compile and execute C++ code with support for input/output.\n" +
                        "4. Debug Code: Debug your code using the integrated GDB support.\n" +
                        "5. Analyze Code: Analyze your code for metrics such as lines, words, and characters.\n" +
                        "6. Format Code: Automatically format your code for readability.\n" +
                        "7. Documentation: View app features and functionality.\n\n" +
                        "Keyboard Shortcuts:\n" +
                        "- Ctrl + S: Save the current file\n" +
                        "- Ctrl + O: Open a file\n" +
                        "- Ctrl + N: Create a new file\n" +
                        "- Ctrl + R: Run the code\n" +
                        "- Ctrl + D: Debug the code\n" +
                        "- Ctrl + Z: Undo\n" +
                        "- Ctrl + Y: Redo\n\n" +
                        "Developed by: Taz, Jamil, Rihin\n" +
                        "Version: 1.0\n" +
                        "License: MIST"
        );
        docTextArea.setEditable(false);
        docTextArea.setWrapText(true);
        docTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 18px;"); // Monospace font for better readability

        ScrollPane scrollPane = new ScrollPane(docTextArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(600, 400);

        Alert docAlert = new Alert(Alert.AlertType.INFORMATION);
        docAlert.setTitle("Application Documentation");
        docAlert.setHeaderText("Enhanced IDE Documentation");
        docAlert.getDialogPane().setContent(scrollPane);
        docAlert.getDialogPane().setPrefSize(620, 420);
        docAlert.showAndWait();
    }

    private void applySyntaxHighlighting(String text) {

        String keywordPattern = "\\b(int|double|float|char|bool|void|if|else|while|for|return|using|namespace|include|std|cin|cout|vector|endl|main|class|struct|template|typename|public|private|protected|const|static|virtual|inline|enum|union|decltype|auto|nullptr)\\b";
        String commentPattern = "//[^\n]*|/\\*(.|\\R)*?\\*/";
        String stringPattern = "\"([^\"\\\\]|\\\\.)*\"";
        String numberPattern = "\\b(\\d+\\.\\d*|\\d*\\.\\d+|\\d+|0x[0-9a-fA-F]+|0b[01]+|0[0-7]+)\\b";
        String headerPattern = "<[^>]{1,20}>";  // Limited to 20 characters
        String inbuildValueTypePattern = "\\b(true|false|NULL|INT_MAX|INT_MIN|SIZE_MAX|FLT_MAX|DBL_MAX|LDBL_MAX|NaN|INF)\\b";
        String symbolPattern = "[,\\=\\+\\-\\*\\/\\;\\#\\<>\\(\\)\\{\\}\\[\\]]|[&|!%<>\\?\\:\\=\\^\\~\\.,]";
        String variablePattern = "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b";
        String builtinPattern = "\\b(cin|cout|endl)\\b";
        String typePattern = "\\b(string|vector|list|deque|set|map|unordered_map|unordered_set|pair|queue|stack|array|bitset|forward_list|shared_ptr|unique_ptr|weak_ptr|tuple|complex)\\b";

        String stlFunctionPattern = "\\b([a-zA-Z_][a-zA-Z0-9_]*\\." +
                "pair\\.first|pair\\.second|" +
                "stack\\.push_back|stack\\.pop|stack\\.top|" +
                "queue\\.push|queue\\.pop|queue\\.front|queue\\.back|" +
                "vector\\.push_back|vector\\.pop_back|vector\\.insert|vector\\.erase|vector\\.begin|vector\\.end|vector\\.size|vector\\.clear|" +
                "list\\.push_back|list\\.pop_back|list\\.insert|list\\.erase|list\\.begin|list\\.end|" +
                "set\\.insert|set\\.erase|set\\.find|set\\.count|" +
                "unordered_set\\.insert|unordered_set\\.erase|unordered_set\\.find|unordered_set\\.count|" +
                "map\\.insert|map\\.erase|map\\.find|map\\.at|map\\.begin|map\\.end|" +
                "unordered_map\\.insert|unordered_map\\.erase|unordered_map\\.find|unordered_map\\.at|unordered_map\\.begin|unordered_map\\.end|" +
                "deque\\.push_back|deque\\.pop_back|deque\\.push_front|deque\\.pop_front|deque\\.begin|deque\\.end|deque\\.size|" +
                "array\\.at|array\\.begin|array\\.end|array\\.size|" +
                "bitset\\.set|bitset\\.reset|bitset\\.flip|bitset\\.size|bitset\\.count|" +
                "forward_list\\.push_front|forward_list\\.pop_front|forward_list\\.insert_after|forward_list\\.erase_after|forward_list\\.begin|forward_list\\.end|" +
                "shared_ptr\\.get|shared_ptr\\.reset|shared_ptr\\.use_count|" +
                "unique_ptr\\.get|unique_ptr\\.release|" +
                "weak_ptr\\.lock|" +
                "complex\\.real|complex\\.imag|complex\\.abs|complex\\.arg|" +
                "tuple\\.get|tuple\\.size|tuple\\.make_tuple|tuple\\.get<\\d+>" +
                ")\\b";

        String stlAlgorithmPattern = "\\b([a-zA-Z_][a-zA-Z0-9_]*\\." +
                "sort|find|binary_search|count|accumulate|reverse|shuffle|lower_bound|upper_bound" +
                ")\\b";

        // Combined pattern including the new inbuilt value pattern
        Pattern pattern = Pattern.compile(
                "(?<KEYWORD>" + keywordPattern + ")"
                        + "|(?<INBUILDVALUE>" + inbuildValueTypePattern + ")"
                        + "|(?<TYPE>" + typePattern + ")"
                        + "|(?<STLFUNCTION>" + stlFunctionPattern + ")"
                        + "|(?<STLALGORITHM>" + stlAlgorithmPattern + ")"
                        + "|(?<COMMENT>" + commentPattern + ")"
                        + "|(?<STRING>" + stringPattern + ")"
                        + "|(?<NUMBER>" + numberPattern + ")"
                        + "|(?<HEADER>" + headerPattern + ")"
                        + "|(?<SYMBOL>" + symbolPattern + ")"
                        + "|(?<VARIABLE>" + variablePattern + ")"
                        + "|(?<BUILTIN>" + builtinPattern + ")"
        );


        Matcher matcher = pattern.matcher(text);

        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastMatchEnd = 0;

        while (matcher.find()) {
            String styleClass = null;

            if (matcher.group("KEYWORD") != null) {
                styleClass = "keyword";
            } else if (matcher.group("INBUILDVALUE") != null) {
                styleClass = "inbuilt-value";
            } else if (matcher.group("COMMENT") != null) {
                styleClass = "comment";
            } else if (matcher.group("STRING") != null) {
                styleClass = "string";
            } else if (matcher.group("NUMBER") != null) {
                styleClass = "number";
            } else if (matcher.group("HEADER") != null) {
                styleClass = "header";
            } else if (matcher.group("SYMBOL") != null) {
                styleClass = "symbol";
            } else if (matcher.group("VARIABLE") != null) {
                styleClass = "variable";
            } else if (matcher.group("BUILTIN") != null) {
                styleClass = "builtin";
            } else if (matcher.group("TYPE") != null) {
                styleClass = "type";
            } else if (matcher.group("STLFUNCTION") != null) {
                styleClass = "stl-function";
            } else if (matcher.group("STLALGORITHM") != null) {
                styleClass = "stl-algorithm";
            }

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastMatchEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastMatchEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastMatchEnd);

        StyleSpans<Collection<String>> styleSpans = spansBuilder.create();
        codeArea.setStyleSpans(0, styleSpans);
    }


    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public static void main(String[] args) {
        launch(args);
    }
}