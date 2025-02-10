package com.codevisualizer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Orientation;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import java.io.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomIDE extends Application {

    private CodeArea codeArea;
    private TextArea outputArea;
    private TextArea inputArea;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem newFile = new MenuItem("New");
        fileMenu.getItems().addAll(newFile, openFile, saveFile);

        Menu runMenu = new Menu("Run");
        MenuItem runCode = new MenuItem("Run");
        runMenu.getItems().add(runCode);
        menuBar.getMenus().addAll(fileMenu, runMenu);

        Menu debugMenu = new Menu("Debug");
        MenuItem debugCode = new MenuItem("Debug");
        debugMenu.getItems().add(debugCode);
        menuBar.getMenus().add(debugMenu);


        Menu toolsMenu = new Menu("Tools");
        MenuItem formatCode = new MenuItem("Code Template");
        MenuItem analyzeCode = new MenuItem("Analyze Code");
        MenuItem deleteCode = new MenuItem("Clear Code");
        toolsMenu.getItems().add(analyzeCode);
        toolsMenu.getItems().add(formatCode);
        toolsMenu.getItems().add(deleteCode);
        menuBar.getMenus().add(toolsMenu);

        Menu settingMenu = new Menu("Setting");
        Menu themeMenu = new Menu("Theme");
        MenuItem lightMode = new MenuItem("Light Mode");
        MenuItem darkMode = new MenuItem("Dark Mode");
        themeMenu.getItems().addAll(lightMode, darkMode);
        settingMenu.getItems().add(themeMenu);
        menuBar.getMenus().add(settingMenu);


        Menu helpMenu = new Menu("Help");
        MenuItem aboutApp = new MenuItem("About");
        MenuItem documentation = new MenuItem("Documentation");
        helpMenu.getItems().add(documentation);
        helpMenu.getItems().add(aboutApp);
        menuBar.getMenus().add(helpMenu);


        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.textProperty().addListener((obs, oldText, newText) -> applySyntaxHighlighting(newText));
        codeArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 14px;");


        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: green;");
        outputArea.setPrefHeight(150);


        inputArea = new TextArea();
        inputArea.setPromptText("Enter input for your program here...");
        inputArea.setPrefHeight(100);


        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(codeArea, inputArea, outputArea);
        splitPane.setDividerPositions(0.6, 0.8);


        root.setTop(menuBar);
        root.setCenter(splitPane);


        openFile.setOnAction(e -> openFile(primaryStage));
        saveFile.setOnAction(e -> saveFile(primaryStage));
        newFile.setOnAction(e -> codeArea.clear());
        runCode.setOnAction(e -> runCode());
        debugCode.setOnAction(e -> debugCode());
        deleteCode.setOnAction(ActionEvente -> deleteCode());
        aboutApp.setOnAction(ActionEvent  -> detailsAboutIDE());
        formatCode.setOnAction(ActionEvent -> codeFormat());
        analyzeCode.setOnAction(ActionEvent -> codeAnalyze());
        documentation.setOnAction(ActionEvent -> appDocumentation());
        lightMode.setOnAction(Accordion -> applyLightMode());
        darkMode.setOnAction(ActionEvent -> applyDarkMode());



       
        primaryStage.setMaximized(true);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Enhanced IDE");
        primaryStage.show();
    }

    private void applyLightMode() {
        codeArea.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-size: 14px;");
    }

    private void applyDarkMode() {
        codeArea.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 14px;");
    }

    private void openFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                codeArea.clear();
                String line;
                while ((line = reader.readLine()) != null) {
                    codeArea.appendText(line + "\n");
                }
            } catch (IOException e) {
                showError("Error opening file", e.getMessage());
            }
        }
    }

    private void saveFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(codeArea.getText());
            } catch (IOException e) {
                showError("Error saving file", e.getMessage());
            }
        }
    }

    private void runCode() {
        try {
            String code = codeArea.getText();
            // Save the code temporarily to a file
            File tempFile = File.createTempFile("CustomIDE", ".cpp");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(code);
            }


            ProcessBuilder compileProcess = new ProcessBuilder("g++", tempFile.getAbsolutePath(), "-o", tempFile.getParent() + "/temp");
            Process compile = compileProcess.start();
            compile.waitFor();

            if (compile.exitValue() == 0) {
                ProcessBuilder runProcess = new ProcessBuilder(tempFile.getParent() + "/temp");
                runProcess.redirectErrorStream(true);

                Process run = runProcess.start();


                try (BufferedWriter processInput = new BufferedWriter(new OutputStreamWriter(run.getOutputStream()))) {
                    processInput.write(inputArea.getText());
                    processInput.flush();
                }


                BufferedReader reader = new BufferedReader(new InputStreamReader(run.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                outputArea.setText(output.toString());
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(compile.getErrorStream()));
                StringBuilder errorMessage = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorMessage.append(line).append("\n");
                }
                outputArea.setText("Compilation failed:\n" + errorMessage.toString());
            }

        } catch (Exception e) {
            outputArea.setText("Error running code: " + e.getMessage());
        }
    }
    private void debugCode() {
        new Thread(() -> {
            try {
                String code = codeArea.getText();
                System.out.println("Code to debug:\n" + code);

                File tempFile = File.createTempFile("CustomIDE", ".cpp");
                System.out.println("Temporary file path: " + tempFile.getAbsolutePath());

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                    writer.write(code);
                }

                ProcessBuilder compileProcess = new ProcessBuilder("g++", "-g", tempFile.getAbsolutePath(), "-o", tempFile.getParent() + "/temp");
                System.out.println("Compilation command: g++ -g " + tempFile.getAbsolutePath() + " -o " + tempFile.getParent() + "/temp");
                Process compile = compileProcess.start();
                compile.waitFor();

                if (compile.exitValue() == 0) {
                    System.out.println("Compilation with debug symbols successful.");

                    ProcessBuilder debugProcess = new ProcessBuilder("gdb", tempFile.getParent() + "/temp");
                    debugProcess.redirectErrorStream(true);

                    Process debug = debugProcess.start();

                    try (BufferedWriter debugInput = new BufferedWriter(new OutputStreamWriter(debug.getOutputStream()))) {
                        String userInput = inputArea.getText();
                        debugInput.write("break main\n"); // Set a breakpoint at main
                        debugInput.write("run\n");        // Run the program
                        debugInput.write(userInput + "\n"); // Additional user commands if needed
                        debugInput.flush();
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(debug.getInputStream()));
                    StringBuilder debugOutput = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        debugOutput.append(line).append("\n");
                    }

                    System.out.println("Debugger output:\n" + debugOutput);

                    Platform.runLater(() -> outputArea.setText(debugOutput.toString()));
                } else {
                    // Capture the error message from the compiler
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(compile.getErrorStream()));
                    StringBuilder errorMessage = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorMessage.append(line).append("\n");
                    }
                    System.err.println("Compilation errors: " + errorMessage);

                    Platform.runLater(() -> outputArea.setText("Compilation failed:\n" + errorMessage.toString()));
                }

                tempFile.delete();
                File tempBinary = new File(tempFile.getParent() + "/temp");
                if (tempBinary.exists()) {
                    tempBinary.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> outputArea.setText("Error debugging code: " + e.getMessage()));
            }
        }).start();
    }

    private void deleteCode() {
        codeArea.clear();
        outputArea.clear();
        inputArea.clear();
        outputArea.setText("Code, input, and output areas have been cleared.");
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

        // Metrics: Lines, Words, Characters
        int lines = code.split("\n").length;
        int words = code.split("\\s+").length;
        int characters = code.length();

        Set<String> variablesSet = new HashSet<>();
        Set<String> functionsSet = new HashSet<>();
        Set<String> classesSet = new HashSet<>();
        Set<String> dataStructuresSet = new HashSet<>();
        Set<String> algorithmsSet = new HashSet<>();

        Pattern variablePattern = Pattern.compile("\\b(int|double|float|char|string|long|short|bool)\\s+\\w+");
        Matcher variableMatcher = variablePattern.matcher(code);
        StringBuilder variables = new StringBuilder("Variables Created:\n");
        while (variableMatcher.find()) {
            String variable = variableMatcher.group();
            if (variablesSet.add(variable)) {  // Add only unique variables
                variables.append(variable).append("\n");
            }
        }

        Pattern classPattern = Pattern.compile("\\bclass\\s+\\w+");
        Matcher classMatcher = classPattern.matcher(code);
        StringBuilder classes = new StringBuilder("Classes Defined:\n");
        while (classMatcher.find()) {
            String className = classMatcher.group();
            if (classesSet.add(className)) {
                classes.append(className).append("\n");
            }
        }
        if (classes.length() == "Classes Defined:\n".length()) {
            classes.append("None\n");
        }

        Pattern functionPattern = Pattern.compile("\\b\\w+\\s+\\w+\\s*\\([^\\)]*\\)\\s*\\{");
        Matcher functionMatcher = functionPattern.matcher(code);
        StringBuilder functions = new StringBuilder("Functions Defined:\n");
        while (functionMatcher.find()) {
            String function = functionMatcher.group();
            if (functionsSet.add(function)) {
                functions.append(function).append("\n");
            }
        }
        if (functions.length() == "Functions Defined:\n".length()) {
            functions.append("None\n");
        }

        StringBuilder dataStructures = new StringBuilder("Data Structures Used:\n");
        if (code.contains("vector") && dataStructuresSet.add("Vector (Dynamic Array)")) {
            dataStructures.append("Vector (Dynamic Array)\n");
        }
        if (code.contains("list") && dataStructuresSet.add("List (Linked List)")) {
            dataStructures.append("List (Linked List)\n");
        }
        if (code.contains("map") && dataStructuresSet.add("Map (Hash Map)")) {
            dataStructures.append("Map (Hash Map)\n");
        }
        if (code.contains("set") && dataStructuresSet.add("Set (Hash Set)")) {
            dataStructures.append("Set (Hash Set)\n");
        }
        if (code.contains("array") && dataStructuresSet.add("Array")) {
            dataStructures.append("Array\n");
        }
        if (code.contains("stack") && dataStructuresSet.add("Stack")) {
            dataStructures.append("Stack\n");
        }
        if (code.contains("queue") && dataStructuresSet.add("Queue")) {
            dataStructures.append("Queue\n");
        }
        if (code.contains("priority_queue") && dataStructuresSet.add("Priority Queue")) {
            dataStructures.append("Priority Queue\n");
        }
        if (code.contains("deque") && dataStructuresSet.add("Deque (Double-ended Queue)")) {
            dataStructures.append("Deque (Double-ended Queue)\n");
        }
        if (dataStructures.length() == "Data Structures Used:\n".length()) {
            dataStructures.append("None\n");
        }

        StringBuilder algorithms = new StringBuilder("Algorithms Detected:\n");
        if (code.contains("sort") && algorithmsSet.add("Sorting Algorithm (e.g., std::sort)")) {
            algorithms.append("Sorting Algorithm (e.g., std::sort)\n");
        }
        if (code.contains("binary_search") && algorithmsSet.add("Binary Search Algorithm")) {
            algorithms.append("Binary Search Algorithm\n");
        }
        if (code.contains("reverse") && algorithmsSet.add("Reversal Algorithm (e.g., std::reverse)")) {
            algorithms.append("Reversal Algorithm (e.g., std::reverse)\n");
        }
        if (code.contains("merge") && algorithmsSet.add("Merge Sort Algorithm")) {
            algorithms.append("Merge Sort Algorithm\n");
        }
        if (code.contains("quick_sort") && algorithmsSet.add("Quick Sort Algorithm")) {
            algorithms.append("Quick Sort Algorithm\n");
        }
        if (code.contains("dijkstra") && algorithmsSet.add("Dijkstra's Shortest Path Algorithm")) {
            algorithms.append("Dijkstra's Shortest Path Algorithm\n");
        }
        if (code.contains("kruskal") && algorithmsSet.add("Kruskal's Minimum Spanning Tree Algorithm")) {
            algorithms.append("Kruskal's Minimum Spanning Tree Algorithm\n");
        }
        if (code.contains("prim") && algorithmsSet.add("Prim's Minimum Spanning Tree Algorithm")) {
            algorithms.append("Prim's Minimum Spanning Tree Algorithm\n");
        }
        if (code.contains("floyd_warshall") && algorithmsSet.add("Floyd-Warshall Algorithm (All-Pairs Shortest Path)")) {
            algorithms.append("Floyd-Warshall Algorithm (All-Pairs Shortest Path)\n");
        }
        if (algorithms.length() == "Algorithms Detected:\n".length()) {
            algorithms.append("None\n");
        }

        String feedback = "Feedback:\n"
                + "1. Ensure variable names are meaningful and self-explanatory.\n"
                + "2. Use comments to describe complex logic.\n"
                + "3. Follow consistent indentation and spacing for readability.\n"
                + "4. Avoid deeply nested loops; consider breaking into smaller functions.\n"
                + "5. Use appropriate data structures and algorithms for efficient code.\n";

        String analysis = String.format(
                "Code Analysis:\nLines: %d\nWords: %d\nCharacters: %d\n\n%s\n%s\n%s\n%s\n%s\n\n%s",
                lines, words, characters, variables.toString(), classes.toString(), functions.toString(),
                dataStructures.toString(), algorithms.toString(), feedback);
        outputArea.setText(analysis);
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
        String finalFormattedCode = formattedCode.toString().trim();
        codeArea.replaceText(finalFormattedCode);
        outputArea.setText("Code formatted successfully!");
    }


    private void appDocumentation() {
        TextArea docTextArea = new TextArea();
        docTextArea.setText(
                "Welcome to Enhanced IDE!\n\n"
                        + "Features:\n"
                        + "1. Code Editing: Write your C++ code with syntax highlighting.\n"
                        + "2. File Operations: Open, save, or create new files directly in the IDE.\n"
                        + "3. Run Code: Compile and execute C++ code with support for input/output.\n"
                        + "4. Debug Code: Debug your code using the integrated GDB support.\n"
                        + "5. Analyze Code: Analyze your code for metrics such as lines, words, and characters.\n"
                        + "6. Format Code: Automatically format your code for readability.\n"
                        + "7. Documentation: View app features and functionality.\n\n"
                        + "Developed by: Taz, Jamil, Rihin\nVersion: 1.0"
        );
        docTextArea.setEditable(false);
        docTextArea.setWrapText(true);
        ScrollPane scrollPane = new ScrollPane(docTextArea);
        scrollPane.setFitToWidth(true);
        Alert docAlert = new Alert(Alert.AlertType.INFORMATION);
        docAlert.setTitle("Application Documentation");
        docAlert.setHeaderText("Enhanced IDE Documentation");
        docAlert.getDialogPane().setContent(scrollPane);
        docAlert.showAndWait();
    }

    private void applySyntaxHighlighting(String text) {
        String keywordPattern = "\\b(int|double|float|char|if|else|while|for|return|using|namespace|include|std|cin|cout|vector|endl|main)\\b";
        String commentPattern = "//[^\n]*|/\\*(.|\\R)*?\\*/";
        String stringPattern = "\"([^\"\\\\]|\\\\.)*\"";

        Pattern pattern = Pattern.compile(
                "(?<KEYWORD>" + keywordPattern + ")"
                        + "|(?<COMMENT>" + commentPattern + ")"
                        + "|(?<STRING>" + stringPattern + ")"
        );

        Matcher matcher = pattern.matcher(text);

        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastMatchEnd = 0;

        while (matcher.find()) {
            String style = null;
            if (matcher.group("KEYWORD") != null) {
                style = "-fx-fill: blue; -fx-font-weight: bold;";
            } else if (matcher.group("COMMENT") != null) {
                style = "-fx-fill: green; -fx-font-style: italic;";
            } else if (matcher.group("STRING") != null) {
                style = "-fx-fill: orange;";
            }

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastMatchEnd);
            spansBuilder.add(Collections.singleton(style), matcher.end() - matcher.start());
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