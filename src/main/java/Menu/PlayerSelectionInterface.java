package Menu;
import Game.GameManager;
import design.animation;
import design.design;
import javafx.animation.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.Map;

public class PlayerSelectionInterface {

        private Stage primaryStage;
        private GameManager gameManager;
        private String selectedAircraft = null;
        private String selectedDifficulty = null;
        private animation animation;
        private design design;
        private MenuManager menuManager;

        // Constants
        public static final int WINDOW_WIDTH = 1200;
        public static final int WINDOW_HEIGHT = 800;
        private final String[] FONT_FAMILIES = {"Agency FB", "Arial", "Bank Gothic"};
        private final Map<String, Color> COLORS = Map.of(
                "PRIMARY", Color.web("#2E86AB"),
                "SECONDARY", Color.web("#F18F01"),
                "DANGER", Color.web("#C73E1D"),
                "LIGHT", Color.web("#F5F5F5"),
                "ACCENT", Color.web("#A23B72"),
                "DARK", Color.web("#1A1A2E")
        );

        // Aircraft data
        private final String[] AIRCRAFT_NAMES = {"F-22 Raptor", "Eurofighter Typhoon", "Sukhoi Su-57"};
        private final String[] AIRCRAFT_IMAGES = {"/Spaceship_04_RED.png", "/Spaceship_05_BLUE.png", "/Spaceship_02_ORANGE.png"};
        private final String[] AIRCRAFT_DESCRIPTIONS = {
                "Top speed: Mach 2.25\nManeuverability: Excellent\nSpecial: Stealth technology",
                "Top speed: Mach 2.0\nManeuverability: Superior\nSpecial: Advanced avionics",
                "Top speed: Mach 2.0\nManeuverability: High\nSpecial: Super-maneuverability"
        };

        // UI State Properties
        private final SimpleStringProperty selectedAircraftProperty = new SimpleStringProperty(null);
        private final SimpleStringProperty selectedDifficultyProperty = new SimpleStringProperty(null);

        public PlayerSelectionInterface(Stage primaryStage) {
            this.primaryStage = primaryStage;
            this.gameManager = new GameManager();
            this.gameManager.setPrimaryStage(primaryStage);
            this.animation = new animation();
            this.design = new design();
            this.menuManager = new MenuManager(primaryStage);
        }

        public void setGameManager(GameManager gameManager) {
            this.gameManager = gameManager;
        }

        public void showSelectionInterface() {
            StackPane root = new StackPane();

            // Background
            ImageView background = design.loadBestBackground();
            design.setupBackgroundImage(background);
            design.animateBackground(background);
            root.getChildren().add(background);

            // Dark overlay
            Rectangle overlay = design.createOverlay();
            root.getChildren().add(overlay);

            // Main container
            VBox mainContainer = new VBox(30);
            mainContainer.setAlignment(Pos.TOP_CENTER);
            mainContainer.setPadding(new Insets(40));
            mainContainer.setMaxWidth(1000);
            //Titre
            Label title1 = new Label("WELCOME !");
            Label title = new Label("CHOOSE YOUR FIGHTER");
            title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 54));
            title1.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 54));
            title.setTextFill(COLORS.get("LIGHT"));
            title1.setTextFill(COLORS.get("LIGHT"));

            DropShadow glow = new DropShadow(15, COLORS.get("PRIMARY"));
            glow.setSpread(0.3);
            Bloom bloom = new Bloom(0.3);
            title.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
            title1.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
            animation.animateTextGlow(title, glow);
            animation.animateTextGlow(title1, glow);

            // Aircraft selection
            HBox aircraftContainer = createAircraftSelection();

            // Difficulty selection
            VBox difficultyContainer = createDifficultySelection();

            // Start button
            Button startGameBtn = animation.createActionButton("START MISSION", "PRIMARY");
            startGameBtn.setPrefWidth(300);
            startGameBtn.setPrefHeight(60);
            startGameBtn.setDisable(true); // Initially disabled

            // Return button
            Button returnBtn = animation.createActionButton("RETURN", "DARK");
            returnBtn.setPrefWidth(200);

            // Button Actions
            startGameBtn.setOnAction(e -> {
                if (selectedAircraft != null && selectedDifficulty != null) {
                    animation.playButtonPressAnimation(startGameBtn);
                    startGame();
                }
            });

            returnBtn.setOnAction(e -> {
                animation.playButtonPressAnimation(returnBtn);
                menuManager.returnToMenu();
            });

            // Enable Start Button if selections are made
            Runnable updateStartButton = () -> {
                startGameBtn.setDisable(selectedAircraft == null || selectedDifficulty == null);
            };
            selectedAircraftProperty.addListener((obs, oldVal, newVal) -> updateStartButton.run());
            selectedDifficultyProperty.addListener((obs, oldVal, newVal) -> updateStartButton.run());

            // Build the main container
            mainContainer.getChildren().addAll(title1,title, aircraftContainer, difficultyContainer, startGameBtn, returnBtn);

            root.getChildren().add(mainContainer);

            // Animation
            mainContainer.setOpacity(0);
            mainContainer.setTranslateY(20);
            animateEntrance(mainContainer);

            // Scene
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Fighter Selection - Jet Fighters");
        }

        private HBox createAircraftSelection() {
            HBox container = new HBox(20);
            container.setAlignment(Pos.CENTER);

            for (int i = 0; i < AIRCRAFT_NAMES.length; i++) {
                VBox aircraftBox = createAircraftBox(i);
                container.getChildren().add(aircraftBox);
            }

            return container;
        }

        private VBox createAircraftBox(int index) {
            VBox box = new VBox(15);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(20));
            box.setMaxWidth(300);
            box.setStyle("-fx-background-color: rgba(10, 10, 30, 0.5); -fx-background-radius: 10;");

            // Image
            ImageView aircraftImage;
            try {
                InputStream is = getClass().getResourceAsStream(AIRCRAFT_IMAGES[index]);
                if (is == null) {
                    System.err.println("Image not found: " + AIRCRAFT_IMAGES[index]);
                    Rectangle placeholder = new Rectangle(240, 160, COLORS.get("PRIMARY"));
                    box.getChildren().add(placeholder);
                } else {
                    aircraftImage = new ImageView(new Image(is));
                    aircraftImage.setFitWidth(240);
                    aircraftImage.setFitHeight(160);
                    box.getChildren().add(aircraftImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Name
            Label nameLabel = new Label(AIRCRAFT_NAMES[index]);
            nameLabel.setTextFill(COLORS.get("LIGHT"));
            nameLabel.setFont(Font.font(FONT_FAMILIES[1], FontWeight.BOLD, 20));

            // Description
            Label descriptionLabel = new Label(AIRCRAFT_DESCRIPTIONS[index]);
            descriptionLabel.setTextFill(COLORS.get("LIGHT"));
            descriptionLabel.setFont(Font.font(FONT_FAMILIES[1], 14));
            descriptionLabel.setWrapText(true);
            descriptionLabel.setMaxWidth(260);

            box.getChildren().addAll(nameLabel, descriptionLabel);

            // Hover effect
            box.setOnMouseEntered(e -> {
                box.setStyle("-fx-background-color: rgba(20, 20, 50, 0.7); -fx-background-radius: 10;");
                box.setScaleX(1.05);
                box.setScaleY(1.05);
                box.setCursor(javafx.scene.Cursor.HAND);
            });

            box.setOnMouseExited(e -> {
                if (selectedAircraft != null && selectedAircraft.equals(AIRCRAFT_NAMES[index])) {
                    box.setStyle("-fx-background-color: rgba(46, 134, 171, 0.7); -fx-background-radius: 10;");
                } else {
                    box.setStyle("-fx-background-color: rgba(10, 10, 30, 0.5); -fx-background-radius: 10;");
                }
                box.setScaleX(1);
                box.setScaleY(1);
                box.setCursor(javafx.scene.Cursor.DEFAULT);
            });

            // Selection action
            box.setOnMouseClicked((MouseEvent e) -> selectAircraft(index, box));

            return box;
        }

        private void selectAircraft(int index, VBox selectedBox) {
            selectedAircraft = AIRCRAFT_NAMES[index];  // Mettez à jour l'avion sélectionné

            selectedAircraftProperty.set(selectedAircraft);

            // Mettre à jour l'interface avec la sélection de l'avion
            for (Node node : ((HBox) selectedBox.getParent()).getChildren()) {
                if (node instanceof VBox) {
                    node.setStyle("-fx-background-color: rgba(10, 10, 30, 0.5); -fx-background-radius: 10;");
                }
            }

            selectedBox.setStyle("-fx-background-color: rgba(46, 134, 171, 0.7); -fx-background-radius: 10;");
        }


        private VBox createDifficultySelection() {
            VBox container = new VBox(10);
            container.setAlignment(Pos.CENTER);

            Label label = new Label("Select Difficulty");
            label.setFont(Font.font(FONT_FAMILIES[1], FontWeight.BOLD, 28));
            label.setTextFill(COLORS.get("LIGHT"));

            HBox difficultyButtons = new HBox(20);
            difficultyButtons.setAlignment(Pos.CENTER);

            String[] difficulties = {"EASY", "MEDIUM", "HARD"};

            for (String difficulty : difficulties) {
                Button btn = animation.createActionButton(difficulty, "SECONDARY");
                btn.setPrefWidth(150);
                btn.setOnAction(e -> selectDifficulty(difficulty));
                difficultyButtons.getChildren().add(btn);
            }

            container.getChildren().addAll(label, difficultyButtons);

            return container;
        }

        private void selectDifficulty(String difficulty) {
            selectedDifficulty = difficulty;
            selectedDifficultyProperty.set(difficulty);
        }

        private void animateEntrance(Node node) {
            FadeTransition ft = new FadeTransition(Duration.seconds(1), node);
            ft.setFromValue(0);
            ft.setToValue(1);

            TranslateTransition tt = new TranslateTransition(Duration.seconds(1), node);
            tt.setFromY(20);
            tt.setToY(0);

            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.play();
        }

        private void startGame() {
            // Utiliser l'avion sélectionné avant de démarrer le jeu
            gameManager.startGame(selectedAircraft);  // Passer l'avion sélectionné au GameManager
        }
    }

