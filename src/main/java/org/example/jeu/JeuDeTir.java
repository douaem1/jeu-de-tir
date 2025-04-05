package org.example.jeu;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import java.io.InputStream;

public class JeuDeTir extends Application {
    private Stage primaryStage;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private static final String COLOR_PRIMARY = "#2E86AB";
    private static final String COLOR_SECONDARY = "#F18F01";
    private static final String COLOR_ACCENT = "#A23B72";
    private static final String COLOR_DANGER = "#C73E1D";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Jet Fighters - Édition Premium");
        setupMainMenu();
    }

    private void setupMainMenu() {
        StackPane root = new StackPane();
        ImageView background = loadBackgroundImage();
        root.getChildren().add(background);

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.3, Color.rgb(0, 0, 0, 0.4)),
                new Stop(1, Color.rgb(0, 0, 0, 0.7))
        ));
        root.getChildren().add(overlay);

        VBox mainContainer = createMainContainer();
        root.getChildren().add(mainContainer);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    private ImageView loadBackgroundImage() {
        try {
            String[] paths = {"/background.jpg", "/background.png", "/img.jpg", "/img.png", "/image.jpg", "/image.png"};
            for (String path : paths) {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    Image image = new Image(is);
                    ImageView view = new ImageView(image);
                    setupBackgroundImage(view);
                    return view;
                }
            }
            throw new RuntimeException("Aucune image de fond trouvée");
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            return createDefaultBackground();
        }
    }

    private void setupBackgroundImage(ImageView imageView) {
        imageView.setFitWidth(WINDOW_WIDTH);
        imageView.setFitHeight(WINDOW_HEIGHT);
        imageView.setPreserveRatio(false);
        ColorAdjust adjust = new ColorAdjust();
        adjust.setBrightness(-0.1);
        adjust.setContrast(0.1);
        imageView.setEffect(adjust);
    }

    private ImageView createDefaultBackground() {
        Rectangle rect = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        rect.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(0.5, Color.web("#b21f1f")),
                new Stop(1, Color.web("#fdbb2d"))));
        return new ImageView(rect.snapshot(null, null));
    }

    private VBox createMainContainer() {
        VBox container = new VBox(25);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40, 60, 40, 60));
        container.setMaxWidth(750);

        Label title = createTitleLabel();
        Label subtitle = createSubtitleLabel();
        HBox actionButtons = createActionButtons();

        container.getChildren().addAll(title, subtitle, actionButtons);
        animateMenuEntrance(container);

        return container;
    }

    private Label createTitleLabel() {
        Label label = new Label("JET FIGHTERS");
        label.setFont(Font.font("Agency FB", FontWeight.EXTRA_BOLD, 74));
        label.setTextFill(Color.WHITE);
        DropShadow glow = new DropShadow(15, Color.web(COLOR_PRIMARY));
        glow.setSpread(0.3);
        label.setEffect(new Blend(BlendMode.SCREEN, new Glow(0.8), glow));
        animateTextGlow(glow);
        return label;
    }

    private void animateTextGlow(DropShadow glow) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 15)),
                new KeyFrame(Duration.seconds(2), new KeyValue(glow.radiusProperty(), 25)),
                new KeyFrame(Duration.seconds(4), new KeyValue(glow.radiusProperty(), 15))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private Label createSubtitleLabel() {
        Label label = new Label("Only the Fastest Survive the Sky");
        label.setFont(Font.font("Bank Gothic", FontWeight.SEMI_BOLD, 26));
        label.setTextFill(Color.web("#CCCCCC"));
        FadeTransition fade = new FadeTransition(Duration.seconds(3), label);
        fade.setFromValue(0.7);
        fade.setToValue(1.0);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
        return label;
    }

    private HBox createActionButtons() {
        HBox buttonBox = new HBox(5);
        buttonBox.setAlignment(Pos.CENTER);

        Button startBtn = createActionButton("START AS GUEST", COLOR_PRIMARY);
        Button signUpBtn = createActionButton("SIGN UP", COLOR_ACCENT);
        Button signInBtn = createActionButton("SIGN IN", COLOR_SECONDARY);
        Button quitBtn = createActionButton("QUITTER", COLOR_DANGER);

        startBtn.setOnAction(e -> showNotification("Initialisation du système..."));
        signUpBtn.setOnAction(e -> ShowSignUpScene());
        signInBtn.setOnAction(e -> showSignInScene());
        quitBtn.setOnAction(e -> primaryStage.close());

        buttonBox.getChildren().addAll(startBtn, signUpBtn, signInBtn, quitBtn);
        return buttonBox;
    }

    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18; -fx-padding: 12 30; -fx-background-radius: 30; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");

        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace(color, darkenColor(color, 0.2)) +
                "-fx-effect: dropshadow(gaussian, " + color + ", 15, 0.5, 0, 0);"));

        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace(darkenColor(color, 0.2), color)
                .replace("-fx-effect:.*;", "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);")));

        return btn;
    }

    private String darkenColor(String hexColor, double factor) {
        Color color = Color.web(hexColor);
        return String.format("#%02x%02x%02x",
                (int)(color.getRed() * 255 * (1 - factor)),
                (int)(color.getGreen() * 255 * (1 - factor)),
                (int)(color.getBlue() * 255 * (1 - factor)));
    }

    private void animateMenuEntrance(VBox container) {
        FadeTransition fade = new FadeTransition(Duration.seconds(1.2), container);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.seconds(1), container);
        slide.setFromY(30);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void showNotification(String message) {
        Label notification = new Label(message);
        notification.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: white; -fx-padding: 12 25; -fx-background-radius: 20; -fx-font-size: 16;");
        notification.setEffect(new DropShadow(10, Color.web(COLOR_PRIMARY)));

        StackPane root = (StackPane) primaryStage.getScene().getRoot();
        root.getChildren().add(notification);

        StackPane.setAlignment(notification, Pos.BOTTOM_CENTER);
        StackPane.setMargin(notification, new Insets(0, 0, 40, 0));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notification);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), notification);
        fadeOut.setDelay(Duration.seconds(2));
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> root.getChildren().remove(notification));

        new SequentialTransition(fadeIn, fadeOut).play();
    }

    public void showSignInScene() {
        VBox loginBox = new VBox(20);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(40));

        Label title = new Label("Connexion");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.web(COLOR_PRIMARY));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");

        Button loginBtn = new Button("Se connecter");
        loginBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: white;");
        loginBtn.setOnAction(e -> showNotification("Connexion réussie !"));

        Button backBtn = new Button("Retour");
        backBtn.setStyle("-fx-background-color: gray; -fx-text-fill: white;");
        backBtn.setOnAction(e -> setupMainMenu());

        loginBox.getChildren().addAll(title, usernameField, passwordField, loginBtn, backBtn);

        StackPane root = new StackPane(loginBox);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a2a6c, #b21f1f);");

        Scene loginScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(loginScene);
    }

    public void ShowSignUpScene(){
        VBox loginBox = new VBox(20);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(40));

        Label title = new Label("SIGN UP");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(Color.web(COLOR_PRIMARY));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your Password");

        PasswordField passwordField1 = new PasswordField();
        passwordField1.setPromptText("Confirm your Password");

        Button loginBtn = new Button("Sign Up");
        loginBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: white;");
        loginBtn.setOnAction(e -> showNotification("Connexion réussie !"));

        Button backBtn = new Button("Return");
        backBtn.setStyle("-fx-background-color: gray; -fx-text-fill: white;");
        backBtn.setOnAction(e -> setupMainMenu());

        loginBox.getChildren().addAll(title, usernameField, passwordField,passwordField1, loginBtn, backBtn);

        StackPane root = new StackPane(loginBox);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a2a6c, #b21f1f);");

        Scene loginScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(loginScene);


    }

    public static void main(String[] args) {
        launch(args);
    }
}