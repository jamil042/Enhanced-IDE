import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Orientation;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import java.io.BufferedReader;
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
import javax.websocket.*;
import java.net.URI;

public class Main extends Application {

    private CodeArea codeArea;
    private TextArea outputArea;
    private TextArea inputArea;
    private ListView<String> fileListView;
    private String currentFileName = "Untitled1";
    private LiveCodeClient liveCodeClient;

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
                currentFileName = newVal;
                codeArea.clear(); // Clear the code area (or load content based on actual implementation)
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

        // Layout Setup
        root.setTop(menuBar);
        root.setCenter(mainSplitPane);

        primaryStage.setMaximized(true);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Enhanced IDE");
        primaryStage.show();
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem newFile = new MenuItem("New");
        MenuItem closeFile = new MenuItem("Close");
        MenuItem deleteFile = new MenuItem("Delete");

        fileMenu.getItems().addAll(newFile, openFile, saveFile, closeFile, deleteFile);

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

        // Go Live Menu
        Menu goLiveMenu = new Menu("Go Live");
        MenuItem startLiveSession = new MenuItem("Start Live Session");
        MenuItem joinLiveSession = new MenuItem("Join Live Session");
        goLiveMenu.getItems().addAll(startLiveSession, joinLiveSession);

        menuBar.getMenus().addAll(fileMenu, runMenu, debugMenu, toolsMenu, settingMenu, goLiveMenu, helpMenu);

        openFile.setOnAction(e -> openFile(primaryStage));
        newFile.setOnAction(e -> saveFile(primaryStage));
        runCode.setOnAction(e -> runCode());
        debugCode.setOnAction(e -> debugCode());
        deleteCode.setOnAction(e -> deleteCode());
        aboutApp.setOnAction(e -> detailsAboutIDE());
        formatCode.setOnAction(e -> codeFormat());
        analyzeCode.setOnAction(e -> codeAnalyze());
        documentation.setOnAction(e -> appDocumentation());
        lightMode.setOnAction(e -> applyLightMode());
        darkMode.setOnAction(e -> applyDarkMode());

        // Go Live Event Handlers
        startLiveSession.setOnAction(e -> startLiveSession());
        joinLiveSession.setOnAction(e -> joinLiveSession());

        return menuBar;
    }

    private CodeArea createCodeArea() {
        CodeArea codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.textProperty().addListener((obs, oldText, newText) -> applySyntaxHighlighting(newText));
        codeArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 14px;");
        codeArea.getStyleClass().add("light-mode");
        codeArea.getStylesheets().add(getClass().getResource("/cssforIDE.css").toExternalForm());
        return codeArea;
    }

    private SplitPane createSplitPane() {
        // Create output area for displaying results
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: green;");
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

    private void startLiveSession() {
        // Start the WebSocket server
        new Thread(WebSocketStarter::startWebSocketServer).start();

        // Connect to the server as the host
        liveCodeClient = new LiveCodeClient(codeArea);
        liveCodeClient.connect("ws://localhost:8080/live");

        // Send initial code to the server
        liveCodeClient.sendMessage(codeArea.getText());

        // Listen for code changes and send updates
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            liveCodeClient.sendMessage(newText);
        });
    }

    private void joinLiveSession() {
        // Prompt the user to enter the server address
        TextInputDialog dialog = new TextInputDialog("ws://localhost:8080/live");
        dialog.setTitle("Join Live Session");
        dialog.setHeaderText("Enter the WebSocket server address:");
        dialog.setContentText("Server URI:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(serverUri -> {
            // Connect to the server as a participant
            liveCodeClient = new LiveCodeClient(codeArea);
            liveCodeClient.connect(serverUri);

            // Listen for code changes and send updates
            codeArea.textProperty().addListener((obs, oldText, newText) -> {
                liveCodeClient.sendMessage(newText);
            });
        });
    }

    // WebSocket Server
    @ServerEndpoint("/live")
    public static class LiveCodeServer {

        private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

        @OnOpen
        public void onOpen(Session session) {
            sessions.add(session);
            System.out.println("New client connected: " + session.getId());
        }

        @OnMessage
        public void onMessage(String message, Session session) {
            System.out.println("Received message: " + message);
            broadcast(message, session);
        }

        @OnClose
        public void onClose(Session session) {
            sessions.remove(session);
            System.out.println("Client disconnected: " + session.getId());
        }

        @OnError
        public void onError(Session session, Throwable throwable) {
            System.err.println("Error for client " + session.getId() + ": " + throwable.getMessage());
        }

        private void broadcast(String message, Session sender) {
            for (Session session : sessions) {
                if (session != sender && session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // WebSocket Client
    @ClientEndpoint
    public static class LiveCodeClient {

        private Session session;
        private CodeArea codeArea;

        public LiveCodeClient(CodeArea codeArea) {
            this.codeArea = codeArea;
        }

        public void connect(String serverUri) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this, new URI(serverUri));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @OnOpen
        public void onOpen(Session session) {
            this.session = session;
            System.out.println("Connected to server: " + session.getId());
        }

        @OnMessage
        public void onMessage(String message) {
            Platform.runLater(() -> codeArea.replaceText(message));
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            System.out.println("Disconnected from server: " + closeReason.getReasonPhrase());
        }

        @OnError
        public void onError(Session session, Throwable throwable) {
            System.err.println("WebSocket error: " + throwable.getMessage());
        }

        public void sendMessage(String message) {
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // WebSocket Server Starter
    public static class WebSocketStarter {
        public static void startWebSocketServer() {
            Server server = new Server(8080); // Port for WebSocket server
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);

            try {
                WebSocketServerContainerInitializer.configure(context, null);
                server.start();
                System.out.println("WebSocket server started on port 8080");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Rest of the methods (openFile, saveFile, runCode, debugCode, etc.) remain unchanged
    // ...

    public static void main(String[] args) {
        launch(args);
    }
}