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
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class JeuDeTir extends Application {
    private Stage primaryStage;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private MediaPlayer mediaPlayer;

    // Palette de couleurs cohérente
    private static final String COLOR_PRIMARY = "#2E86AB";  // Bleu aéronautique
    private static final String COLOR_SECONDARY = "#F18F01"; // Orange warning
    private static final String COLOR_ACCENT = "#A23B72";   // Violet high-tech
    private static final String COLOR_DANGER = "#C73E1D";   // Rouge alerte

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Jet Fighters - Édition Premium");
        setupMainMenu();
        playBackgroundMusic();
    }

    private void playBackgroundMusic() {
        try {
            // Essaye plusieurs noms de fichiers courants
            String[] possibleMusicPaths = {
                    "/background_music.mp3",
                    "/music.mp3",
                    "/soundtrack.mp3",
                    "/audio.mp3"
            };

            for (String path : possibleMusicPaths) {
                try {
                    InputStream is = getClass().getResourceAsStream(path);
                    if (is != null) {
                        String musicFile = getClass().getResource(path).toExternalForm(); // Changement important ici
                        Media sound = new Media(musicFile);
                        mediaPlayer = new MediaPlayer(sound);
                        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                        mediaPlayer.setVolume(0.3);
                        mediaPlayer.play();
                        return; // Sort après avoir trouvé un fichier valide
                    }
                } catch (Exception e) {
                    System.err.println("Erreur avec le fichier " + path + ": " + e.getMessage());
                }
            }
            System.err.println("Aucun fichier audio valide trouvé");
        } catch (Exception e) {
            System.err.println("Échec critique de l'initialisation audio: " + e.getMessage());
            // L'application continue sans musique
        }
    }
    private void setupMainMenu() {
        StackPane root = new StackPane();

        // Chargement de l'image de fond
        ImageView background = loadBackgroundImage();
        root.getChildren().add(background);

        // Overlay semi-transparent amélioré
        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.3, Color.rgb(0, 0, 0, 0.4)),
                new Stop(1, Color.rgb(0, 0, 0, 0.7))
        ));
        root.getChildren().add(overlay);

        // Conteneur principal amélioré
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
            String[] possibleImagePaths = {
                    "/background.jpg",
                    "/background.png",
                    "/img.jpg",
                    "/img.png",
                    "/image.jpg",
                    "/image.png"
            };

            for (String path : possibleImagePaths) {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    Image image = new Image(is);
                    ImageView imageView = new ImageView(image);
                    setupBackgroundImage(imageView);
                    return imageView;
                }
            }
            throw new RuntimeException("Aucune image de fond trouvée");
        } catch (Exception e) {
            System.err.println("Erreur de chargement: " + e.getMessage());
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
        rect.setFill(new RadialGradient(
                0, 0, 0.5, 0.5, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(0.5, Color.web("#b21f1f")),
                new Stop(1, Color.web("#fdbb2d"))
        ));
        return new ImageView(rect.snapshot(null, null));
    }

    private VBox createMainContainer() {
        VBox container = new VBox(25);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40, 60, 40, 60));
        container.setMaxWidth(750);

        Label title = createTitleLabel();
        Label subtitle = createSubtitleLabel();
        GridPane loginSection = createLoginSection();
        HBox actionButtons = createActionButtons();

        container.getChildren().addAll(title, subtitle, loginSection, actionButtons);
        animateMenuEntrance(container);

        return container;
    }

    private Label createTitleLabel() {
        Label label = new Label("JET FIGHTERS");
        label.setFont(Font.font("Agency FB", FontWeight.EXTRA_BOLD, 74));
        label.setTextFill(Color.WHITE);

        DropShadow glow = new DropShadow(15, Color.web(COLOR_PRIMARY));
        glow.setSpread(0.3);

        label.setEffect(new Blend(
                BlendMode.SCREEN,
                new Glow(0.8),
                glow
        ));

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

    private GridPane createLoginSection() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(25, 30, 25, 30));

        grid.setStyle("-fx-background-color: rgba(20, 40, 60, 0.5); " +
                "-fx-background-radius: 10; " +
                "-fx-border-radius: 10; " +
                "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                "-fx-border-width: 1;");

        Label title = new Label("IDENTIFICATION PILOTE");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(COLOR_PRIMARY));
        GridPane.setColumnSpan(title, 2);
        grid.add(title, 0, 0);

        addFormField(grid, "Nom:", 1);
        addFormField(grid, "Code:", 2);

        return grid;
    }

    private void addFormField(GridPane grid, String labelText, int row) {
        Label label = new Label(labelText);
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        label.setTextFill(Color.WHITE);

        TextField field = row == 2 ? new PasswordField() : new TextField();
        field.setPromptText("Entrez votre " + labelText.toLowerCase());

        field.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16; " +
                "-fx-padding: 10 15; " +
                "-fx-background-radius: 5; " +
                "-fx-border-radius: 5; " +
                "-fx-border-color: " + COLOR_PRIMARY + "; " +
                "-fx-border-width: 1.5;");

        field.setPrefWidth(350);

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle() + "-fx-border-color: " + COLOR_SECONDARY + ";");
            } else {
                field.setStyle(field.getStyle().replace("-fx-border-color:.*;",
                        "-fx-border-color: " + COLOR_PRIMARY + ";"));
            }
        });

        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    private HBox createActionButtons() {
        HBox buttonBox = new HBox(25);
        buttonBox.setAlignment(Pos.CENTER);

        Button startBtn = createActionButton("Start", COLOR_PRIMARY);
        Button signUpBtn = createActionButton("SIGN UP", COLOR_ACCENT);
        Button signInBtn = createActionButton("SIGN IN", COLOR_SECONDARY);
        Button quitBtn = createActionButton("QUITTER", COLOR_DANGER);

        startBtn.setOnAction(e -> showNotification("Initialisation du système..."));
        signUpBtn.setOnAction(e -> showNotification("Création de compte..."));
        signInBtn.setOnAction(e -> showNotification("Connexion en cours..."));
        quitBtn.setOnAction(e -> primaryStage.close());

        buttonBox.getChildren().addAll(startBtn, signUpBtn, signInBtn, quitBtn);
        return buttonBox;
    }

    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 18; " +
                "-fx-padding: 12 30; " +
                "-fx-background-radius: 30; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");

        btn.setOnMouseEntered(e -> {
            btn.setStyle(btn.getStyle().replace(color, darkenColor(color, 0.2)) +
                    "-fx-effect: dropshadow(gaussian, " + color + ", 15, 0.5, 0, 0);");
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(btn.getStyle().replace(darkenColor(color, 0.2), color)
                    .replace("-fx-effect:.*;", "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);"));
        });

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

        ParallelTransition combo = new ParallelTransition(fade, slide);
        combo.play();
    }

    private void showNotification(String message) {
        Label notification = new Label(message);
        notification.setStyle("-fx-background-color: rgba(0,0,0,0.8); " +
                "-fx-text-fill: white; " +
                "-fx-padding: 12 25; " +
                "-fx-background-radius: 20; " +
                "-fx-font-size: 16;");
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

        SequentialTransition sequence = new SequentialTransition(fadeIn, fadeOut);
        sequence.play();
    }

    @Override
    public void stop() {
        // Arrête la musique quand l'application se ferme
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}