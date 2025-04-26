package design;

import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;


public class HUD {
    private Stage primaryStage;
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    // Éléments du jeu

    public ImageView player;
    public final CopyOnWriteArrayList<ImageView> enemies = new CopyOnWriteArrayList<>();
    public final CopyOnWriteArrayList<Animation> activeAnimations = new CopyOnWriteArrayList<>();
    public int score = 0;
    public int lives = 3;
    public Pane gamepane;  // Conteneur principal du jeu

    // Dans la section Éléments du jeu

    public BorderPane hudContainer;
    public Label scoreLabel;
    public Label levelLabel;
    public ProgressBar healthBar;
    public Label healthLabel;
    public Label ammoLabel;
    public Label notificationLabel;

    // Variables de contrôle du mouvement
    public volatile boolean movingLeft = false;
    public volatile boolean movingRight = false;
    public volatile boolean movingUp = false;
    public volatile boolean movingDown = false;
    public volatile boolean firing = false;
    public final double PLAYER_SPEED = 5.0;
    public final double LASER_SPEED = 10.0;


    // Threads
    private ExecutorService gameExecutor = Executors.newFixedThreadPool(3);
    private volatile boolean gameRunning = false;

    // Chat
    public Socket socket;
    public PrintWriter out;
    public TextArea chatArea;

    public void initializeHUD() {


        // Top HUD (score, level)
        HBox topHUD = new HBox(20);
        topHUD.setPadding(new Insets(10));
        topHUD.setAlignment(Pos.CENTER);

        scoreLabel = createHUDLabel("SCORE: 0");
        levelLabel = createHUDLabel("NIVEAU: 1");

        topHUD.getChildren().addAll(scoreLabel, levelLabel);

        // Bottom HUD (health, ammo)
        HBox bottomHUD = new HBox(20);
        bottomHUD.setPadding(new Insets(10));
        bottomHUD.setAlignment(Pos.CENTER);

        // Barre de santé
        VBox healthBox = new VBox(5);
        healthBox.setAlignment(Pos.CENTER_LEFT);

        Label healthTitle = createHUDLabel("SANTÉ");
        healthTitle.setFont(Font.font("Orbitron", FontWeight.BOLD, 12));

        healthBar = new ProgressBar(1.0);
        healthBar.setPrefWidth(150);
        healthBar.setStyle(
                "-fx-accent: linear-gradient(to right, #ff0000, #00ff00);" +
                        "-fx-control-inner-background: rgba(0, 0, 0, 0.5);" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;"
        );

        healthLabel = createHUDLabel("100%");
        healthLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 12));

        healthBox.getChildren().addAll(healthTitle, healthBar, healthLabel);

        // Munitions
        VBox ammoBox = new VBox(5);
        ammoBox.setAlignment(Pos.CENTER_RIGHT);

        Label ammoTitle = createHUDLabel("MUNITIONS");
        ammoTitle.setFont(Font.font("Orbitron", FontWeight.BOLD, 12));

        ammoLabel = createHUDLabel("100");
        ammoLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 18));

        ammoBox.getChildren().addAll(ammoTitle, ammoLabel);

        bottomHUD.getChildren().addAll(healthBox, ammoBox);

        // Notification au centre
        notificationLabel = new Label("");
        notificationLabel.setFont(Font.font("Orbitron", FontWeight.BOLD, 24));
        notificationLabel.setTextFill(Color.WHITE);
        notificationLabel.setOpacity(0);
        StackPane centerPane = new StackPane(notificationLabel);

        // Assemblage du HUD
        hudContainer.setTop(topHUD);
        hudContainer.setBottom(bottomHUD);
        hudContainer.setCenter(centerPane);

        // Style global
        hudContainer.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.5),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }
    public Label createHUDLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Orbitron", FontWeight.BOLD, 16));
        label.setTextFill(Color.WHITE);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.BLACK);
        shadow.setRadius(2);
        shadow.setOffsetX(1);
        shadow.setOffsetY(1);
        label.setEffect(shadow);

        return label;
    }
    public void updateScore(int score) {
        this.score = score;
        scoreLabel.setText("SCORE: " + score);

        // Animation pour le score
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), scoreLabel);
        scaleUp.setToX(1.2);
        scaleUp.setToY(1.2);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), scoreLabel);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        scaleUp.setOnFinished(e -> scaleDown.play());
        scaleUp.play();
    }
    public void updateHUD() {

        if (scoreLabel != null) scoreLabel.setText("SCORE: " + score);

    }
    public void HUD() {
        // this.uiFactory = new UIFactory();
        this.hudContainer = new BorderPane();
        initializeHUD();
    }
    public void setupHUD() {
        hudContainer = new BorderPane();

        // Top HUD
        HBox topHUD = new HBox(20);
        scoreLabel = createHUDLabel("SCORE: 0");
        levelLabel = createHUDLabel("NIVEAU: 1");
        topHUD.getChildren().addAll(scoreLabel, levelLabel);

        // Bottom HUD
        HBox bottomHUD = new HBox(20);
        healthBar = new ProgressBar(1.0);
        healthLabel = createHUDLabel("100%");
        ammoLabel = createHUDLabel("100");
        bottomHUD.getChildren().addAll(healthBar, healthLabel, ammoLabel);

        hudContainer.setTop(topHUD);
        hudContainer.setBottom(bottomHUD);

        gamepane.getChildren().add(hudContainer);
    }
}
