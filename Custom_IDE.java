import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.geometry.Orientation;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

public class Main extends Application {

    private CodeArea codeArea;
    private TextArea outputArea;
    private TextArea inputArea;
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


                // Create the "Files" label
                Label filesLabel = new Label("File Section");
                filesLabel.setStyle(
                        "-fx-font-size: 18px; " +  // Larger font size for the section title
                                "-fx-font-family: 'Segoe UI', sans-serif; " +  // Modern font family
                                "-fx-font-weight: bold; " +  // Bold text
                                "-fx-text-fill: #2c3e50; " +  // Darker text color for better contrast (matching fileListView theme)
                                "-fx-background-color: rgba(173, 216, 230, 0.6); " +  // Light sky-blue background similar to fileListView
                                "-fx-padding: 10px 15px; " +  // Padding inside the label
                                "-fx-border-radius: 5px;"  // Rounded corners for the label
                );

                fileListView = new ListView<>();
                fileListView.setPrefWidth(80);
                fileListView.setMinWidth(60);
                fileListView.getItems().add(currentFileName);
                fileContentMap.put(currentFileName, "");
                initializeFileListView();

                VBox leftPane = new VBox(filesLabel, fileListView);
                leftPane.setPrefWidth(90);
                leftPane.setMinWidth(70);
                leftPane.setStyle(
                        "-fx-background-color: #f4f4f4; " +  // Light gray background for the VBox
                                "-fx-padding: 0px; " +  // No extra padding
                                "-fx-spacing: 0px;"  // No extra space between label and file list
                );
                filesLabel.setMaxWidth(Double.MAX_VALUE);
                filesLabel.setStyle(filesLabel.getStyle() + "-fx-alignment: center-left; ");
                leftPane.setFillWidth(true);


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

        // Load the CSS file
        menuBar.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem newFile = new MenuItem("New");
        MenuItem closeFile = new MenuItem("Close");
        fileMenu.getItems().addAll(newFile, openFile, saveFile, closeFile);

        // Run Menu
        Menu runMenu = new Menu("Run");
        MenuItem runCode = new MenuItem("Run");
        runMenu.getItems().add(runCode);

        // Debug Menu
        Menu debugMenu = new Menu("Debug");
        MenuItem debugCode = new MenuItem("Debug");
        debugMenu.getItems().add(debugCode);

        // Tools Menu
        Menu toolsMenu = new Menu("Tools");
        MenuItem formatCode = new MenuItem("Code Template");
        MenuItem analyzeCode = new MenuItem("Analyze Code");
        MenuItem deleteCode = new MenuItem("Clear Code");
        toolsMenu.getItems().addAll(formatCode, analyzeCode, deleteCode);

        // Setting Menu
        Menu settingMenu = new Menu("Setting");
        Menu themeMenu = new Menu("Theme");
        MenuItem lightMode = new MenuItem("Light Mode");
        MenuItem darkMode = new MenuItem("Dark Mode");
        themeMenu.getItems().addAll(lightMode, darkMode);
        settingMenu.getItems().add(themeMenu);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutApp = new MenuItem("About");
        MenuItem documentation = new MenuItem("Documentation");
        helpMenu.getItems().addAll(documentation, aboutApp);

        // Add Menus to MenuBar
        menuBar.getMenus().addAll(fileMenu, runMenu, debugMenu, toolsMenu, settingMenu, helpMenu);

        // Event Handlers
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

        // Default font size
        final IntegerProperty fontSize = new SimpleIntegerProperty(14);

        // Apply initial style
        codeArea.styleProperty().bind(Bindings.concat(
                "-fx-font-family: 'Courier New', 'Courier', 'monospace'; ",
                "-fx-font-size: ", fontSize.asString(), "px; ",
                "-fx-text-fill: #000000;" // Changed text color to black for better visibility
        ));

        // Handle Zoom In / Zoom Out using Mouse Scroll + Ctrl
        codeArea.setOnScroll(event -> {
            if (event.isControlDown()) {
                if (event.getDeltaY() > 0) { // Scroll Up -> Zoom In
                    fontSize.set(Math.min(fontSize.get() + 2, 30));
                } else { // Scroll Down -> Zoom Out
                    fontSize.set(Math.max(fontSize.get() - 2, 10));
                }
            }
        });

        // Handle Zoom In / Zoom Out using Keyboard Shortcuts (Ctrl + '+' and Ctrl + '-')
        codeArea.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case EQUALS: // Ctrl + '=' (Zoom In)
                    case PLUS: // Ctrl + '+'
                        fontSize.set(Math.min(fontSize.get() + 2, 30));
                        break;
                    case MINUS: // Ctrl + '-'
                        fontSize.set(Math.max(fontSize.get() - 2, 10));
                        break;
                    default:
                        return;
                }
            }
        });

        codeArea.getStyleClass().add("light-mode");
        codeArea.getStylesheets().add(getClass().getResource("/Main.css").toExternalForm());

        return codeArea;
    }



    private SplitPane createSplitPane() {
        // Create output area
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-control-inner-background: black; " +
                "-fx-text-fill: white; " +
                "-fx-font-family: 'Courier New'; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: transparent; " +
                "-fx-border-width: 1.5px; " +
                "-fx-padding: 0px;");
        outputArea.setPrefHeight(150);

        // Create input area
        inputArea = new TextArea();
        inputArea.setPromptText("Enter input here...");
        inputArea.setStyle(
                "-fx-control-inner-background: #ced9d7; " + /* Soft blue background */
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: #89CFF0; " + /* Soft blue border */
                        "-fx-border-width: 2px; " +
                        "-fx-background-radius: 8px; " + /* Rounded corners */
                        "-fx-padding: 3px; " +
                        "-fx-prompt-text-fill: rgba(44, 62, 80, 0.5);"
        );

        // Hover effect - Brighter blue glow
        inputArea.setOnMouseEntered(e -> inputArea.setStyle(
                "-fx-control-inner-background: #ced9d7; " + /* Slightly darker blue */
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-size: 14px; " +/* Brighter blue border */
                        "-fx-border-width: 3px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-padding: 3px; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 255, 0.3), 8, 0, 0, 3);" + /* Blue glow */
                        "-fx-prompt-text-fill: rgba(44, 62, 80, 0.6);"
        ));

        // Reset hover effect
        inputArea.setOnMouseExited(e -> inputArea.setStyle(
                "-fx-control-inner-background: #dce6e4; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-width: 2px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-padding: 3px; " +
                        "-fx-prompt-text-fill: rgba(44, 62, 80, 0.5);"
        ));

        // Code area
        codeArea = createCodeArea();
        VirtualizedScrollPane<CodeArea> scrollableCodeArea = new VirtualizedScrollPane<>(codeArea);

        // Create SplitPane
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);

        // **Hide divider initially & add blue hover effect**
        splitPane.setStyle("-fx-divider-color: transparent;");

        // Hover - Soft Blue Divider
        splitPane.setOnMouseEntered(e -> splitPane.setStyle("-fx-divider-color: rgba(30, 144, 255, 0.5);")); // DodgerBlue

        // Click Effect - Glowing Blue
        splitPane.setOnMousePressed(e -> splitPane.setStyle("-fx-divider-color: rgba(0, 102, 204, 1);")); // Deep Blue

        // Release - Fade Out Effect
        splitPane.setOnMouseReleased(e -> {
            new Timeline(new KeyFrame(Duration.millis(300), evt ->
                    splitPane.setStyle("-fx-divider-color: transparent;"))
            ).play();
        });

        // Add areas to the split pane
        splitPane.getItems().addAll(scrollableCodeArea, inputArea, outputArea);

        // Set divider positions (code gets 70%, input/output get 30%)
        splitPane.setDividerPositions(0.7, 0.85);

        return splitPane;
    }



    private void switchToFile(String newFileName) {
        fileContentMap.put(currentFileName, codeArea.getText());
        currentFileName = newFileName;
        codeArea.replaceText(fileContentMap.getOrDefault(newFileName, ""));
        fileListView.getSelectionModel().select(newFileName);
    }

    private void initializeFileListView() {
        fileListView.setCellFactory(lv -> new ListCell<String>() {
            private final HBox hbox = new HBox();
            private final Label fileNameLabel = new Label();
            private final Label closeButton = new Label("✕"); // Unicode for 'X'
            private final Region spacer = new Region(); // Spacer to push the close button to the end

            {
                // Styling the close button
                closeButton.setStyle(
                        "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 12px; " +
                                "-fx-cursor: hand; " +
                                "-fx-padding: 0 0 0 5px;"
                );

                // Hover effect for the close button
                closeButton.setOnMouseEntered(e -> closeButton.setStyle(
                        "-fx-text-fill: red; " +
                                "-fx-font-size: 12px; " +
                                "-fx-cursor: hand; " +
                                "-fx-padding: 0 0 0 5px;"
                ));

                closeButton.setOnMouseExited(e -> closeButton.setStyle(
                        "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 12px; " +
                                "-fx-cursor: hand; " +
                                "-fx-padding: 0 0 0 5px;"
                ));

                // Action when close button is clicked
                closeButton.setOnMouseClicked(e -> {
                    String item = getItem();
                    if (item != null) {
                        fileListView.getItems().remove(item);
                        if (item.equals(currentFileName)) {
                            currentFileName = null;
                            codeArea.clear();
                        }
                    }
                });

                // Configure the HBox layout
                HBox.setHgrow(spacer, Priority.ALWAYS); // Spacer will take up all available space
                hbox.getChildren().addAll(fileNameLabel, spacer, closeButton);
                hbox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    fileNameLabel.setText(item);
                    setGraphic(hbox);
                    setStyle(
                            "-fx-padding: 10px 14px; " +
                                    "-fx-font-size: 14px; " + // Default font size
                                    "-fx-font-family: 'Segoe UI', sans-serif; " +
                                    "-fx-text-fill: #2c3e50; " + /* Dark blue text */
                                    "-fx-background-color: transparent; " + // Transparent background for items
                                    "-fx-transition: all 0.3s ease-in-out;"
                    );

                    // Hover Effect: Light glow around the item with shadow
                    setOnMouseEntered(e -> {
                        if (isSelected()) {
                            // Apply both selection and hover styles
                            setStyle(
                                    "-fx-background-color: rgba(173, 216, 230, 0.5); " + /* Slightly transparent light blue */
                                            "-fx-text-fill: white; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-font-size: 16px; " + // Slightly larger font size
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 15, 0, 0, 5);" // Darker and more prominent shadow
                            );
                        } else {
                            // Apply only hover style
                            setStyle(
                                    "-fx-background-color: rgba(173, 216, 230, 0.3); " + /* Soft transparent light blue */
                                            "-fx-text-fill: white; " +
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: dropshadow(three-pass-box, rgba(173, 216, 230, 0.7), 12, 0, 0, 5);"  // Subtle shadow for glowing effect
                            );
                        }
                    });

                    setOnMouseExited(e -> {
                        if (isSelected()) {
                            // Revert to selection style only
                            setStyle(
                                    "-fx-background-color: rgba(0, 102, 204, 0.4); " + /* Slightly transparent deep blue */
                                            "-fx-text-fill: white; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-font-size: 16px; " + // Slightly larger font size
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 15, 0, 0, 5);" // Darker and more prominent shadow
                            );
                        } else {
                            // Revert to default style
                            setStyle(
                                    "-fx-background-color: transparent; " +
                                            "-fx-text-fill: #2c3e50; " +
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: none;"
                            );
                        }
                    });

                    // Selection Effect: Deep blue with stronger glow, larger font size, and darker shadow
                    selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                        if (isSelected) {
                            setStyle(
                                    "-fx-background-color: rgba(0, 102, 204, 0.4); " + /* Slightly transparent deep blue */
                                            "-fx-text-fill: white; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-font-size: 16px; " + // Slightly larger font size
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 15, 0, 0, 5);" // Darker and more prominent shadow
                            );
                        } else {
                            setStyle(
                                    "-fx-background-color: transparent; " +
                                            "-fx-text-fill: #2c3e50; " +
                                            "-fx-font-size: 14px; " + // Reset to default font size
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: none;"
                            );
                        }
                    });
                }
            }
        });

        // Customizing File ListView Appearance
        fileListView.setStyle(
                "-fx-background-color: rgba(173, 216, 230, 0.2); " + // Slightly transparent light blue background
                        "-fx-border-color: transparent; " + // Remove border
                        "-fx-padding: 0; " + // Remove padding
                        "-fx-effect: none;" // Remove shadow effect
        );

        // Selection Handling
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(currentFileName)) {
                switchToFile(newVal);
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
            String newFileName = file.getName();
            if (!fileListView.getItems().contains(newFileName)) {
                fileListView.getItems().add(newFileName);
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                fileContentMap.put(newFileName, content.toString());
                switchToFile(newFileName);
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

                boolean isUntitled = currentFileName.startsWith("Untitled");
                if (isUntitled) {
                    fileListView.getItems().remove(currentFileName);
                    fileContentMap.remove(currentFileName);
                }

                String newFileName = file.getName();
                fileListView.getItems().add(newFileName);
                fileContentMap.put(newFileName, content);
                switchToFile(newFileName);
            } catch (IOException e) {
                showError("Error saving file", e.getMessage());
            }
        }
    }

    private void newFile(Stage stage) {
        String newFileName = "Untitled" + (fileListView.getItems().size() + 1);
        fileListView.getItems().add(newFileName);
        fileContentMap.put(newFileName, "");
        switchToFile(newFileName);
    }

    private void closeFile(Stage stage) {
        if (hasUnsavedChanges()) {
            promptSaveChanges(stage);
        }

        int currentFileIndex = fileListView.getItems().indexOf(currentFileName);
        fileListView.getItems().remove(currentFileName);
        fileContentMap.remove(currentFileName);

        if (fileListView.getItems().isEmpty()) {
            String newFileName = "Untitled1";
            fileListView.getItems().add(newFileName);
            fileContentMap.put(newFileName, "");
            switchToFile(newFileName);
        } else {
            int previousFileIndex = Math.max(0, currentFileIndex - 1);
            String newFileName = fileListView.getItems().get(previousFileIndex);
            switchToFile(newFileName);
        }
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
        try {
            String code = codeArea.getText();

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

        // Basic metrics
        int lines = code.split("\n").length;
        int words = code.split("\\s+").length;
        int characters = code.length();

        // Data structures for analysis
        Set<String> variablesSet = new HashSet<>();
        Set<String> functionsSet = new HashSet<>();
        Set<String> classesSet = new HashSet<>();
        Set<String> dataStructuresSet = new HashSet<>();
        Set<String> algorithmsSet = new HashSet<>();

        // Regex patterns
        Pattern variablePattern = Pattern.compile("\\b(int|double|float|char|string|long|short|bool|auto)\\s+(\\w+)\\b(?!\\s*\\()");

        Pattern classPattern = Pattern.compile("\\b(class|struct)\\s+(\\w+)");
        Pattern functionPattern = Pattern.compile("\\b(\\w+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:\\{|\\n)");
        Pattern dataStructurePattern = Pattern.compile("\\b(vector|list|deque|set|map|unordered_map|unordered_set|stack|queue|priority_queue|array|bitset|forward_list|shared_ptr|unique_ptr|weak_ptr|tuple|pair)\\b");
        Pattern algorithmPattern = Pattern.compile("\\b(sort|find|binary_search|count|accumulate|reverse|shuffle|lower_bound|upper_bound|merge|quick_sort|dijkstra|kruskal|prim|floyd_warshall)\\b");

        // Analyze variables
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

        // Analyze classes and structs
        Matcher classMatcher = classPattern.matcher(code);
        StringBuilder classes = new StringBuilder("Classes/Structs Defined:\n");
        while (classMatcher.find()) {
            String className = classMatcher.group(2); // Capture class/struct name
            if (classesSet.add(className)) {
                classes.append(className).append("\n");
            }
        }
        if (classesSet.isEmpty()) {
            classes.append("None\n");
        }

        // Analyze functions
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

        // Analyze data structures
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

        // Analyze algorithms
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

        // Feedback for code quality
        String feedback = "Feedback:\n"
                + "1. Ensure variable names are meaningful and self-explanatory.\n"
                + "2. Use comments to describe complex logic.\n"
                + "3. Follow consistent indentation and spacing for readability.\n"
                + "4. Avoid deeply nested loops; consider breaking into smaller functions.\n"
                + "5. Use appropriate data structures and algorithms for efficient code.\n"
                + "6. Avoid global variables; prefer local scope where possible.\n"
                + "7. Use const-correctness for variables and functions where applicable.\n";

        // Compile analysis results
        String analysis = String.format(
                "Code Analysis:\nLines: %d\nWords: %d\nCharacters: %d\n\n%s\n%s\n%s\n%s\n%s\n\n%s",
                lines, words, characters, variables.toString(), classes.toString(), functions.toString(),
                dataStructures.toString(), algorithms.toString(), feedback);

        // Display results in the output area// Replace with actual analysis
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
        // Replace with actual formatted code
        codeArea.replaceText(String.valueOf(formattedCode));
        outputArea.setText("Code formatted successfully!");
    }


    private void appDocumentation() {
        // Create a TextArea for documentation
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
        docTextArea.setEditable(false); // Make it read-only
        docTextArea.setWrapText(true); // Enable text wrapping
        docTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 18px;"); // Monospace font for better readability

        // Create a ScrollPane to handle overflow
        ScrollPane scrollPane = new ScrollPane(docTextArea);
        scrollPane.setFitToWidth(true); // Fit the width of the TextArea
        scrollPane.setPrefSize(600, 400); // Set preferred size for the dialog

        // Create an Alert dialog for documentation
        Alert docAlert = new Alert(Alert.AlertType.INFORMATION);
        docAlert.setTitle("Application Documentation");
        docAlert.setHeaderText("Enhanced IDE Documentation");
        docAlert.getDialogPane().setContent(scrollPane); // Add the ScrollPane to the dialog
        docAlert.getDialogPane().setPrefSize(620, 420); // Set preferred size for the dialog pane

        // Show the dialog and wait for user interaction
        docAlert.showAndWait();
    }

    private void applySyntaxHighlighting(String text) {

        String keywordPattern = "\\b(int|double|float|char|bool|void|if|else|while|for|return|using|namespace|include|std|cin|cout|vector|endl|main|class|struct|template|typename|public|private|protected|const|static|virtual|inline|enum|union|decltype|auto|nullptr)\\b";
        String commentPattern = "//[^\n]*|/\\*(.|\\R)*?\\*/";
        String stringPattern = "\"([^\"\\\\]|\\\\.)*\"";
        String numberPattern = "\\b(\\d+\\.\\d*|\\d*\\.\\d+|\\d+|0x[0-9a-fA-F]+|0b[01]+|0[0-7]+)\\b";
        String headerPattern = "#\\s*include\\s*.*";
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