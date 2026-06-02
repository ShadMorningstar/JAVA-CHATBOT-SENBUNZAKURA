package ui;

import api.GeminiAPI;
import db.DatabaseHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;

public class ChatScreen extends Application {

    private VBox chatBox;
    private ScrollPane scrollPane;
    private TextField inputField;
    private VBox sidebarBox;

    private String currentSessionId;
    private Timer autoSendTimer;
    private boolean isVoiceModeActive = false; // 🔥 Mic status check

    private final String BG_COLOR = "#0d0e15";
    private final String BUBBLE_USER = "#1a1c29";
    private final String BUBBLE_AI = "#161322";
    private final String PINK_ACCENT = "#ff2a5f";
    private final String BLUE_ACCENT = "#00f0ff";

    @Override
    public void start(Stage stage) {
        createNewSession();

        HBox mainLayout = new HBox();
        mainLayout.setStyle("-fx-background-color: " + BG_COLOR + ";");

        // ================== 1. LEFT SIDEBAR ==================
        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(280);
        sidebar.setPadding(new Insets(20, 15, 20, 15));
        sidebar.setStyle("-fx-background-color: #08090d; -fx-border-color: #1b1d2e; -fx-border-width: 0 1 0 0;");

        Button newChatBtn = new Button("+ New Chat Session");
        newChatBtn.setMaxWidth(Double.MAX_VALUE);
        newChatBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + BLUE_ACCENT + "; -fx-border-color: " + BLUE_ACCENT + "; -fx-border-radius: 20; -fx-padding: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        newChatBtn.setOnAction(e -> {
            createNewSession();
            chatBox.getChildren().clear();
            GeminiAPI.clearHistory();
        });

        Button clearAllBtn = new Button("🗑 Clear All History");
        clearAllBtn.setMaxWidth(Double.MAX_VALUE);
        clearAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #565a75; -fx-border-color: #2c2f48; -fx-border-radius: 20; -fx-padding: 6; -fx-font-size: 11px; -fx-cursor: hand;");
        clearAllBtn.setOnAction(e -> {
            DatabaseHelper.clearAllSessions();
            createNewSession();
            chatBox.getChildren().clear();
            GeminiAPI.clearHistory();
            refreshSidebarSessions();
        });

        Label historyTitle = new Label("CHAT HISTORY TIMES");
        historyTitle.setStyle("-fx-text-fill: #565a75; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px; padding-top: 10px;");

        sidebarBox = new VBox(8);
        VBox.setVgrow(sidebarBox, Priority.ALWAYS);

        refreshSidebarSessions();
        sidebar.getChildren().addAll(newChatBtn, clearAllBtn, historyTitle, sidebarBox);

        // ================== 2. RIGHT MAIN CHAT SCREEN ==================
        VBox chatContentArea = new VBox();
        HBox.setHgrow(chatContentArea, Priority.ALWAYS);

        HBox header = new HBox();
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: #11131f; -fx-border-color: " + PINK_ACCENT + "; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("SENBUNZAKURA AI");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        header.getChildren().add(titleLabel);

        chatBox = new VBox(18);
        chatBox.setPadding(new Insets(25));
        chatBox.setStyle("-fx-background-color: " + BG_COLOR + ";");

        scrollPane = new ScrollPane(chatBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: " + BG_COLOR + "; -fx-background-color: " + BG_COLOR + "; -fx-vbar-policy: as-needed;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox inputBar = new HBox(12);
        inputBar.setPadding(new Insets(20));
        inputBar.setStyle("-fx-background-color: #11131f; -fx-border-color: #1b1d2e; -fx-border-width: 1 0 0 0;");
        inputBar.setAlignment(Pos.CENTER_LEFT);

        // UI Clean mic button
        Button micBtn = new Button("🎤 Mic Mode");
        micBtn.setStyle("-fx-background-color: #141622; -fx-text-fill: " + BLUE_ACCENT + "; -fx-border-color: " + BLUE_ACCENT + "; -fx-border-radius: 20; -fx-padding: 10 15; -fx-font-weight: bold; -fx-cursor: hand;");

        inputField = new TextField();
        inputField.setPromptText("Type a message or use /draw...");
        inputField.setStyle("-fx-background-color: #1b1d2e; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 25; -fx-padding: 12 20; -fx-border-color: #2c2f48; -fx-border-radius: 25;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // 🔥 FOOLPROOF VOICE DETECTION ENGINE
        micBtn.setOnAction(e -> {
            isVoiceModeActive = !isVoiceModeActive;
            if (isVoiceModeActive) {
                micBtn.setStyle("-fx-background-color: " + PINK_ACCENT + "; -fx-text-fill: white; -fx-border-color: " + PINK_ACCENT + "; -fx-border-radius: 20; -fx-padding: 10 15; -fx-font-weight: bold;");
                inputField.setPromptText("Voice Active! Press [Win + H] once, speak, then pause to auto-send...");
                inputField.requestFocus();
            } else {
                micBtn.setStyle("-fx-background-color: #141622; -fx-text-fill: " + BLUE_ACCENT + "; -fx-border-color: " + BLUE_ACCENT + "; -fx-border-radius: 20; -fx-padding: 10 15; -fx-font-weight: bold;");
                inputField.setPromptText("Type a message or use /draw...");
            }
        });

        // Smart text tracking listener for auto sending
        inputField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isVoiceModeActive && !newValue.trim().isEmpty()) {
                if (autoSendTimer != null) {
                    autoSendTimer.cancel();
                }
                autoSendTimer = new Timer();
                autoSendTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            handleMessage(); // Trigger Send command on quiet threshold
                        });
                    }
                }, 1500); // 1.5 Second dynamic pause filter
            }
        });

        Button sendBtn = new Button("SEND ➤");
        sendBtn.setStyle("-fx-background-color: " + PINK_ACCENT + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 12 24; -fx-cursor: hand;");

        inputBar.getChildren().addAll(micBtn, inputField, sendBtn);
        chatContentArea.getChildren().addAll(header, scrollPane, inputBar);

        mainLayout.getChildren().addAll(sidebar, chatContentArea);

        sendBtn.setOnAction(e -> handleMessage());
        inputField.setOnAction(e -> handleMessage());

        stage.setScene(new Scene(mainLayout, 1200, 780));
        stage.setTitle("Senbunzakura AI — Stable Voice Tracker Edition");
        stage.show();
    }

    private void createNewSession() {
        currentSessionId = "Chat_" + new SimpleDateFormat("dd-MMM_HH:mm:ss").format(new Date());
    }

    private void refreshSidebarSessions() {
        sidebarBox.getChildren().clear();
        List<String> sessions = DatabaseHelper.getAllSessions();
        for (String sessionToken : sessions) {
            HBox sessionRow = new HBox(5);
            sessionRow.setAlignment(Pos.CENTER_LEFT);

            Button sessionBtn = new Button("⏳ " + sessionToken);
            sessionBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(sessionBtn, Priority.ALWAYS);
            sessionBtn.setAlignment(Pos.CENTER_LEFT);
            sessionBtn.setStyle("-fx-background-color: #141622; -fx-text-fill: #a0a5c1; -fx-background-radius: 8 0 0 8; -fx-padding: 8 12; -fx-cursor: hand;");
            sessionBtn.setOnAction(e -> loadOldSession(sessionToken));

            Button delBtn = new Button("❌");
            delBtn.setStyle("-fx-background-color: #141622; -fx-text-fill: #565a75; -fx-background-radius: 0 8 8 0; -fx-padding: 8 10; -fx-cursor: hand;");
            delBtn.setOnAction(e -> {
                DatabaseHelper.deleteSession(sessionToken);
                if (sessionToken.equals(currentSessionId)) {
                    createNewSession();
                    chatBox.getChildren().clear();
                    GeminiAPI.clearHistory();
                }
                refreshSidebarSessions();
            });

            sessionRow.getChildren().addAll(sessionBtn, delBtn);
            sidebarBox.getChildren().add(sessionRow);
        }
    }

    private void loadOldSession(String sessionId) {
        this.currentSessionId = sessionId;
        chatBox.getChildren().clear();
        List<String[]> dbHistory = DatabaseHelper.getSessionHistory(sessionId);
        GeminiAPI.reloadContextFromHistory(dbHistory);

        for (String[] row : dbHistory) {
            String sender = row[0];
            if (sender.equalsIgnoreCase("Tu")) {
                sender = "YOU";
            }
            renderMessageUI(sender, row[1], sender.equalsIgnoreCase("YOU"), row[1].startsWith("https://image.pollinations.ai"));
        }
    }

    private void handleMessage() {
        if (autoSendTimer != null) {
            autoSendTimer.cancel();
            autoSendTimer = null;
        }

        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;

        DatabaseHelper.saveChatMessage(currentSessionId, "Tu", msg);
        addMessage("YOU", msg, true, false);
        inputField.clear();

        CompletableFuture.supplyAsync(() -> GeminiAPI.chat(msg))
                .thenAccept(res -> Platform.runLater(() -> {
                    if (res.startsWith("IMAGE_GEN:")) {
                        String prompt = res.replace("IMAGE_GEN:", "").trim().replace(" ", "%20");
                        String imageUrl = "https://image.pollinations.ai/prompt/" + prompt;
                        DatabaseHelper.saveChatMessage(currentSessionId, "Senbunzakura", imageUrl);
                        addMessage("Senbunzakura", imageUrl, false, true);
                    } else {
                        DatabaseHelper.saveChatMessage(currentSessionId, "Senbunzakura", res);
                        addMessage("Senbunzakura", res, false, false);
                    }
                    refreshSidebarSessions();

                    // Voice mode reset control
                    if (isVoiceModeActive) {
                        inputField.setPromptText("Listening... Speak again or pause to send...");
                    } else {
                        inputField.setPromptText("Type a message or use /draw...");
                    }
                }));
    }

    private void addMessage(String sender, String content, boolean isUser, boolean isImage) {
        renderMessageUI(sender, content, isUser, isImage);
        Platform.runLater(this::scrollToBottom);
    }

    private void renderMessageUI(String sender, String content, boolean isUser, boolean isImage) {
        VBox bubbleContainer = new VBox(4);
        bubbleContainer.setMaxWidth(550);

        Label sLabel = new Label(sender.toUpperCase());
        sLabel.setStyle("-fx-text-fill: " + (isUser ? BLUE_ACCENT : PINK_ACCENT) + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        VBox bubble = new VBox(5);
        bubble.setPadding(new Insets(12, 16, 12, 16));
        bubble.setStyle("-fx-background-color: " + (isUser ? BUBBLE_USER : BUBBLE_AI) + "; -fx-background-radius: " + (isUser ? "18 18 2 18" : "18 18 18 2") + "; -fx-border-color: " + (isUser ? "#25283d" : "#2d1b33") + "; -fx-border-width: 1;");

        if (isImage) {
            Image img = new Image(content, true);
            ImageView imgView = new ImageView(img);
            imgView.setFitWidth(500); imgView.setPreserveRatio(true);
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.setArcWidth(20); clip.setArcHeight(20); imgView.setClip(clip);
            imgView.boundsInLocalProperty().addListener((obs, oldB, newB) -> {
                clip.setWidth(newB.getWidth()); clip.setHeight(newB.getHeight());
            });
            bubble.getChildren().add(imgView);
        } else {
            Label cLabel = new Label(content);
            cLabel.setWrapText(true); cLabel.setStyle("-fx-text-fill: #e1e2ec; -fx-font-size: 14px;");
            bubble.getChildren().add(cLabel);
        }

        HBox actionBtnBar = new HBox(8);
        actionBtnBar.setAlignment(isUser ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

        Button copyBtn = new Button("📋 Copy");
        copyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #565a75; -fx-font-size: 10px; -fx-cursor: hand;");
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent cc = new ClipboardContent();
            cc.putString(content); clipboard.setContent(cc);
            copyBtn.setText("✓ Copied!");
        });

        actionBtnBar.getChildren().add(copyBtn);
        bubbleContainer.getChildren().addAll(sLabel, bubble, actionBtnBar);
        bubbleContainer.setAlignment(isUser ? Pos.TOP_RIGHT : Pos.TOP_LEFT);

        HBox align = new HBox(bubbleContainer);
        align.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatBox.getChildren().add(align);
    }

    private void scrollToBottom() {
        chatBox.layout();
        scrollPane.setVvalue(1.0);
    }
}