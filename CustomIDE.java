package com.codevisualizer;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.geometry.Orientation;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomIDE extends Application {

    private CodeArea codeArea;
    private TextArea outputArea;
    private TextArea inputArea;
    private Pane visualizationPane;

    @Override
    public void start(Stage primaryStage) {
        // Main layout
        BorderPane root = new BorderPane();

        // Menu bar
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

        // Code editing area with syntax highlighting
        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.textProperty().addListener((obs, oldText, newText) -> applySyntaxHighlighting(newText));
        codeArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 14px;");

        // Output area
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: green;");
        outputArea.setPrefHeight(150);

        // Input area
        inputArea = new TextArea();
        inputArea.setPromptText("Enter input for your program here...");
        inputArea.setPrefHeight(100);
        
        visualizationPane = new Pane();
        visualizationPane.setPrefSize(800, 400); // Set preferred size

        
        // Layout
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(codeArea, inputArea, outputArea);
        splitPane.setDividerPositions(0.6, 0.8);

        // Add components to root layout
        root.setTop(menuBar);
        root.setCenter(splitPane);

        // Menu actions
        openFile.setOnAction(e -> openFile(primaryStage));
        saveFile.setOnAction(e -> saveFile(primaryStage));
        newFile.setOnAction(e -> codeArea.clear());
        runCode.setOnAction(e -> runCode());

        // Scene and stage setup
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Custom IDE with Syntax Highlighting and Full Features");
        primaryStage.show();
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

            // Compile and run the C++ code
            ProcessBuilder compileProcess = new ProcessBuilder("g++", tempFile.getAbsolutePath(), "-o", tempFile.getParent() + "/temp");
            Process compile = compileProcess.start();
            compile.waitFor();

            if (compile.exitValue() == 0) {
                ProcessBuilder runProcess = new ProcessBuilder(tempFile.getParent() + "/temp");
                runProcess.redirectErrorStream(true);

                Process run = runProcess.start();

                // Provide input to the running process
                try (BufferedWriter processInput = new BufferedWriter(new OutputStreamWriter(run.getOutputStream()))) {
                    processInput.write(inputArea.getText());
                    processInput.flush();
                }

                // Capture output
                BufferedReader reader = new BufferedReader(new InputStreamReader(run.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                outputArea.setText(output.toString());
            } else {
                // Capture the error message from the compiler process
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
    private void visualizeCode() {
        visualizationPane.getChildren().clear();
        String code = codeArea.getText();

        // Split code into lines for parsing
        String[] lines = code.split("\n");
        int x = 50;  // Initial X-coordinate
        int y = 50;  // Initial Y-coordinate
        int boxWidth = 200; // Box width
        int boxHeight = 50; // Box height
        int ySpacing = 80;  // Vertical spacing between boxes

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.matches("\\b(int|double|float|char|string)\\b.*;")) {
                // Variable declaration
                String[] parts = line.split("=");
                String declaration = parts[0].trim();
                String value = parts.length > 1 ? parts[1].replace(";", "").trim() : "null";

                // Create rectangle for variable
                Rectangle box = new Rectangle(x, y, boxWidth, boxHeight);
                box.setStyle("-fx-fill: lightblue; -fx-stroke: black; -fx-stroke-width: 2;");

                // Add label for variable
                Text label = new Text(x + 10, y + 30, declaration + " = " + value);

                visualizationPane.getChildren().addAll(box, label);
                y += ySpacing;
            } else if (line.startsWith("if") || line.startsWith("for") || line.startsWith("while")) {
                // Conditional or loop block
                Rectangle box = new Rectangle(x, y, boxWidth, boxHeight);
                box.setStyle("-fx-fill: lightgreen; -fx-stroke: black; -fx-stroke-width: 2;");
                Text label = new Text(x + 10, y + 30, line);

                visualizationPane.getChildren().addAll(box, label);
                y += ySpacing;
            } else if (line.startsWith("return")) {
                // Return statement
                Rectangle box = new Rectangle(x, y, boxWidth, boxHeight);
                box.setStyle("-fx-fill: lightcoral; -fx-stroke: black; -fx-stroke-width: 2;");
                Text label = new Text(x + 10, y + 30, line);

                visualizationPane.getChildren().addAll(box, label);
                y += ySpacing;
            } else {
                // General statements
                Rectangle box = new Rectangle(x, y, boxWidth, boxHeight);
                box.setStyle("-fx-fill: white; -fx-stroke: black; -fx-stroke-width: 2;");
                Text label = new Text(x + 10, y + 30, line);

                visualizationPane.getChildren().addAll(box, label);
                y += ySpacing;
            }

            // Wrap rows if needed
            if (y > visualizationPane.getHeight() - 100) {
                y = 50;
                x += boxWidth + 50;
            }
        }
    }

    private void applySyntaxHighlighting(String text) {
        // Regex patterns for C++ keywords, comments, and strings
        String keywordPattern = "\\b(int|double|float|char|if|else|while|for|return|using|namespace|include|std|cin|cout|vector|endl|main)\\b";
        String commentPattern = "//[^\n]*|/\\*(.|\\R)*?\\*/";
        String stringPattern = "\"([^\"\\\\]|\\\\.)*\"";

        Pattern pattern = Pattern.compile(
            "(?<KEYWORD>" + keywordPattern + ")"
            + "|(?<COMMENT>" + commentPattern + ")"
            + "|(?<STRING>" + stringPattern + ")"
        );

        Matcher matcher = pattern.matcher(text);

        // Using a StyleSpansBuilder to construct style spans
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
