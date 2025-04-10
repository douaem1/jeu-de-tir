package chat_Client_Serveur;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class GameChat {
    private VBox chatContainer;
    private TextArea chatArea;
    private TextField inputField;
    private Button toggleBtn;
    private boolean isVisible = false;
    private double windowWidth;
    private double windowHeight;

    public GameChat(Pane root, double windowWidth, double windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        createChatUI(root);
        setupChatDrag();
        positionChatInGame();
    }

    public void createChatUI(Pane root) {
        chatContainer = new VBox(10);
        chatContainer.setAlignment(Pos.BOTTOM_RIGHT);
        chatContainer.setPadding(new Insets(10));
        // Modifier le style pour qu'il soit semi-transparent
        chatContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-background-radius: 10;");
        chatContainer.setVisible(true);
        chatContainer.setMaxSize(300, 200);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("×");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> toggleVisibility());

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setStyle("-fx-control-inner-background: #00000055; " +
                "-fx-text-fill: lime; " +
                "-fx-font-family: 'Courier New'; " +
                "-fx-font-size: 14px;");
        chatArea.setPrefHeight(150);

        inputField = new TextField();
        inputField.setPromptText("Tapez votre message...");
        inputField.setStyle("-fx-background-color: #ffffff15; " +
                "-fx-text-fill: #00ff00; " +
                "-fx-font-style: italic; " +
                "-fx-prompt-text-fill: #aaaaaa;");

        header.getChildren().add(closeBtn);
        chatContainer.getChildren().addAll(header, chatArea, inputField);
        root.getChildren().add(chatContainer);

        toggleBtn = new Button("Chat");
        toggleBtn.setStyle("-fx-background-color: #2E86AB55; -fx-text-fill: white; -fx-background-radius: 5;");
        toggleBtn.setOnAction(e -> toggleVisibility());
        root.getChildren().add(toggleBtn);
    }

    public void setupChatDrag() {
        final double[] offset = new double[2];
        if(!isVisible) return;
        chatContainer.setOnMousePressed(e -> {
            offset[0] = e.getSceneX() - chatContainer.getTranslateX();
            offset[1] = e.getSceneY() - chatContainer.getTranslateY();
        });

        chatContainer.setOnMouseDragged(e -> {
            double newX = e.getSceneX() - offset[0];
            double newY = e.getSceneY() - offset[1];

            newX = Math.max(windowWidth - 350, Math.min(newX, windowWidth - 300)); // Permet un décalage léger
            newY = Math.max(windowHeight - 250, Math.min(newY, windowHeight - 200));

            chatContainer.setTranslateX(newX);
            chatContainer.setTranslateY(newY);

            toggleBtn.setTranslateX(newX);
            toggleBtn.setTranslateY(newY + chatContainer.getHeight() + 5);
        });
    }

    private void toggleVisibility() {
        isVisible = !isVisible;
        chatContainer.setVisible(isVisible);
        toggleBtn.setText(isVisible ? "▼ Chat" : "▲ Chat"); // Inverser les symboles
    }

    public TextField getInputField() {
        return inputField;
    }

    public VBox getChatContainer() {
        return chatContainer;
    }

    public Button getToggleBtn() {
        return toggleBtn;
    }

    public TextArea getChatArea() {
        return chatArea;
    }

    public void positionChatInGame() {
        if (chatContainer == null) return;
        chatContainer.setTranslateX(windowWidth - 320);
        chatContainer.setTranslateY(windowHeight - 220);
        toggleBtn.setTranslateX(windowWidth - 100);
        toggleBtn.setTranslateY(windowHeight - 50);
    }
}

