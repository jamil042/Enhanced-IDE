import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.geometry.Orientation;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import javafx.scene.control.TextArea;

public class CustomIDE extends Application {

    private CodeArea codeArea;
    private TextArea outputArea;
    private TextArea inputArea;
    private ListView<String> fileListView;
    private String currentFileName = "Untitled1";
    private int currentLineIndex = 0;
    private List<String> codeLines = new ArrayList<>();
    private Pane visualizationPane;
    private Label currentLineLabel;
    private SplitPane mainSplitPane;
    private boolean isVisualizationVisible = false;
    private VBox visualizationWorkarea;
    private Map<String, Object> variables = new HashMap<>();
    private Map<String, Rectangle> variableBoxes = new HashMap<>();
    private Map<String, Text> variableTexts = new HashMap<>();
    private Map<String, String> fileContentMap = new HashMap<>();
    private IntegerProperty currentLineProperty = new SimpleIntegerProperty(-1);


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
                        "-fx-font-size: 18px; " +
                                "-fx-font-family: 'Segoe UI', sans-serif; " +
                                "-fx-font-weight: bold; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-background-color: rgba(173, 216, 230, 0.6); " +
                                "-fx-padding: 10px 15px; " +
                                "-fx-border-radius: 5px;"
                );

                fileListView = new ListView<>();
                fileListView.setPrefWidth(80);
                fileListView.setMinWidth(60);
                fileListView.getItems().add(currentFileName);
                fileContentMap.put(currentFileName, "");
                initializeFileListView();

                Image bottomImage = new Image(getClass().getResourceAsStream("sample.jpg"));
                ImageView bottomImageView = new ImageView(bottomImage);
                bottomImageView.setFitWidth(300);
                bottomImageView.setPreserveRatio(true);

                VBox leftPane = new VBox(filesLabel, fileListView, bottomImageView);
                leftPane.setPrefWidth(90);
                leftPane.setMinWidth(70);
                leftPane.setStyle(
                        "-fx-background-color: #f4f4f4; " +
                                "-fx-padding: 0px; " +
                                "-fx-spacing: 0px;"
                );
                filesLabel.setMaxWidth(Double.MAX_VALUE);
                filesLabel.setStyle(filesLabel.getStyle() + "-fx-alignment: center-left; ");
                leftPane.setFillWidth(true);
                VBox.setVgrow(bottomImageView, Priority.ALWAYS);
                SplitPane centerSplitPane = createSplitPane();
                mainSplitPane = new SplitPane();
                mainSplitPane.setOrientation(Orientation.HORIZONTAL);
                mainSplitPane.getItems().addAll(leftPane, centerSplitPane);
                mainSplitPane.setDividerPositions(0.02);
                visualizationWorkarea = createVisualizationWorkarea();
                currentLineProperty.addListener((obs, oldVal, newVal) -> {
                    Platform.runLater(() -> {
                        codeArea.requestLayout();
                    });
                });
                root.setTop(menuBar);
                root.setCenter(mainSplitPane);
                Image Icon = new Image(getClass().getResourceAsStream("IDE_Logo.png"));
                Scene scene = new Scene(root, 1072, 600);
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


    private VBox createVisualizationWorkarea() {
        VBox visualizationBox = new VBox(10);
        visualizationBox.setPadding(new Insets(10));
        visualizationBox.setStyle("-fx-background-color: #cdf7e9; -fx-border-color: #ccc; -fx-border-width: 1px;");

        currentLineLabel = new Label("Current Line:");
        currentLineLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        currentLineLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-background-color: #16a085; " +
                        "-fx-padding: 10px 15px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #1abc9c; " +
                        "-fx-border-width: 2px; "
        );

        DropShadow redGlow = new DropShadow();
        redGlow.setColor(Color.rgb(255, 68, 68, 0.8));
        redGlow.setRadius(10);
        redGlow.setSpread(0.6);

        Button closeButton = new Button("✕");
        closeButton.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-color: #ff4444; " +
                        "-fx-padding: 5px 10px; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px; " +
                        "-fx-cursor: hand;"
        );

        closeButton.setOnMouseEntered(e -> {
            closeButton.setStyle(
                    "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-color: #f74d4d; " +
                            "-fx-padding: 5px 10px; " +
                            "-fx-border-radius: 5px; " +
                            "-fx-background-radius: 5px; " +
                            "-fx-cursor: hand;"
            );
            closeButton.setEffect(redGlow);
        });

        closeButton.setOnMouseExited(e -> {
            closeButton.setStyle(
                    "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-color: #ff4444; " +
                            "-fx-padding: 5px 10px; " +
                            "-fx-border-radius: 5px; " +
                            "-fx-background-radius: 5px; " +
                            "-fx-cursor: hand;"
            );
            closeButton.setEffect(null);
        });

        closeButton.setOnAction(e -> toggleVisualization());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(10, currentLineLabel, spacer, closeButton);
        topBar.setAlignment(Pos.CENTER_LEFT);

        visualizationPane = new Pane();
        visualizationPane.setMinSize(800, 500);
        visualizationPane.setStyle("-fx-background-color: #e1eff7;");
        ScrollPane scrollPane = new ScrollPane(visualizationPane);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);

        scrollPane.setStyle("-fx-background: #f0f0f0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        DropShadow glowEffect = new DropShadow();
        glowEffect.setColor(Color.rgb(76, 175, 80, 0.8));
        glowEffect.setRadius(10);
        glowEffect.setSpread(0.5);

        Button nextButton = new Button("Next");
        nextButton.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-color: #4CAF50; " +
                        "-fx-padding: 8px 16px; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px; " +
                        "-fx-cursor: hand;"
        );
        nextButton.setOnMouseEntered(e -> {
            nextButton.setStyle(
                    "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-color: #63c967; " +
                            "-fx-padding: 8px 16px; " +
                            "-fx-border-radius: 5px; " +
                            "-fx-background-radius: 5px; " +
                            "-fx-cursor: hand;"
            );
            nextButton.setEffect(glowEffect);
        });
        nextButton.setOnMouseExited(e -> {
            nextButton.setStyle(
                    "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-color: #4CAF50; " +
                            "-fx-padding: 8px 16px; " +
                            "-fx-border-radius: 5px; " +
                            "-fx-background-radius: 5px; " +
                            "-fx-cursor: hand;"
            );
            nextButton.setEffect(null);
        });
        nextButton.setOnAction(e -> visualizeNextLine());

        Button prevButton = new Button("Previous");
        prevButton.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-color: #4CAF50; " +
                        "-fx-padding: 8px 16px; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px; " +
                        "-fx-cursor: hand;"
        );
        prevButton.setOnMouseEntered(e -> {
            prevButton.setStyle(
                    "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-color: #63c967; " +
                            "-fx-padding: 8px 16px; " +
                            "-fx-border-radius: 5px; " +
                            "-fx-background-radius: 5px; " +
                            "-fx-cursor: hand;"
            );
            prevButton.setEffect(glowEffect);
        });
        prevButton.setOnMouseExited(e -> {
            prevButton.setStyle(
                    "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-color: #4CAF50; " +
                            "-fx-padding: 8px 16px; " +
                            "-fx-border-radius: 5px; " +
                            "-fx-background-radius: 5px; " +
                            "-fx-cursor: hand;"
            );
            prevButton.setEffect(null);
        });
        prevButton.setOnAction(e -> visualizePreviousLine());
        HBox buttonBox = new HBox(10, prevButton, nextButton);
        buttonBox.setAlignment(Pos.CENTER);
        visualizationBox.getChildren().addAll(topBar, scrollPane, buttonBox);
        return visualizationBox;
    }

    private void toggleVisualization() {
        isVisualizationVisible = !isVisualizationVisible;

        if (isVisualizationVisible) {
            if (!mainSplitPane.getItems().contains(visualizationWorkarea)) {
                mainSplitPane.getItems().add(visualizationWorkarea);
            }
            mainSplitPane.setDividerPositions(0.02, 0.7);
            startVisualization();
        } else {
            mainSplitPane.getItems().remove(visualizationWorkarea);
            mainSplitPane.setDividerPositions(0.02);
            currentLineProperty.set(-1);
        }
    }


    private void visualizeNextLine() {
        if (currentLineIndex < codeLines.size() - 1) {
            currentLineIndex++;
            currentLineProperty.set(currentLineIndex);
            updateVisualization();
        }
    }

    private void visualizePreviousLine() {
        if (currentLineIndex > 0) {
            currentLineIndex--;
            currentLineProperty.set(currentLineIndex);
            updateVisualization();
        }
    }

    private void updateVisualization() {
        currentLineLabel.setText("Current Line: " + codeLines.get(currentLineIndex));
        codeArea.moveTo(currentLineIndex, 0);
        codeArea.requestFollowCaret();
        String currentLine = codeLines.get(currentLineIndex).trim();

        if (currentLine.matches("(int|float|double|string|char|bool|short)\\s+\\w+\\s*(=\\s*[^;]+)?;")) {

            String[] parts = currentLine.split("=|;");
            String declaration = parts[0].trim();
            String[] declParts = declaration.split("\\s+");
            String type = declParts[0];
            String varName = declParts[1];

            Object value = null;
            if (parts.length > 1) {
                value = evaluateExpression(parts[1].replace(";", "").trim(), type);
            } else {
                if (type.equals("char")) {
                    value = '\0';
                } else if (type.equals("string")) {
                    value = "null";
                } else if (type.equals("bool")) {
                    value = false;
                } else if (type.equals("short")) {
                    value = (short) 0;
                }
            }

            variables.put(varName, value);

            if (variableBoxes.containsKey(varName)) {
                Text varText = variableTexts.get(varName);
                if (value instanceof Double || value instanceof Float) {
                    varText.setText(varName + " = " + String.format("%.2f", value));
                } else {
                    varText.setText(varName + " = " + value);
                }
            } else {
                double startX = 10;
                double startY = 50 + (variables.size() - 1) * 50;

                Rectangle rect = new Rectangle(startX, startY, 120, 30);
                rect.setFill(Color.LIGHTBLUE);
                rect.setStroke(Color.BLACK);
                rect.setArcWidth(10);
                rect.setArcHeight(10);
                rect.setStroke(null);

                double requiredHeight = startY + 50;
                if (visualizationPane.getPrefHeight() < requiredHeight) {
                    visualizationPane.setPrefHeight(requiredHeight);
                }

                double requiredWidth = startX + 130;
                if (visualizationPane.getPrefWidth() < requiredWidth) {
                    visualizationPane.setPrefWidth(requiredWidth);
                }

                Text varText = new Text(startX + 10, startY + 20, varName + " = " + (value instanceof Double || value instanceof Float ? String.format("%.2f", value) : value));
                varText.setFont(Font.font("Arial", FontWeight.BOLD, 14));


                rect.setOnMouseEntered(e -> rect.setFill(Color.LIGHTGREEN));
                rect.setOnMouseExited(e -> rect.setFill(Color.LIGHTBLUE));

                visualizationPane.getChildren().addAll(rect, varText);
                variableBoxes.put(varName, rect);
                variableTexts.put(varName, varText);

                if (type.equals("string") || type.equals("int[]") || type.equals("vector")) {
                    visualizeDataStructure(varName, value, type);
                }
            }
        }

        else if (currentLine.matches("\\w+\\s*=\\s*[^;]+;")) {
            // Handle assignments
            String[] parts = currentLine.split("=");
            String varName = parts[0].trim();
            String valueStr = parts[1].replace(";", "").trim();

            String type = "int";
            if (variables.containsKey(varName)) {
                Object currentValue = variables.get(varName);
                if (currentValue instanceof Double) {
                    type = "double";
                } else if (currentValue instanceof Float) {
                    type = "float";
                } else if (currentValue instanceof String) {
                    type = "string";
                } else if (currentValue instanceof Character) {
                    type = "char";
                } else if (currentValue instanceof Boolean) {
                    type = "bool";
                } else if (currentValue instanceof Short) {
                    type = "short";
                }
            }

            Object value;
            if (type.equals("char") && valueStr.matches("'.'")) {
                value = valueStr.charAt(1);
            } else {
                value = evaluateExpression(valueStr, type);
            }
            variables.put(varName, value);

            if (variableBoxes.containsKey(varName)) {
                Text oldText = variableTexts.get(varName);
                visualizationPane.getChildren().remove(oldText);
                Text newText = new Text(oldText.getX(), oldText.getY(), varName + " = " + value);
                newText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                visualizationPane.getChildren().add(newText);
                variableTexts.put(varName, newText);
            }
        }
        else if (currentLine.matches("\\w+\\[\\d+\\]\\s*=\\s*[^;]+;")) {
            String[] parts = currentLine.split("=");
            String leftSide = parts[0].trim();
            String valueStr = parts[1].replace(";", "").trim();

            String varName = leftSide.split("\\[")[0];
            int index = Integer.parseInt(leftSide.split("\\[")[1].replace("]", ""));
            Object value = evaluateExpression(valueStr, "int");
            updateArrayElement(varName, index, value);
            updateDataStructureVisualization(varName, index, value);
        }
        else if (currentLine.startsWith("cout")) {
            String output = evaluateCoutStatement(currentLine);
            Text outputText = new Text(10, 200, "Output: " + output);
            outputText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            visualizationPane.getChildren().add(outputText);
        }
        else if (currentLine.startsWith("if") || currentLine.startsWith("else if") || currentLine.startsWith("else")) {
            handleConditionalStatement(currentLine);
        }
        else if (currentLine.startsWith("for")) {
            handleForLoop(currentLine);
        }
        else if (currentLine.startsWith("while")) {
            handleWhileLoop(currentLine);
        }
        else if (currentLine.startsWith("do")) {
            handleDoWhileLoop(currentLine);
        }
        else if (currentLine.startsWith("switch")) {
            handleSwitchCase(currentLine);
        }
        else if (currentLine.startsWith("int main")) {
            handleMainFunction(currentLine);
        }
        else if (currentLine.startsWith("#include")) {
            handleLibraryInclusion(currentLine);
        }
        else if (currentLine.startsWith("return")) {
            handleReturnStatement(currentLine);
        }
        else if (currentLine.startsWith("break")) {
            handleBreakStatement();
        }
        else if (currentLine.startsWith("continue")) {
            handleContinueStatement();
        }
        else if (currentLine.startsWith("void")) {
            handleVoidFunction(currentLine);
        }
        else if (currentLine.startsWith("class")) {
            handleClassDefinition(currentLine);
        }
        else if (currentLine.startsWith("struct")) {
            handleStructDefinition(currentLine);
        }
        else if (currentLine.startsWith("typedef")) {
            handleTypeDefinition(currentLine);
        }
        else if (currentLine.startsWith("enum")) {
            handleEnumDefinition(currentLine);
        }
        else if (currentLine.startsWith("namespace")) {
            handleNamespaceDefinition(currentLine);
        }
        else if (currentLine.startsWith("using")) {
            handleUsingDirective(currentLine);
        }
        else if (currentLine.startsWith("template")) {
            handleTemplateDefinition(currentLine);
        }
        else if (currentLine.startsWith("friend")) {
            handleFriendDeclaration(currentLine);
        }
        else if (currentLine.startsWith("extern")) {
            handleExternDeclaration(currentLine);
        }
        else if (currentLine.startsWith("static")) {
            handleStaticDeclaration(currentLine);
        }
        else if (currentLine.startsWith("const")) {
            handleConstantDeclaration(currentLine);
        }
        else if (currentLine.startsWith("volatile")) {
            handleVolatileDeclaration(currentLine);
        }
        else if (currentLine.startsWith("register")) {
            handleRegisterDeclaration(currentLine);
        }
        else if (currentLine.startsWith("auto")) {
            handleAutoDeclaration(currentLine);
        }
        else if (currentLine.startsWith("typedef")) {
            handleTypedefDeclaration(currentLine);
        }
        else if (currentLine.startsWith("asm")) {
            handleAssemblyCode(currentLine);
        }
        else if (currentLine.startsWith("goto")) {
            handleGotoStatement(currentLine);
        }
        else if (currentLine.startsWith("try")) {
            handleTryCatchBlock(currentLine);
        }
        else if (currentLine.startsWith("throw")) {
            handleThrowStatement(currentLine);
        }
        else if (currentLine.startsWith("catch")) {
            handleCatchBlock(currentLine);
        }
        else if (currentLine.startsWith("finally")) {
            handleFinallyBlock(currentLine);
        }
        else if (currentLine.startsWith("new")) {
            handleDynamicMemoryAllocation(currentLine);
        }
        else if (currentLine.startsWith("delete")) {
            handleDynamicMemoryDeallocation(currentLine);
        }
        else if (currentLine.startsWith("sizeof")) {
            handleSizeofOperator(currentLine);
        }
        else if (currentLine.startsWith("typeid")) {
            handleTypeidOperator(currentLine);
        }
        else if (currentLine.startsWith("alignas")) {
            handleAlignasSpecifier(currentLine);
        }
        else if (currentLine.startsWith("alignof")) {
            handleAlignofOperator(currentLine);
        }
        else if (currentLine.startsWith("and")) {
            handleLogicalAndOperator(currentLine);
        }
    }


    private void handleConditionalStatement(String currentLine) {
        System.out.println("Handling conditional statement: " + currentLine); // Debug statement

        if (currentLine.startsWith("if") || currentLine.startsWith("else if")) {
            String condition = currentLine.substring(currentLine.indexOf('(') + 1, currentLine.lastIndexOf(')')).trim();
            System.out.println("Evaluating condition: " + condition);

            if (evaluateCondition(condition)) {
                System.out.println("Condition is true. Visualizing block...");
                visualizeBlock();
                skipToEndOfConditional();
            } else {
                System.out.println("Condition is false. Skipping block...");
                skipToNextCondition();
            }
        } else if (currentLine.startsWith("else")) {
            System.out.println("Visualizing else block...");
            visualizeBlock();
        }
    }


    private boolean evaluateCondition(String condition) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            engine.put(entry.getKey(), entry.getValue());
        }
        try {

            Object result = engine.eval(condition);
            System.out.println("Condition: " + condition + " | Result: " + result); // Debug statement

            if (result instanceof Boolean) {
                return (Boolean) result;
            } else {
                throw new ScriptException("Condition did not evaluate to a boolean value.");
            }
        } catch (ScriptException e) {
            showError("Evaluation Error", "Failed to evaluate condition: " + condition + "\n" + e.getMessage());
            return false;
        }
    }

    private void visualizeBlock() {
        currentLineIndex++;
        int openBraces = 0;
        while (currentLineIndex < codeLines.size()) {
            String line = codeLines.get(currentLineIndex).trim();

            if (line.equals("{")) {
                openBraces++;
            } else if (line.equals("}")) {
                if (openBraces == 0) {
                    break;
                }
                openBraces--;
            }

            updateVisualization();
            currentLineIndex++;
        }
    }
    private void skipToEndOfConditional() {
        int openBraces = 0;
        while (currentLineIndex < codeLines.size()) {
            String line = codeLines.get(currentLineIndex).trim();
            if (line.equals("}") && openBraces == 0) {
                break;
            }
            if (line.contains("{")) {
                openBraces++;
            }
            if (line.contains("}")) {
                openBraces--;
            }
            currentLineIndex++;
        }
    }
    private void skipToNextCondition() {
        int openBraces = 0;
        while (currentLineIndex < codeLines.size()) {
            String line = codeLines.get(currentLineIndex).trim();
            if (line.startsWith("else if") || line.startsWith("else") || (line.equals("}") && openBraces == 0)) {
                break;
            }
            if (line.contains("{")) {
                openBraces++;
            }
            if (line.contains("}")) {
                openBraces--;
            }
            currentLineIndex++;
        }
    }
    private void handleForLoop(String currentLine) {
        String forContent = currentLine.substring(currentLine.indexOf('(') + 1, currentLine.lastIndexOf(')')).trim();
        String[] parts = forContent.split(";");
        String initialization = parts[0].trim();
        String condition = parts[1].trim();
        String update = parts[2].trim();
        evaluateExpression(initialization, "int");

        Rectangle loopBox = new Rectangle(10, 50 + (variables.size() - 1) * 40, 300, 200);
        loopBox.setFill(Color.LIGHTGRAY);
        loopBox.setStroke(Color.BLACK);
        visualizationPane.getChildren().add(loopBox);
        while (evaluateCondition(condition)) {
            updateVisualization();
            evaluateExpression(update, "int");
        }
    }

    private void handleWhileLoop(String currentLine) {
        // Implementation for handling while loops
    }

    private void handleDoWhileLoop(String currentLine) {
        // Implementation for handling do-while loops
    }

    private void handleSwitchCase(String currentLine) {
        // Implementation for handling switch-case statements
    }

    private void handleMainFunction(String currentLine) {
        // Implementation for handling main function
    }

    private void handleLibraryInclusion(String currentLine) {
        // Implementation for handling #include statements
    }

    private void handleReturnStatement(String currentLine) {
        // Implementation for handling return statements
    }

    private void handleBreakStatement() {
        // Implementation for handling break statements
    }

    private void handleContinueStatement() {
        // Implementation for handling continue statements
    }

    private void handleVoidFunction(String currentLine) {
        // Implementation for handling void functions
    }

    private void handleClassDefinition(String currentLine) {
        // Implementation for handling class definitions
    }

    private void handleStructDefinition(String currentLine) {
        // Implementation for handling struct definitions
    }

    private void handleTypeDefinition(String currentLine) {
        // Implementation for handling type definitions
    }

    private void handleEnumDefinition(String currentLine) {
        // Implementation for handling enum definitions
    }

    private void handleSizeofOperator(String currentLine) {
        // Implementation for handling sizeof operator
    }

    private void handleNamespaceDefinition(String currentLine) {
        // Implementation for handling namespace definitions
    }

    private void handleUsingDirective(String currentLine) {
        // Implementation for handling using directives
    }

    private void handleTemplateDefinition(String currentLine) {
        // Implementation for handling template definitions
    }

    private void handleFriendDeclaration(String currentLine) {
        // Implementation for handling friend declarations
    }

    private void handleExternDeclaration(String currentLine) {
        // Implementation for handling extern declarations
    }

    private void handleStaticDeclaration(String currentLine) {
        // Implementation for handling static declarations
    }

    private void handleConstantDeclaration(String currentLine) {
        // Implementation for handling constant declarations
    }

    private void handleVolatileDeclaration(String currentLine) {
        // Implementation for handling volatile declarations
    }

    private void handleRegisterDeclaration(String currentLine) {
        // Implementation for handling register declarations
    }

    private void handleAutoDeclaration(String currentLine) {
        // Implementation for handling auto declarations
    }

    private void handleTypedefDeclaration(String currentLine) {
        // Implementation for handling typedef declarations
    }

    private void handleAssemblyCode(String currentLine) {
        // Implementation for handling assembly code
    }

    private void handleGotoStatement(String currentLine) {
        // Implementation for handling goto statements
    }

    private void handleTryCatchBlock(String currentLine) {
        // Implementation for handling try-catch blocks
    }

    private void handleThrowStatement(String currentLine) {
        // Implementation for handling throw statements
    }

    private void handleCatchBlock(String currentLine) {
        // Implementation for handling catch blocks
    }

    private void handleFinallyBlock(String currentLine) {
        // Implementation for handling finally blocks
    }

    private void handleDynamicMemoryAllocation(String currentLine) {
        // Implementation for handling dynamic memory allocation
    }

    private void handleDynamicMemoryDeallocation(String currentLine) {
        // Implementation for handling dynamic memory deallocation
    }

    private void handleTypeidOperator(String currentLine) {
        // Implementation for handling typeid operator
    }

    private void handleAlignasSpecifier(String currentLine) {
        // Implementation for handling alignas specifier
    }

    private void handleAlignofOperator(String currentLine) {
        // Implementation for handling alignof operator
    }

    private void handleLogicalAndOperator(String currentLine) {
        // Implementation for handling logical AND operator
    }


    private void visualizeDataStructure(String varName, Object value, String type) {

        visualizationPane.getChildren().removeIf(node -> node instanceof Rectangle && node.getUserData() != null && node.getUserData().equals(varName));
        visualizationPane.getChildren().removeIf(node -> node instanceof Text && node.getUserData() != null && node.getUserData().equals(varName));

        double startX = 150;
        double startY = 50 + (variables.size() - 1) * 40;

        if (type.equals("string")) {
            String strValue = (String) value;
            for (int i = 0; i < strValue.length(); i++) {
                Rectangle rect = new Rectangle(startX + i * 40, startY, 30, 30);
                rect.setFill(Color.LIGHTGREEN);
                rect.setStroke(Color.BLACK);
                rect.setArcWidth(5);
                rect.setArcHeight(5);
                rect.setUserData(varName);

                Text charText = new Text(startX + i * 40 + 10, startY + 20, String.valueOf(strValue.charAt(i)));
                charText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                charText.setUserData(varName);

                Text indexText = new Text(startX + i * 40 + 10, startY + 50, String.valueOf(i));
                indexText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                indexText.setUserData(varName);
                visualizationPane.getChildren().addAll(rect, charText, indexText);
            }
        }

        else if (type.equals("int[]")) {
            int[] arrayValue = (int[]) value;
            for (int i = 0; i < arrayValue.length; i++) {
                Rectangle rect = new Rectangle(startX + i * 40, startY, 30, 30);
                rect.setFill(Color.LIGHTGREEN);
                rect.setStroke(Color.BLACK);
                rect.setUserData(varName);

                Text valueText = new Text(startX + i * 40 + 10, startY + 20, String.valueOf(arrayValue[i]));
                valueText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                valueText.setUserData(varName);

                Text indexText = new Text(startX + i * 40 + 10, startY + 50, String.valueOf(i));
                indexText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                indexText.setUserData(varName);
                visualizationPane.getChildren().addAll(rect, valueText, indexText);
            }
        } else if (type.equals("vector")) {
            List<?> vectorValue = (List<?>) value;
            for (int i = 0; i < vectorValue.size(); i++) {
                Rectangle rect = new Rectangle(startX + i * 40, startY, 30, 30);
                rect.setFill(Color.LIGHTGREEN);
                rect.setStroke(Color.BLACK);
                rect.setUserData(varName);

                Text valueText = new Text(startX + i * 40 + 10, startY + 20, String.valueOf(vectorValue.get(i)));
                valueText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                valueText.setUserData(varName);

                Text indexText = new Text(startX + i * 40 + 10, startY + 50, String.valueOf(i));
                indexText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                indexText.setUserData(varName);

                visualizationPane.getChildren().addAll(rect, valueText, indexText);
            }
        }
    }

    private void updateDataStructureVisualization(String varName, int index, Object value) {
        if (variables.containsKey(varName)) {
            Object dataStructure = variables.get(varName);
            if (dataStructure instanceof String) {
                String strValue = (String) dataStructure;
                if (index >= 0 && index < strValue.length()) {
                    strValue = strValue.substring(0, index) + value + strValue.substring(index + 1);
                    variables.put(varName, strValue);
                    visualizeDataStructure(varName, strValue, "string");
                }
            } else if (dataStructure instanceof int[]) {
                int[] arrayValue = (int[]) dataStructure;
                if (index >= 0 && index < arrayValue.length) {
                    arrayValue[index] = (int) value;
                    visualizeDataStructure(varName, arrayValue, "int[]");
                }
            } else if (dataStructure instanceof List) {
                List<?> vectorValue = (List<?>) dataStructure;
                if (index >= 0 && index < vectorValue.size()) {
                    ((List<Object>) vectorValue).set(index, value);
                    visualizeDataStructure(varName, vectorValue, "vector");
                }
            }
        }
    }

    private Object evaluateExpression(String expression, String type) {
        expression = expression.trim();

        if (type.equals("char") && expression.matches("'.'")) {
            return expression.charAt(1);
        }
        if (type.equals("string") && expression.startsWith("\"") && expression.endsWith("\"")) {
            return expression.substring(1, expression.length() - 1);
        }
        if (type.equals("bool")) {
            return Boolean.parseBoolean(expression);
        }
        if (type.equals("short")) {
            return Short.parseShort(expression);
        }

        if (variables.containsKey(expression)) {
            return variables.get(expression);
        }

        try {
            switch (type) {
                case "int":
                    return Integer.parseInt(expression);
                case "double":
                    return Double.parseDouble(expression);
                case "float":
                    return Float.parseFloat(expression);
                default:
                    return evaluateArithmeticExpression(expression);
            }
        } catch (NumberFormatException ignored) {}

        return evaluateArithmeticExpression(expression);
    }
    private Object evaluateArithmeticExpression(String expression) {
        List<String> postfix = infixToPostfix(expression);
        Stack<Number> stack = new Stack<>();

        for (String token : postfix) {
            if (token.matches("\\d+")) {
                stack.push(Integer.parseInt(token));
            } else if (token.matches("\\d+\\.\\d+")) {
                stack.push(Double.parseDouble(token));
            } else if (variables.containsKey(token)) {
                stack.push((Number) variables.get(token));
            }
            else if (token.matches("\\+\\+\\w+|\\w+\\+\\+|--\\w+|\\w+--")) {
                String varName = token.replaceAll("\\+\\+|--", "");
                if (variables.containsKey(varName)) {
                    int value = (int) variables.get(varName);
                    if (token.startsWith("++") || token.endsWith("++")) {
                        value++;
                    } else if (token.startsWith("--") || token.endsWith("--")) {
                        value--;
                    }
                    variables.put(varName, value);
                    stack.push(value);
                }
            }
            else {
                Number b = stack.pop();
                Number a = stack.pop();

                if (a instanceof Integer && b instanceof Integer) {
                    switch (token) {
                        case "+": stack.push(a.intValue() + b.intValue()); break;
                        case "-": stack.push(a.intValue() - b.intValue()); break;
                        case "*": stack.push(a.intValue() * b.intValue()); break;
                        case "/": stack.push(a.intValue() / b.intValue()); break;
                        case "%": stack.push(a.intValue() % b.intValue()); break;

                    }
                } else {
                    double aVal = a.doubleValue();
                    double bVal = b.doubleValue();
                    double result=0;
                    switch (token) {
                        case "+": result=(aVal + bVal); break;
                        case "-": result=(aVal - bVal); break;
                        case "*": result=(aVal * bVal); break;
                        case "/": result=(aVal / bVal); break;
                        case "%": result=(aVal % bVal); break;
                    }
                    stack.push(result);
                }
            }
        }
        return stack.pop();
    }


    private List<String> infixToPostfix(String expression) {
        List<String> output = new ArrayList<>();
        Stack<String> operators = new Stack<>();
        Map<String, Integer> precedence = Map.of("+", 1, "-", 1, "*", 2, "/", 2);

        StringTokenizer tokenizer = new StringTokenizer(expression, "+-*/()", true);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (token.isEmpty()) continue;

            if (token.matches("\\d+(\\.\\d+)?") || variables.containsKey(token)) {
                output.add(token);
            } else if (token.equals("(")) {
                operators.push(token);
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }
                operators.pop();
            } else {
                while (!operators.isEmpty() && precedence.getOrDefault(operators.peek(), 0) >= precedence.get(token)) {
                    output.add(operators.pop());
                }
                operators.push(token);
            }
        }
        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }
        return output;
    }



    private String evaluateCoutStatement(String coutLine) {
        String expression = coutLine.replace("cout", "").replace("<<", "").replace(";", "").trim();
        return String.valueOf(evaluateExpression(expression, "int"));
    }

    private void initializeCodeLines() {
        String code = codeArea.getText();
        codeLines = Arrays.asList(code.split("\n"));
        currentLineIndex = 0;
        variables.clear();
        variableBoxes.clear();
        variableTexts.clear();
        visualizationPane.getChildren().clear();
        updateVisualization();
    }
    private void visualizeArray(String varName, Object value) {
        if (value instanceof String) {
            String strValue = (String) value;
            int length = strValue.length();
            for (int i = 0; i < length; i++) {
                Rectangle rect = new Rectangle(10 + i * 40, 50 + (variables.size() - 1) * 40, 30, 30);
                rect.setFill(Color.LIGHTGREEN);
                rect.setStroke(Color.BLACK);

                Text indexText = new Text(15 + i * 40, 70 + (variables.size() - 1) * 40, String.valueOf(i));
                indexText.setFont(Font.font("Arial", FontWeight.BOLD, 12));

                Text charText = new Text(20 + i * 40, 90 + (variables.size() - 1) * 40, String.valueOf(strValue.charAt(i)));
                charText.setFont(Font.font("Arial", FontWeight.BOLD, 14));

                visualizationPane.getChildren().addAll(rect, indexText, charText);
            }
        } else if (value instanceof int[]) {
            int[] arrayValue = (int[]) value;
            for (int i = 0; i < arrayValue.length; i++) {
                Rectangle rect = new Rectangle(10 + i * 40, 50 + (variables.size() - 1) * 40, 30, 30);
                rect.setFill(Color.LIGHTGREEN);
                rect.setStroke(Color.BLACK);

                Text indexText = new Text(15 + i * 40, 70 + (variables.size() - 1) * 40, String.valueOf(i));
                indexText.setFont(Font.font("Arial", FontWeight.BOLD, 12));

                Text valueText = new Text(20 + i * 40, 90 + (variables.size() - 1) * 40, String.valueOf(arrayValue[i]));
                valueText.setFont(Font.font("Arial", FontWeight.BOLD, 14));

                visualizationPane.getChildren().addAll(rect, indexText, valueText);
            }
        } else if (value instanceof List) {
            List<?> listValue = (List<?>) value;
            for (int i = 0; i < listValue.size(); i++) {
                Rectangle rect = new Rectangle(10 + i * 40, 50 + (variables.size() - 1) * 40, 30, 30);
                rect.setFill(Color.LIGHTGREEN);
                rect.setStroke(Color.BLACK);

                Text indexText = new Text(15 + i * 40, 70 + (variables.size() - 1) * 40, String.valueOf(i));
                indexText.setFont(Font.font("Arial", FontWeight.BOLD, 12));

                Text valueText = new Text(20 + i * 40, 90 + (variables.size() - 1) * 40, String.valueOf(listValue.get(i)));
                valueText.setFont(Font.font("Arial", FontWeight.BOLD, 14));

                visualizationPane.getChildren().addAll(rect, indexText, valueText);
            }
        }
    }
    private void updateArrayElement(String varName, int index, Object value) {
        if (variables.containsKey(varName)) {
            Object array = variables.get(varName);
            if (array instanceof String) {
                String strValue = (String) array;
                if (index >= 0 && index < strValue.length()) {
                    strValue = strValue.substring(0, index) + value + strValue.substring(index + 1);
                    variables.put(varName, strValue);
                }
            } else if (array instanceof int[]) {
                int[] arrayValue = (int[]) array;
                if (index >= 0 && index < arrayValue.length) {
                    arrayValue[index] = (int) value;
                }
            } else if (array instanceof List) {
                List<?> listValue = (List<?>) array;
                if (index >= 0 && index < listValue.size()) {
                    ((List<Object>) listValue).set(index, value);
                }
            }
            visualizeArray(varName, variables.get(varName));
        }
    }

    private void startVisualization() {
        initializeCodeLines();
        currentLineIndex = 0;
        currentLineProperty.set(currentLineIndex);
        updateVisualization();
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();
        menuBar.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        Menu fileMenu = new Menu("File");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem newFile = new MenuItem("New");
        MenuItem closeFile = new MenuItem("Close");
        fileMenu.getItems().addAll(newFile, openFile, saveFile, closeFile);

        Menu runMenu = new Menu("Run");
        MenuItem runCode = new MenuItem("Run");
        runMenu.getItems().addAll(runCode);

        Menu debugMenu = new Menu("Debug");
        MenuItem debugCode = new MenuItem("Debug");
        debugMenu.getItems().add(debugCode);

        Menu toolsMenu = new Menu("Tools");
        MenuItem visualizeCode = new MenuItem("Visualize");
        MenuItem formatCode = new MenuItem("Code Template");
        MenuItem analyzeCode = new MenuItem("Analyze Code");
        MenuItem timeComplexity = new MenuItem("Time Complexity");
        MenuItem spaceComplexity = new MenuItem("Space Complexity");
        MenuItem deleteCode = new MenuItem("Clear Code");
        timeComplexity.setOnAction(e -> calculateTimeComplexity());
        spaceComplexity.setOnAction(e -> calculateSpaceComplexity());
        visualizeCode.setOnAction(e -> startVisualization());

        toolsMenu.getItems().addAll(visualizeCode,formatCode, analyzeCode,timeComplexity,spaceComplexity, deleteCode);


        Menu settingMenu = new Menu("Setting");
        Menu themeMenu = new Menu("Theme");
        MenuItem lightMode = new MenuItem("Light Mode");
        MenuItem darkMode = new MenuItem("Dark Mode");
        themeMenu.getItems().addAll(lightMode, darkMode);
        settingMenu.getItems().addAll(themeMenu);

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
        visualizeCode.setOnAction(e -> toggleVisualization());
        timeComplexity.setOnAction(e -> calculateTimeComplexity());
        spaceComplexity.setOnAction(e -> calculateSpaceComplexity());

        return menuBar;
    }

    private CodeArea createCodeArea() {
        CodeArea codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(paraIdx -> {
            HBox hbox = new HBox();
            hbox.setSpacing(5);
            hbox.setStyle("-fx-background-color: #cdf7e9; -fx-padding: 2px;"); // Gray background

            Label arrow = new Label("➤"); // Thicker arrow
            arrow.setStyle("-fx-text-fill: red; -fx-font-size: 18px; -fx-font-weight: bold;");
            arrow.visibleProperty().bind(currentLineProperty.isEqualTo(paraIdx));

            Label lineNumber = new Label(String.valueOf(paraIdx + 1));
            lineNumber.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");

            hbox.getChildren().addAll(arrow, lineNumber);
            return hbox;
        });

        codeArea.textProperty().addListener((obs, oldText, newText) -> applySyntaxHighlighting(newText));
        final IntegerProperty fontSize = new SimpleIntegerProperty(14);

        codeArea.styleProperty().bind(Bindings.concat(
                "-fx-font-family: 'Courier New', 'Courier', 'monospace'; ",
                "-fx-font-size: ", fontSize.asString(), "px; ",
                "-fx-text-fill: #000000;"
        ));

        codeArea.setOnScroll(event -> {
            if (event.isControlDown()) {
                if (event.getDeltaY() > 0) { // Scroll Up -> Zoom In
                    fontSize.set(Math.min(fontSize.get() + 2, 30));
                } else { // Scroll Down -> Zoom Out
                    fontSize.set(Math.max(fontSize.get() - 2, 10));
                }
            }
        });

        codeArea.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case EQUALS:
                    case PLUS:
                        fontSize.set(Math.min(fontSize.get() + 2, 30));
                        break;
                    case MINUS:
                        fontSize.set(Math.max(fontSize.get() - 2, 10));
                        break;
                    default:
                        return;
                }
            }
        });

        codeArea.getStyleClass().add("light-mode");
        codeArea.getStylesheets().add(getClass().getResource("Main.css").toExternalForm());

        codeArea.setOnKeyTyped(event -> {
            String typedChar = event.getCharacter();
            if (typedChar.isEmpty()) return;

            switch (typedChar.charAt(0)) {
                case '{':
                    handleAutoFormatCurlyBrace(codeArea);
                    break;
                case '(':
                    handleAutoFormatParenthesis(codeArea);
                    break;
                case '[':
                    handleAutoFormatSquareBracket(codeArea);
                    break;
            }
        });

        return codeArea;
    }


    private void showComplexityPane(String complexityType, String complexityValue) {
        Stage complexityStage = new Stage();
        complexityStage.initStyle(StageStyle.TRANSPARENT);

        Label complexityLabel = new Label(complexityValue);
        complexityLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 26));
        complexityLabel.setTextFill(Color.WHITE);
        complexityLabel.setPadding(new Insets(15));

        DropShadow glow = new DropShadow();
        glow.setColor(Color.WHITE);
        glow.setRadius(3);
        glow.setSpread(0.2);
        glow.setBlurType(BlurType.GAUSSIAN);
        complexityLabel.setEffect(glow);

        String fullComplexityType = complexityType.equalsIgnoreCase("Time") ? "Time Complexity" :
                complexityType.equalsIgnoreCase("Space") ? "Space Complexity" : complexityType;

        Label headerLabel = new Label(fullComplexityType);
        headerLabel.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 20));
        headerLabel.setTextFill(Color.BLACK);
        headerLabel.setAlignment(Pos.TOP_CENTER);
        headerLabel.setPadding(new Insets(10));
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setStyle("-fx-background-color: yellow;");

        VBox layout = new VBox(headerLabel, complexityLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #000000; -fx-border-color: #444; -fx-border-width: 2px; -fx-border-radius: 8px;");
        layout.setPadding(new Insets(30));

        double cardWidth = 420;
        double cardHeight = 260;

        Scene scene = new Scene(layout, cardWidth, cardHeight);
        scene.setFill(Color.TRANSPARENT);
        complexityStage.setScene(scene);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        complexityStage.setX((screenBounds.getWidth() - cardWidth) / 2);
        complexityStage.setY((screenBounds.getHeight() - cardHeight) / 2);

        scene.setOnMouseClicked(event -> complexityStage.close());

        complexityStage.show();
    }

    private void calculateTimeComplexity() {
        String code = codeArea.getText();
        String timeComplexity = analyzeTimeComplexity(code);
        showComplexityPane("Time", timeComplexity);
    }

    private String analyzeTimeComplexity(String code) {
        String[] lines = code.split("\n");
        int maxNestedLoops = 0;
        int currentDepth = 0;
        boolean hasRecursion = false;
        boolean hasLogarithmicLoop = false;
        boolean hasLinearithmic = false;
        boolean hasDifferentRanges = false;
        List<String> complexities = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("for") || line.startsWith("while")) {
                currentDepth++;
                maxNestedLoops = Math.max(maxNestedLoops, currentDepth);

                if (line.contains("*=") || line.contains("/=")) {
                    hasLogarithmicLoop = true;
                }

                if (line.contains("i<n") && line.contains("j<m")) {
                    hasDifferentRanges = true;
                }
            }

            if (line.equals("}")) {
                if (currentDepth > 0) {
                    currentDepth--;
                }
            }

            if (line.contains("function_name(")) {
                hasRecursion = true;
            }

            if (line.contains("sort(")) {
                complexities.add("O(n log n)");
            } else if (line.contains("binary_search(")) {
                complexities.add("O(log n)");
            } else if (line.contains("find(") || line.contains("count(")) {
                complexities.add("O(n)");
            }
        }

        if (code.contains("mergeSort") || code.contains("quickSort")||code.contains("MergeSort") || code.contains("QuickSort")) {
            complexities.add("O(n log n)");
        } else if (code.contains("bubbleSort") || code.contains("selectionSort") || code.contains("insertionSort")) {
            complexities.add("O(n²)");
        } else if (code.contains("binarySearch")||code.contains("BinarySearch")||code.contains("binary_search")) {
            complexities.add("O(log n)");
        } else if (code.contains("fibonacci")) {
            complexities.add("O(2ⁿ)");
        } else if (code.contains("dijkstra")||code.contains("Dijkstra")) {
            complexities.add("O(V²) or O(E + V log V)");
        } else if (code.contains("bellmanFord")||code.contains("BellmanFord")) {
            complexities.add("O(VE)");
        } else if (code.contains("prims")||code.contains("Prims")) {
            complexities.add("O(V²) or O(E + V log V)");
        } else if (code.contains("kruskal")||code.contains("Kruskal")) {
            complexities.add("O(E log E)");
        } else if (code.contains("bfs")||code.contains("BFS")||code.contains("breadthFirstSearch")) {
            complexities.add("O(V + E)");
        } else if (code.contains("dfs")||code.contains("DFS")||code.contains("depthFirstSearch")) {
            complexities.add("O(V + E)");
        }
        else if (maxNestedLoops == 0 && !hasRecursion) {
            complexities.add("O(1)");
        } else if (maxNestedLoops == 1 && !hasRecursion) {
            if (hasLogarithmicLoop) {
                complexities.add("O(log n)");
            } else {
                complexities.add("O(n)");
            }
        } else if (maxNestedLoops == 2 && !hasRecursion) {
            if (hasDifferentRanges) {
                complexities.add("O(n * m)");
            } else {
                complexities.add("O(n²)");
            }
        } else if (hasLinearithmic) {
            complexities.add("O(n log n)");
        } else if (hasRecursion) {
            complexities.add("O(2ⁿ)");
        }  else {
            complexities.add("O(n^" + maxNestedLoops + ")");
        }

        return complexities.stream().max(String::compareTo).orElse("O(1)");
    }

    private void calculateSpaceComplexity() {
        String code = codeArea.getText();
        String spaceComplexity = analyzeSpaceComplexity(code);
        showComplexityPane("Space", spaceComplexity);
    }

    private String analyzeSpaceComplexity(String code) {
        String[] lines = code.split("\n");

        int variableCount = 0;
        int arraySize = 0;
        int matrixSize = 0;
        int recursiveDepth = 0;
        boolean hasDynamicAllocation = false;
        boolean hasQueue = false;
        boolean hasStack = false;
        boolean hasLinkedList = false;
        boolean hasVector = false;
        boolean has2DVector = false;

        for (String line : lines) {
            line = line.trim();

            if (line.matches("(vector|int|double|float|char|string|bool|long|short|auto)\\s+\\w+\\s*(=\\s*[^;]+)?;")) {
                variableCount++;
            }

            if (line.matches("(vector|int|double|float|char|string|bool|long|short|auto)\\s+\\w+\\s*\\[\\s*\\w+\\s*\\]\\s*(=\\s*\\{[^;]*\\})?;")) {
                String sizeStr = line.replaceAll(".*\\[\\s*(\\w+)\\s*\\].*", "$1");
                if (sizeStr.matches("\\d+")) {
                    arraySize += Integer.parseInt(sizeStr);
                } else {
                    arraySize += 1;
                }
            }

            if (line.matches("(vector|int|double|float|char|string|bool|long|short|auto)\\s+\\w+\\s*\\[\\s*\\w+\\s*\\]\\s*\\[\\s*\\w+\\s*\\]\\s*(=\\s*\\{[^;]*\\})?;")) {
                String[] sizes = line.replaceAll(".*\\[\\s*(\\w+)\\s*\\]\\s*\\[\\s*(\\w+)\\s*\\].*", "$1 $2").split(" ");
                if (sizes[0].matches("\\d+") && sizes[1].matches("\\d+")) {
                    matrixSize += Integer.parseInt(sizes[0]) * Integer.parseInt(sizes[1]);
                } else {
                    matrixSize += 1; // Assume dynamic size
                }
            }

            if (line.matches("vector\\s*<\\s*vector\\s*<.*>\\s*>\\s*\\w+\\s*(=\\s*\\{[^;]*\\})?;")) {
                has2DVector = true;
            }

            if (line.matches("vector\\s*<\\s*\\w+\\s*>\\s*\\w+\\s*\\(\\s*\\w+\\s*\\);")) {
                String sizeStr = line.replaceAll(".*\\(\\s*(\\w+)\\s*\\).*", "$1");
                if (sizeStr.matches("\\d+")) {
                    arraySize += Integer.parseInt(sizeStr);
                } else {
                    arraySize += 1;
                }
            }

            if (line.matches("vector\\s*<\\s*\\w+\\s*>\\s*\\w+\\s*\\(\\s*\\w+\\s*\\+\\s*\\d+\\s*\\);")) {
                String sizeStr = line.replaceAll(".*\\(\\s*(\\w+)\\s*\\+\\s*(\\d+)\\s*\\).*", "$1 $2");
                String[] sizes = sizeStr.split(" ");
                if (sizes[0].matches("\\d+") && sizes[1].matches("\\d+")) {
                    arraySize += Integer.parseInt(sizes[0]) + Integer.parseInt(sizes[1]);
                } else {
                    arraySize += 1;
                }
            }

            if (line.matches("vector\\s*<\\s*\\w+\\s*>\\s*\\w+\\s*\\(\\s*\\w+\\s*\\+\\s*\\d+\\s*\\)\\s*=\\s*[^;]+;")) {
                String sizeStr = line.replaceAll(".*\\(\\s*(\\w+)\\s*\\+\\s*(\\d+)\\s*\\).*", "$1 $2");
                String[] sizes = sizeStr.split(" ");
                if (sizes[0].matches("\\d+") && sizes[1].matches("\\d+")) {
                    arraySize += Integer.parseInt(sizes[0]) + Integer.parseInt(sizes[1]);
                } else {
                    arraySize += 1;
                }
            }

            if (line.matches("vector\\s*<.*>\\s*\\w+\\s*(=\\s*\\{[^;]*\\})?;")) {
                hasVector = true;
            }

            if (line.matches("queue\\s*<.*>\\s*\\w+\\s*(=\\s*\\{[^;]*\\})?;")||line.matches("priority_queue\\s*<.*>\\s*\\w+\\s*(=\\s*\\{[^;]*\\})?;")) {
                hasQueue = true;
            }
            if (line.matches("stack\\s*<.*>\\s*\\w+\\s*(=\\s*\\{[^;]*\\})?;")) {
                hasStack = true;
            }
            if (line.matches("list\\s*<.*>\\s*\\w+\\s*(=\\s*\\{[^;]*\\})?;")) {
                hasLinkedList = true;
            }

            if (line.contains("function_name(")) {
                recursiveDepth++;
            }

            if (line.contains("new") || line.contains("malloc") || line.contains("calloc")) {
                hasDynamicAllocation = true;
            }
        }

        if (variableCount == 0 && arraySize == 0 && matrixSize == 0 && recursiveDepth == 0 && !hasDynamicAllocation && !hasVector && !has2DVector) {
            return "O(1)";
        } else if (matrixSize > 0 || has2DVector) {
            return "O(n²)";
        } else if (arraySize > 0 || hasVector) {
            return "O(n)";
        } else if (hasVector || hasQueue || hasStack || hasLinkedList) {
            return "O(n)";
        } else if (recursiveDepth > 0) {
            return "O(n)";
        } else if (hasDynamicAllocation) {
            return "O(n)";
        } else {
            return "O(1)";
        }
    }

    private void handleAutoFormatSquareBracket(CodeArea codeArea) {
        int caretPosition = codeArea.getCaretPosition();
        codeArea.insertText(caretPosition, "]");
        codeArea.moveTo(caretPosition);
    }
    private void handleAutoFormatParenthesis(CodeArea codeArea) {
        int caretPosition = codeArea.getCaretPosition();
        codeArea.insertText(caretPosition, ")");
        codeArea.moveTo(caretPosition);
    }

    private void handleAutoFormatCurlyBrace(CodeArea codeArea) {
        int caretPosition = codeArea.getCaretPosition();
        codeArea.insertText(caretPosition, "\n\n}");
        codeArea.moveTo(caretPosition + 1);
    }
    private SplitPane createSplitPane() {
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

        inputArea = new TextArea();
        inputArea.setPromptText("Enter input here...");
        inputArea.setStyle(
                "-fx-control-inner-background: #ced9d7; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: #89CFF0; " +
                        "-fx-border-width: 2px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-padding: 3px; " +
                        "-fx-prompt-text-fill: rgba(44, 62, 80, 0.5);"
        );

        inputArea.setOnMouseEntered(e -> inputArea.setStyle(
                "-fx-control-inner-background: #ced9d7; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-width: 3px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-padding: 3px; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 255, 0.3), 8, 0, 0, 3);" +
                        "-fx-prompt-text-fill: rgba(44, 62, 80, 0.6);"
        ));

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

        codeArea = createCodeArea();
        VirtualizedScrollPane<CodeArea> scrollableCodeArea = new VirtualizedScrollPane<>(codeArea);
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setStyle("-fx-divider-color: transparent;");
        splitPane.setOnMouseEntered(e -> splitPane.setStyle("-fx-divider-color: rgba(30, 144, 255, 0.5);"));
        splitPane.setOnMousePressed(e -> splitPane.setStyle("-fx-divider-color: rgba(0, 102, 204, 1);"));
        splitPane.setOnMouseReleased(e -> {
            new Timeline(new KeyFrame(Duration.millis(300), evt ->
                    splitPane.setStyle("-fx-divider-color: transparent;"))
            ).play();
        });
        splitPane.getItems().addAll(scrollableCodeArea, inputArea, outputArea);
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
            private final Label closeButton = new Label("✕");
            private final Region spacer = new Region();

            {
                closeButton.setStyle(
                        "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 12px; " +
                                "-fx-cursor: hand; " +
                                "-fx-padding: 0 0 0 5px;"
                );

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

                HBox.setHgrow(spacer, Priority.ALWAYS);
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
                                    "-fx-font-size: 14px; " +
                                    "-fx-font-family: 'Segoe UI', sans-serif; " +
                                    "-fx-text-fill: #2c3e50; " +
                                    "-fx-background-color: transparent; " +
                                    "-fx-transition: all 0.3s ease-in-out;"
                    );

                    setOnMouseEntered(e -> {
                        if (isSelected()) {
                            setStyle(
                                    "-fx-background-color: rgba(173, 216, 230, 0.5); " +
                                            "-fx-text-fill: white; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-font-size: 16px; " +
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 15, 0, 0, 5);"
                            );
                        } else {
                            setStyle(
                                    "-fx-background-color: rgba(173, 216, 230, 0.3); " +
                                            "-fx-text-fill: white; " +
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: dropshadow(three-pass-box, rgba(173, 216, 230, 0.7), 12, 0, 0, 5);"
                            );
                        }
                    });

                    setOnMouseExited(e -> {
                        if (isSelected()) {
                            setStyle(
                                    "-fx-background-color: rgba(0, 102, 204, 0.4); " +
                                            "-fx-text-fill: white; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-font-size: 16px; " +
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 15, 0, 0, 5);"
                            );
                        } else {
                            setStyle(
                                    "-fx-background-color: transparent; " +
                                            "-fx-text-fill: #2c3e50; " +
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: none;"
                            );
                        }
                    });

                    selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                        if (isSelected) {
                            setStyle(
                                    "-fx-background-color: rgba(0, 102, 204, 0.4); " +
                                            "-fx-text-fill: white; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-font-size: 16px; " +
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 15, 0, 0, 5);"
                            );
                        } else {
                            setStyle(
                                    "-fx-background-color: transparent; " +
                                            "-fx-text-fill: #2c3e50; " +
                                            "-fx-font-size: 14px; " +
                                            "-fx-padding: 10px 14px; " +
                                            "-fx-effect: none;"
                            );
                        }
                    });
                }
            }
        });

        fileListView.setStyle(
                "-fx-background-color: rgba(173, 216, 230, 0.2); " +
                        "-fx-border-color: transparent; " +
                        "-fx-padding: 0; " +
                        "-fx-effect: none;"
        );

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
                        debugInput.write("break main\n");
                        debugInput.write("run\n");
                        debugInput.write(userInput + "\n");
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
                + "-Tools (Visulization, Code Template, Anylize Code, Time & Space Complexity, Clear Code)\n"
                + "- File operations (Open, Save, New)\n\n"
                + "Developed by: Taz , Jamil , Rihin");
        detailsAlert.showAndWait();
    }

    private void codeAnalyze() {
        String code = codeArea.getText();
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
            String variable = variableMatcher.group(2);
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
            String functionName = functionMatcher.group(2);
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
                        "6. Time Complexity: Analyze the time complexity of your code.\n" +
                        "7. Space Complexity: Analyze the space complexity of your code.\n" +
                        "8. Visualize Data: Visualize your data structures and algorithms.\n" +
                        "9. Format Code: Automatically format your code for readability.\n" +
                        "10. Documentation: View app features and functionality.\n\n" +
                        "Developed by: Taz, Jamil, Rihin\n" +
                        "Version: 1.0\n" +
                        "License: MIST"
        );
        docTextArea.setEditable(false);
        docTextArea.setWrapText(true);
        docTextArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 18px;");

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