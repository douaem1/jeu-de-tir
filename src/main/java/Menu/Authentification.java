package Menu;
import Game.GameManager;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import DAO.users;
import javafx.stage.Stage;
import java.util.Map;
import design.design;
import design.animation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * This class handles the user authentication process, including sign-in and sign-up functionalities.
 * It provides a graphical user interface for users to input their credentials and interact with the application.
 */

public class Authentification {

    public static int WINDOW_WIDTH = 1200;
    public static int WINDOW_HEIGHT = 800;
    public String[] BACKGROUND_PATHS = {"/img.jpg", "/background.jpg", "/backround.jpg"};
    public String[] FONT_FAMILIES = {"Agency FB", "Arial", "Bank Gothic"};
    public Map<String, Color> COLORS = Map.of(
            "PRIMARY", Color.web("#2E86AB"),
            "SECONDARY", Color.web("#F18F01"),
            "DANGER", Color.web("#C73E1D"),
            "LIGHT", Color.web("#F5F5F5"),
            "ACCENT", Color.web("#A23B72"),
            "DARK", Color.web("#1A1A2E")
    );
    private Stage primaryStage;
    private GameManager gameManager;

    public Authentification(Stage primaryStage) {
        if (primaryStage == null) {
            throw new IllegalArgumentException("primaryStage cannot be null");
        }
        this.primaryStage = primaryStage;
        this.gameManager = new GameManager();
    }

    public void showSignInScene() {
        VBox loginBox = new VBox(20);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(40));
        loginBox.setMaxWidth(500);

        loginBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.7); -fx-background-radius: 15;");
        design design = new design();
        animation animation = new animation();
        StackPane root = new StackPane();
        ImageView background = design.loadBestBackground();
        design.setupBackgroundImage(background);
        design.animateBackground(background);
        root.getChildren().add(background);
        Scene loginScene = new Scene(root, 1200, 800);
        primaryStage.setScene(loginScene);
        Rectangle overlay = design.createOverlay();
        root.getChildren().add(overlay);

        Label title = new Label("SIGN IN");
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 46));
        title.setTextFill(COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, COLORS.get("PRIMARY"));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);
        title.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animation.animateTextGlow(title, glow);

        TextField usernameField = animation.createStylizedTextField("Username");
        PasswordField passwordField = animation.createStylizedPasswordField("Password");

        Button loginBtn = animation.createActionButton("SIGN IN", "PRIMARY");
        loginBtn.setPrefWidth(200);

        Button backBtn = animation.createActionButton("Return", "DARK");
        backBtn.setPrefWidth(200);

        // Correction pour le bouton de retour
        backBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(backBtn);
            MenuManager menuManager = new MenuManager(primaryStage);
            menuManager.returnToMenu();
        });

        loginBox.getChildren().addAll(title, usernameField, passwordField, loginBtn, backBtn);

        loginBox.setOpacity(0);
        loginBox.setTranslateY(20);
        root.getChildren().add(loginBox);

        animation.animateFormEntrance(loginBox);

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            users user = new users();
            boolean isValid = user.verifyUser(username, password);
            MenuManager menuManager = new MenuManager(primaryStage);
            if (isValid) {
                menuManager.showNotification("Sign in successful!");
                PlayerSelectionInterface selectionInterface = new PlayerSelectionInterface(primaryStage);
                selectionInterface.showSelectionInterface();
            } else {
                menuManager.showNotification("Incorrect username or password or inexistant account (Sign up first)");
            }
        });
    }

    public void showSignUpScene() {
        VBox signupBox = new VBox(20);
        signupBox.setAlignment(Pos.CENTER);
        signupBox.setPadding(new Insets(40));
        signupBox.setMaxWidth(500);
        signupBox.setStyle("-fx-background-color: rgba(10, 10, 30, 0.7); -fx-background-radius: 15;");
        design design = new design();
        animation animation = new animation();
        StackPane root = new StackPane();
        ImageView background = design.loadBestBackground();
        design.setupBackgroundImage(background);
        design.animateBackground(background);
        root.getChildren().add(background);

        Rectangle overlay = design.createOverlay();
        root.getChildren().add(overlay);

        Label title = new Label("SIGN UP");
        title.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 46));
        title.setTextFill(COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, COLORS.get("ACCENT"));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);
        title.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animation.animateTextGlow(title, glow);

        TextField usernameField = animation.createStylizedTextField("Enter your Username");
        PasswordField passwordField = animation.createStylizedPasswordField("Enter your Password");
        PasswordField passwordField1 = animation.createStylizedPasswordField("Confirm your Password");
        Button signupBtn = animation.createActionButton("SIGN UP", "ACCENT");
        signupBtn.setPrefWidth(200);
        MenuManager menuManager = new MenuManager(primaryStage);

        // Récupération Input
        signupBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(signupBtn);

            String username = usernameField.getText();
            String password = passwordField.getText();
            String confirmPassword = passwordField1.getText();
            if (username.isEmpty() || confirmPassword.isEmpty() || password.isEmpty()) {
                menuManager.showNotification("Please fill in all fields.");
                return;
            }
            if (!password.equals(confirmPassword)) {
                menuManager.showNotification("Passwords do not match!");
                return;
            }
            users newUser = new users(username, password);
            if (newUser.userExists(username)) {
                menuManager.showNotification("Username already exists. Please choose another one.");
                return;
            } else {
                System.out.println("Tentative d'inscription avec :" + username + " et " + password);

                newUser.addUser(newUser);

                menuManager.showNotification("Account created successfully!");
                PlayerSelectionInterface selectionInterface = new PlayerSelectionInterface(primaryStage);
                selectionInterface.showSelectionInterface();
            }
        });

        Button backBtn = animation.createActionButton("Return", "DARK");
        backBtn.setPrefWidth(200);

        // Correction pour le bouton de retour
        backBtn.setOnAction(e -> {
            animation.playButtonPressAnimation(backBtn);
           // MenuManager menuManager = new MenuManager(primaryStage);
            menuManager.returnToMenu();
        });

        signupBox.getChildren().addAll(title, usernameField, passwordField, passwordField1, signupBtn, backBtn);

        signupBox.setOpacity(0);
        signupBox.setTranslateY(20);
        root.getChildren().add(signupBox);

        Scene signupScene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setScene(signupScene);

        animation.animateFormEntrance(signupBox);
    }
}