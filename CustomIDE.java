import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.geometry.Orientation;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CustomIDE extends Application {

    private CodeArea codeArea;
    private TextArea outputArea;
    private TextArea inputArea;
    private ListView<String> fileListView;
    private String currentFileName = "Untitled1";
    private int currentLineIndex = 0; // Track the current line being visualized
    private List<String> codeLines = new ArrayList<>(); // Store individual lines of code
    private Pane visualizationPane; // Pane for visualization
    private Label currentLineLabel; // Label to display the current line
    private Map<String, Object> variables = new HashMap<>(); // Track variables and their values
    private Map<String, Rectangle> variableBoxes = new HashMap<>(); // Track variable boxes
    private Map<String, Text> variableTexts = new HashMap<>(); // Track variable text labels
    private Map<String, String> fileContents = new HashMap<>();
    private Map<String, File> fileMap = new HashMap<>();
    //private String currentFileName = null;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // MenuBar
        MenuBar menuBar = createMenuBar(primaryStage);
        codeArea = createCodeArea();
        SplitPane splitPane = createSplitPane();

        // File ListView
        fileListView = new ListView<>();
        fileListView.getItems().add(currentFileName); // Add the first default file
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                fileContents.put(currentFileName, codeArea.getText());
                currentFileName = newVal;
                codeArea.replaceText(fileContents.get(newVal));
            }
        });

        applyLightMode();
        VBox leftPane = new VBox(new Label("Files"), fileListView);

        // Center SplitPane
        SplitPane centerSplitPane = createSplitPane();

        // Create a horizontal SplitPane to include the resizable left pane
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.setOrientation(Orientation.HORIZONTAL);
        mainSplitPane.getItems().addAll(leftPane, centerSplitPane);

        // Optionally, set the initial divider position (e.g., 20% for the left pane)
        mainSplitPane.setDividerPositions(0.2);

        // Visualization Workarea
        VBox visualizationWorkarea = createVisualizationWorkarea();
        mainSplitPane.getItems().add(visualizationWorkarea);

        // Layout Setup
        root.setTop(menuBar);
        root.setCenter(mainSplitPane);

        primaryStage.setMaximized(true);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Enhanced IDE");
        primaryStage.show();
    }

    private VBox createVisualizationWorkarea() {
        VBox visualizationBox = new VBox(10);
        visualizationBox.setPadding(new javafx.geometry.Insets(10));

        // Label to display the current line of code
        currentLineLabel = new Label("Current Line: ");
        currentLineLabel.setFont(Font.font(14));

        // Pane for visualization
        visualizationPane = new Pane();
        visualizationPane.setPrefSize(1000, 500);
        visualizationPane.setStyle("-fx-background-color: #f0f0f0;");

        // Buttons for navigation
        Button nextButton = new Button("Next");
        Button prevButton = new Button("Previous");

        nextButton.setOnAction(e -> visualizeNextLine());
        prevButton.setOnAction(e -> visualizePreviousLine());

        HBox buttonBox = new HBox(10, prevButton, nextButton);

        // Add components to the visualization box
        visualizationBox.getChildren().addAll(currentLineLabel, visualizationPane, buttonBox);

        return visualizationBox;
    }

    private void visualizeNextLine() {
        if (currentLineIndex < codeLines.size() - 1) {
            currentLineIndex++;
            updateVisualization();
        }
    }

    private void visualizePreviousLine() {
        if (currentLineIndex > 0) {
            currentLineIndex--;
            updateVisualization();
        }
    }

    private void updateVisualization() {
        // Update the current line label
        currentLineLabel.setText("Current Line: " + codeLines.get(currentLineIndex));

        // Highlight the current line in the code area
        codeArea.moveTo(currentLineIndex, 0);
        codeArea.requestFollowCaret();

        // Parse and visualize the current line
        String currentLine = codeLines.get(currentLineIndex).trim();

        // Handle variable declarations and assignments
        if (currentLine.matches("(int|float|double|string|char|bool|short)\\s+\\w+\\s*(=\\s*[^;]+)?;")) {
            // Extract variable name and value
            String[] parts = currentLine.split("=|;");
            String declaration = parts[0].trim();
            String[] declParts = declaration.split("\\s+");
            String type = declParts[0];
            String varName = declParts[1];

            Object value = null;
            if (parts.length > 1) {
                value = evaluateExpression(parts[1].replace(";", "").trim(), type);
            } else {
                // Assign default values for char, string, bool, and short
                if (type.equals("char")) {
                    value = '\0';  // Default null character
                } else if (type.equals("string")) {
                    value = "null"; // Default empty string
                } else if (type.equals("bool")) {
                    value = false; // Default false
                } else if (type.equals("short")) {
                    value = (short) 0; // Default short value
                }
            }

            // Update variables map
            variables.put(varName, value);

            // Draw variable box
            if (variableBoxes.containsKey(varName)) {
                variableTexts.get(varName).setText(varName + " = " + value); // Update text directly
            } else {
                // **New variable: Create UI elements**
                Rectangle rect = new Rectangle(10, 50 + (variables.size() - 1) * 40, 100, 30);
                rect.setFill(Color.LIGHTBLUE);
                rect.setStroke(Color.BLACK);

                Text varText = new Text(15, 70 + (variables.size() - 1) * 40, varName + " = " + value);
                varText.setFont(Font.font("Arial", FontWeight.BOLD, 14));

                visualizationPane.getChildren().addAll(rect, varText);
                variableBoxes.put(varName, rect);
                variableTexts.put(varName, varText);

                // If the variable is an array, string, or vector, visualize it as partitioned boxes
                if (type.equals("string") || type.equals("int[]") || type.equals("vector")) {
                    visualizeDataStructure(varName, value, type);
                }
            }
        } else if (currentLine.matches("\\w+\\s*=\\s*[^;]+;")) {
            // Handle assignments
            String[] parts = currentLine.split("=");
            String varName = parts[0].trim();
            String valueStr = parts[1].replace(";", "").trim();

            // Determine the type of the variable
            String type = "int"; // Default type
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

            // Evaluate the value
            Object value;
            if (type.equals("char") && valueStr.matches("'.'")) {
                value = valueStr.charAt(1); // Extract the character inside single quotes
            } else {
                value = evaluateExpression(valueStr, type);
            }

            // Update variable value
            variables.put(varName, value);

            // Update visualization
            if (variableBoxes.containsKey(varName)) {
                // Remove old text
                Text oldText = variableTexts.get(varName);
                visualizationPane.getChildren().remove(oldText);

                // Add new text
                Text newText = new Text(oldText.getX(), oldText.getY(), varName + " = " + value);
                newText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                visualizationPane.getChildren().add(newText);

                // Update variableTexts map
                variableTexts.put(varName, newText);
            }
        } else if (currentLine.matches("\\w+\\[\\d+\\]\\s*=\\s*[^;]+;")) {
            // Handle array/vector index assignments (e.g., arr[2] = 10; or s[1] = 'a';)
            String[] parts = currentLine.split("=");
            String leftSide = parts[0].trim(); // e.g., "arr[2]"
            String valueStr = parts[1].replace(";", "").trim(); // e.g., "10"

            // Extract variable name and index
            String varName = leftSide.split("\\[")[0]; // e.g., "arr"
            int index = Integer.parseInt(leftSide.split("\\[")[1].replace("]", "")); // e.g., 2

            // Evaluate the new value
            Object value = evaluateExpression(valueStr, "int"); // Assuming int for simplicity

            // Update the array/vector element
            updateArrayElement(varName, index, value);

            // Update visualization for the specific index
            updateDataStructureVisualization(varName, index, value);
        } else if (currentLine.startsWith("cout")) {
            // Handle cout statements
            String output = evaluateCoutStatement(currentLine);
            Text outputText = new Text(10, 200, "Output: " + output);
            outputText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            visualizationPane.getChildren().add(outputText);
        }
    }

    private void visualizeDataStructure(String varName, Object value, String type) {
        // Clear previous visualization for this data structure
        visualizationPane.getChildren().removeIf(node -> node instanceof Rectangle && node.getUserData() != null && node.getUserData().equals(varName));
        visualizationPane.getChildren().removeIf(node -> node instanceof Text && node.getUserData() != null && node.getUserData().equals(varName));

        // Position the data structure visualization closer to the variable name
        double startX = 150; // Start X position for data structure visualization
        double startY = 50 + (variables.size() - 1) * 40;

        if (type.equals("string")) {
            String strValue = (String) value;
            for (int i = 0; i < strValue.length(); i++) {
                Rectangle rect = new Rectangle(startX + i * 40, startY, 30, 30);
                rect.setFill(Color.LIGHTGREEN);
                rect.setStroke(Color.BLACK);
                rect.setUserData(varName); // Tag the rectangle with the variable name

                Text charText = new Text(startX + i * 40 + 10, startY + 20, String.valueOf(strValue.charAt(i)));
                charText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                charText.setUserData(varName); // Tag the text with the variable name

                Text indexText = new Text(startX + i * 40 + 10, startY + 50, String.valueOf(i));
                indexText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                indexText.setUserData(varName); // Tag the text with the variable name

                visualizationPane.getChildren().addAll(rect, charText, indexText);
            }
        } else if (type.equals("int[]")) {
            int[] arrayValue = (int[]) value;
            for (int i = 0; i < arrayValue.length; i++) {
                Rectangle rect = new Rectangle(startX + i * 40, startY, 30, 30);
                rect.setFill(Color.LIGHTGREEN);
                rect.setStroke(Color.BLACK);
                rect.setUserData(varName); // Tag the rectangle with the variable name

                Text valueText = new Text(startX + i * 40 + 10, startY + 20, String.valueOf(arrayValue[i]));
                valueText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                valueText.setUserData(varName); // Tag the text with the variable name

                Text indexText = new Text(startX + i * 40 + 10, startY + 50, String.valueOf(i));
                indexText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                indexText.setUserData(varName); // Tag the text with the variable name

                visualizationPane.getChildren().addAll(rect, valueText, indexText);
            }
        } else if (type.equals("vector")) {
            List<?> vectorValue = (List<?>) value;
            for (int i = 0; i < vectorValue.size(); i++) {
                Rectangle rect = new Rectangle(startX + i * 40, startY, 30, 30);
                rect.setFill(Color.LIGHTGREEN);
                rect.setStroke(Color.BLACK);
                rect.setUserData(varName); // Tag the rectangle with the variable name

                Text valueText = new Text(startX + i * 40 + 10, startY + 20, String.valueOf(vectorValue.get(i)));
                valueText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                valueText.setUserData(varName); // Tag the text with the variable name

                Text indexText = new Text(startX + i * 40 + 10, startY + 50, String.valueOf(i));
                indexText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                indexText.setUserData(varName); // Tag the text with the variable name

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
                    // Update the string value
                    strValue = strValue.substring(0, index) + value + strValue.substring(index + 1);
                    variables.put(varName, strValue);
                    // Revisualize the string
                    visualizeDataStructure(varName, strValue, "string");
                }
            } else if (dataStructure instanceof int[]) {
                int[] arrayValue = (int[]) dataStructure;
                if (index >= 0 && index < arrayValue.length) {
                    // Update the array value
                    arrayValue[index] = (int) value;
                    // Revisualize the array
                    visualizeDataStructure(varName, arrayValue, "int[]");
                }
            } else if (dataStructure instanceof List) {
                List<?> vectorValue = (List<?>) dataStructure;
                if (index >= 0 && index < vectorValue.size()) {
                    // Update the vector value
                    ((List<Object>) vectorValue).set(index, value);
                    // Revisualize the vector
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
                case "int": return Integer.parseInt(expression);
                case "float": return Float.parseFloat(expression);
                case "double": return Double.parseDouble(expression);
                case "string": return expression.replace("\"", "");
                case "char": return expression.length() == 3 ? expression.charAt(1) : '\0';
                case "bool": return Boolean.parseBoolean(expression);
                case "short": return Short.parseShort(expression);
            }
        } catch (NumberFormatException ignored) {}

        return evaluateArithmeticExpression(expression);
    }

    private Object evaluateArithmeticExpression(String expression) {
        List<String> postfix = infixToPostfix(expression);
        Stack<Number> stack = new Stack<>();

        for (String token : postfix) {
            if (token.matches("\\d+")) {  // Integer case
                stack.push(Integer.parseInt(token));
            } else if (token.matches("\\d+\\.\\d+")) {  // Double case
                stack.push(Double.parseDouble(token));
            } else if (variables.containsKey(token)) {
                stack.push((Number) variables.get(token));
            } else {
                Number b = stack.pop();
                Number a = stack.pop();

                if (a instanceof Integer && b instanceof Integer) {
                    switch (token) {
                        case "+": stack.push(a.intValue() + b.intValue()); break;
                        case "-": stack.push(a.intValue() - b.intValue()); break;
                        case "*": stack.push(a.intValue() * b.intValue()); break;
                        case "/": stack.push(a.intValue() / b.intValue()); break;
                    }
                } else { // At least one operand is a Double
                    double aVal = a.doubleValue();
                    double bVal = b.doubleValue();
                    double result=0;
                    switch (token) {
                        case "+": result=(aVal + bVal); break;
                        case "-": result=(aVal - bVal); break;
                        case "*": result=(aVal * bVal); break;
                        case "/": result=(aVal / bVal); break;
                    }
                    //result=Math.round(result * 100.0) / 100.0;
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
        // Extract the expression inside cout
        String expression = coutLine.replace("cout", "").replace("<<", "").replace(";", "").trim();
        return String.valueOf(evaluateExpression(expression, "int"));
    }

    private void initializeCodeLines() {
        String code = codeArea.getText();
        codeLines = Arrays.asList(code.split("\n"));
        currentLineIndex = 0;
        variables.clear(); // Reset variables
        variableBoxes.clear(); // Reset variable boxes
        variableTexts.clear(); // Reset variable texts
        visualizationPane.getChildren().clear(); // Clear visualization pane
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
    // Add this method to the "Visualize" button action
    private void startVisualization() {
        initializeCodeLines();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            visualizeArray(entry.getKey(), entry.getValue());
        }
    }



    // Add a "Visualize" button to the menu or toolbar
    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem newFile = new MenuItem("New");
        MenuItem closeFile = new MenuItem("Close");
        //MenuItem deleteFile = new MenuItem("Delete");

        fileMenu.getItems().addAll(newFile, openFile, saveFile, closeFile);

        Menu runMenu = new Menu("Run");
        MenuItem runCode = new MenuItem("Run");
        MenuItem visualizeCode = new MenuItem("Visualize");
        runMenu.getItems().addAll(runCode, visualizeCode);

        visualizeCode.setOnAction(e -> startVisualization());

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

        newFile.setOnAction(e -> createNewFile(primaryStage));
        openFile.setOnAction(e -> openFile(primaryStage));
        saveFile.setOnAction(e -> saveFile(primaryStage));
        closeFile.setOnAction(e -> closeFile(primaryStage));
        runCode.setOnAction(e -> runCode());
        debugCode.setOnAction(e -> debugCode());
        //deleteCode.setOnAction(e -> deleteCode());
        aboutApp.setOnAction(e -> detailsAboutIDE());
        formatCode.setOnAction(e -> codeFormat());
        analyzeCode.setOnAction(e -> codeAnalyze());
        documentation.setOnAction(e -> appDocumentation());
        lightMode.setOnAction(e -> applyLightMode());
        darkMode.setOnAction(e -> applyDarkMode());

        return menuBar;
    }

    private void createNewFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Create New File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("C++ Files", "*.cpp"));
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            String fileName = file.getName();
            if (!fileListView.getItems().contains(fileName)) {
                fileListView.getItems().add(fileName);
                fileContents.put(fileName, "");
                currentFileName = fileName;
                codeArea.clear();
            }
        }
    }

    private void closeFile(Stage stage) {
        if (fileContents.containsKey(currentFileName)) {
            String currentContent = fileContents.get(currentFileName);
            String codeAreaContent = codeArea.getText();

            if (!currentContent.equals(codeAreaContent)) {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("You have unsaved changes.");
                alert.setContentText("Do you want to save them before closing the file?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    saveFile(stage);
                }
            }

            fileListView.getItems().remove(currentFileName);
            fileContents.remove(currentFileName);
            if (!fileListView.getItems().isEmpty()) {
                currentFileName = fileListView.getItems().get(0);
                codeArea.replaceText(fileContents.get(currentFileName));
            } else {
                currentFileName = "Untitled1";
                codeArea.clear();
            }
        }
    }


    private CodeArea createCodeArea() {
        CodeArea codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.textProperty().addListener((obs, oldText, newText) -> applySyntaxHighlighting(newText));
        codeArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 14px;");
        codeArea.getStyleClass().add("light-mode");
        codeArea.getStylesheets().add(getClass().getResource("/Main.css").toExternalForm());
        return codeArea;
    }

    private SplitPane createSplitPane() {
        // Create output area for displaying results
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: white;");
        outputArea.setPrefHeight(150); // You can tweak this based on preference

        // Create input area for user input (e.g., for running programs)
        inputArea = new TextArea();
        inputArea.setPromptText("Enter input for your program here...");
        inputArea.setPrefHeight(100); // This can be adjusted dynamically

        // Create the code area (for coding)
        codeArea = createCodeArea(); // Initialize the codeArea

        // Create the SplitPane
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL); // Stack vertically (code on top, input/output on bottom)

        // Add the areas to the split pane
        splitPane.getItems().addAll(codeArea, inputArea, outputArea);

        // Set divider positions (these can be adjusted to allow resizing)
        splitPane.setDividerPositions(0.7, 0.85); // Initially, codeArea gets 70%, input/output get 30%

        return splitPane;
    }

    private String getFileContent(String fileName) {
        StringBuilder content = new StringBuilder();
        File file = new File(fileName); // Locate the file

        // Ensure the file exists before trying to read it
        if (!file.exists()) {
            showError("File Not Found", "The file '" + fileName + "' does not exist.");
            return ""; // Return empty content if the file doesn't exist
        }

        // Read the file content
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            showError("Error Reading File", e.getMessage());
        }

        return content.toString();
    }


    private void openFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("C++ Files", "*.cpp"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            String fileName = file.getName();
            if (!fileListView.getItems().contains(fileName)) {
                fileListView.getItems().add(fileName);
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    fileContents.put(fileName, content.toString());
                    currentFileName = fileName;
                    codeArea.replaceText(content.toString());
                } catch (IOException e) {
                    showError("Error opening file", e.getMessage());
                }
            }
        }
    }


    private void saveFile(Stage stage) {
        if (currentFileName != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("C++ Files", "*.cpp"));
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(codeArea.getText());
                    fileContents.put(currentFileName, codeArea.getText());
                } catch (IOException e) {
                    showError("Error saving file", e.getMessage());
                }
            }
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
            if (variablesSet.add(variable)) {
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
        // Define regex patterns for keywords, types, comments, strings, etc.
        String keywordPattern = "\\b(int|double|float|char|bool|void|if|else|while|for|return|using|namespace|include|std|cin|cout|vector|endl|main)\\b";
        String commentPattern = "//[^\n]*|/\\*(.|\\R)*?\\*/";
        String stringPattern = "\"([^\"\\\\]|\\\\.)*\"";
        String numberPattern = "\\b(\\d+\\.\\d*|\\d*\\.\\d+|\\d+|0x[0-9a-fA-F]+|0b[01]+|0[0-7]+)\\b";
        String headerPattern = "<[^>]{1,20}>";  // Limited to 20 characters
        String inbuildValueTypePattern = "\\b(true|false|NULL|INT_MAX|INT_MIN|SIZE_MAX|FLT_MAX|DBL_MAX|LDBL_MAX|NaN|INF)\\b";
        String symbolPattern = "[,\\=\\+\\-\\*\\/\\;\\#\\<>\\(\\)\\{\\}\\[\\]]|[&|!%<>\\?\\:\\=\\^\\~\\.,]";
        String variablePattern = "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b";
        String builtinPattern = "\\b(cin|cout|endl)\\b";
        String typePattern = "\\b(string|vector|list|deque|set|map|unordered_map|unordered_set|pair|queue|stack|array|bitset|forward_list|shared_ptr|unique_ptr|weak_ptr|tuple|complex|class|public|private|protected|const|static|virtual|inline|typename|enum|struct|union|decltype|auto|nullptr)\\b";


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

        // Matcher to find all patterns in the input text
        Matcher matcher = pattern.matcher(text);

        // StyleSpansBuilder to collect style information
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastMatchEnd = 0;

        // Iterate through all the matched groups
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

        // Apply the collected style spans to the code area
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
