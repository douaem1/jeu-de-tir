package chat_Client_Serveur;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;


import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class DuelMultiplayerGame extends Application {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;
    private static final int PLAYER_SIZE = 80;
    private static final int PROJECTILE_SPEED = 10;

    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private boolean isHost;

    private String playerName;
    private String opponentName;
    private String selectedAircraft;

    private double playerX, playerY;
    private double opponentX, opponentY;

    private Image playerImage;
    private Image opponentImage;
    private Image backgroundImage;
    private Image projectileImage;

    private CopyOnWriteArrayList<Projectile> myProjectiles = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Projectile> opponentProjectiles = new CopyOnWriteArrayList<>();

    private int myScore = 0;
    private int opponentScore = 0;
    private int myLives = 3;
    private int opponentLives = 3;

    private boolean gameRunning = true;
    private Set<KeyCode> pressedKeys = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        try {
            // Configuration initiale
            Pane root = new Pane();
            Canvas canvas = new Canvas(WIDTH, HEIGHT);
            root.getChildren().add(canvas);

            Scene scene = new Scene(root, WIDTH, HEIGHT);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Duel de Tir 1v1");

            // Chargement des images
            loadImages();

            // Configuration réseau via Radmin VPN
            setupNetwork();

            // Configuration des contrôles
            setupControls(scene);

            // Lancement de la boucle de jeu
            new AnimationTimer() {
                @Override
                public void handle(long now) {
                    update();
                    render(canvas.getGraphicsContext2D());
                }
            }.start();

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadImages() {
        try {
            // Charger les images selon l'avion sélectionné
            playerImage = new Image(getClass().getResourceAsStream(
                    "/images/" + selectedAircraft + ".png"));

            // Image par défaut pour l'adversaire (sera remplacée après échange réseau)
            opponentImage = new Image(getClass().getResourceAsStream("/images/default_enemy.png"));

            backgroundImage = new Image(getClass().getResourceAsStream("/images/space_bg.jpg"));
            projectileImage = new Image(getClass().getResourceAsStream("/images/projectile.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupNetwork() throws IOException, ClassNotFoundException {
        // Configuration Radmin VPN - l'hôte écoute sur son IP Radmin, le client se connecte
        String hostIP = JOptionPane.showInputDialog("Entrez l'IP Radmin VPN de l'hôte:");
        int port = 5555;

        if (hostIP.isEmpty()) {
            // Mode hôte
            isHost = true;
            ServerSocket serverSocket = new ServerSocket(port);
            socket = serverSocket.accept();
            serverSocket.close();
        } else {
            // Mode client
            socket = new Socket(hostIP, port);
        }

        // Échange d'informations initiales
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());

        if (isHost) {
            // Envoyer nos infos et recevoir celles de l'adversaire
            outputStream.writeObject(playerName);
            outputStream.writeObject(selectedAircraft);
            outputStream.flush();

            opponentName = (String) inputStream.readObject();
            String opponentAircraft = (String) inputStream.readObject();

            // Charger l'image de l'avion adverse
            opponentImage = new Image(getClass().getResourceAsStream(
                    "/images/" + opponentAircraft + ".png"));
        } else {
            // Recevoir les infos de l'hôte puis envoyer les nôtres
            opponentName = (String) inputStream.readObject();
            String opponentAircraft = (String) inputStream.readObject();

            outputStream.writeObject(playerName);
            outputStream.writeObject(selectedAircraft);
            outputStream.flush();

            // Charger l'image de l'avion adverse
            opponentImage = new Image(getClass().getResourceAsStream(
                    "/images/" + opponentAircraft + ".png"));
        }

        // Position initiale des joueurs
        if (isHost) {
            playerX = WIDTH / 4;
            playerY = HEIGHT - PLAYER_SIZE - 20;
            opponentX = 3 * WIDTH / 4;
            opponentY = 20;
        } else {
            playerX = 3 * WIDTH / 4;
            playerY = 20;
            opponentX = WIDTH / 4;
            opponentY = HEIGHT - PLAYER_SIZE - 20;
        }

        // Démarrer le thread de réception réseau
        new Thread(this::receiveNetworkData).start();
    }

    private void setupControls(Scene scene) {
        scene.setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());
            handleMovement();
        });

        scene.setOnKeyReleased(e -> {
            pressedKeys.remove(e.getCode());
        });
    }

    private void handleMovement() {
        double speed = 5;
        boolean moved = false;

        if (pressedKeys.contains(KeyCode.LEFT)) {
            playerX = Math.max(0, playerX - speed);
            moved = true;
        }
        if (pressedKeys.contains(KeyCode.RIGHT)) {
            playerX = Math.min(WIDTH - PLAYER_SIZE, playerX + speed);
            moved = true;
        }
        if (pressedKeys.contains(KeyCode.UP)) {
            playerY = Math.max(0, playerY - speed);
            moved = true;
        }
        if (pressedKeys.contains(KeyCode.DOWN)) {
            playerY = Math.min(HEIGHT - PLAYER_SIZE, playerY + speed);
            moved = true;
        }
        if (pressedKeys.contains(KeyCode.SPACE)) {
            fireProjectile();
        }

        if (moved) {
            sendNetworkData(new PositionUpdate(playerX, playerY));
        }
    }

    private void fireProjectile() {
        double projectileX = playerX + PLAYER_SIZE / 2 - 5;
        double projectileY = isHost ? playerY : playerY + PLAYER_SIZE;

        Projectile projectile = new Projectile(projectileX, projectileY,
                isHost ? -PROJECTILE_SPEED : PROJECTILE_SPEED);

        myProjectiles.add(projectile);
        sendNetworkData(new ProjectileFired(projectileX, projectileY));
    }

    private void update() {
        if (!gameRunning) return;

        // Mise à jour des projectiles
        updateProjectiles(myProjectiles);
        updateProjectiles(opponentProjectiles);

        // Détection des collisions
        checkCollisions();

        // Vérifier si la partie est terminée
        if (myLives <= 0 || opponentLives <= 0) {
            gameRunning = false;
            showGameOver();
        }
    }

    private void updateProjectiles(CopyOnWriteArrayList<Projectile> projectiles) {
        projectiles.removeIf(p -> p.y < 0 || p.y > HEIGHT);
        projectiles.forEach(Projectile::update);
    }

    private void checkCollisions() {
        // Vérifier si nos projectiles touchent l'adversaire
        myProjectiles.removeIf(p -> {
            if (p.getBounds().intersects(opponentX, opponentY, PLAYER_SIZE, PLAYER_SIZE)) {
                opponentLives--;
                myScore += 10;
                return true;
            }
            return false;
        });

        // Vérifier si les projectiles adverses nous touchent
        opponentProjectiles.removeIf(p -> {
            if (p.getBounds().intersects(playerX, playerY, PLAYER_SIZE, PLAYER_SIZE)) {
                myLives--;
                opponentScore += 10;
                return true;
            }
            return false;
        });
    }

    private void render(GraphicsContext gc) {
        // Dessiner l'arrière-plan
        gc.drawImage(backgroundImage, 0, 0, WIDTH, HEIGHT);

        // Dessiner les joueurs
        gc.drawImage(playerImage, playerX, playerY, PLAYER_SIZE, PLAYER_SIZE);
        gc.drawImage(opponentImage, opponentX, opponentY, PLAYER_SIZE, PLAYER_SIZE);

        // Dessiner les projectiles
        myProjectiles.forEach(p -> p.render(gc, projectileImage));
        opponentProjectiles.forEach(p -> p.render(gc, projectileImage));

        // Afficher les scores et vies
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText(playerName + ": " + myScore + " pts | Vies: " + myLives, 20, 30);
        gc.fillText(opponentName + ": " + opponentScore + " pts | Vies: " + opponentLives,
                WIDTH - 250, 30);

        // Afficher "Game Over" si la partie est terminée
        if (!gameRunning) {
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillText("GAME OVER - " + (myLives > 0 ? "VOUS AVEZ GAGNÉ!" : opponentName + " A GAGNÉ!"),
                    WIDTH / 2 - 100, HEIGHT / 2);
        }
    }

    private void showGameOver() {
        // Vous pouvez implémenter une boîte de dialogue plus élaborée ici
        System.out.println("Game Over! " + (myLives > 0 ? "Vous avez gagné!" : opponentName + " a gagné!"));
    }

    private void receiveNetworkData() {
        try {
            while (gameRunning) {
                Object data = inputStream.readObject();

                if (data instanceof PositionUpdate) {
                    PositionUpdate update = (PositionUpdate) data;
                    opponentX = update.x;
                    opponentY = update.y;
                }
                else if (data instanceof ProjectileFired) {
                    ProjectileFired fire = (ProjectileFired) data;
                    double speed = isHost ? PROJECTILE_SPEED : -PROJECTILE_SPEED;
                    opponentProjectiles.add(new Projectile(fire.x, fire.y, speed));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            gameRunning = false;
        }
    }

    private void sendNetworkData(Object data) {
        try {
            outputStream.writeObject(data);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            gameRunning = false;
        }
    }

    // Classes pour la communication réseau
    private static class PositionUpdate implements Serializable {
        public double x, y;
        public PositionUpdate(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class ProjectileFired implements Serializable {
        public double x, y;
        public ProjectileFired(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    // Classe pour les projectiles
    private static class Projectile {
        public double x, y;
        public double speed;

        public Projectile(double x, double y, double speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
        }

        public void update() {
            y += speed;
        }

        public void render(GraphicsContext gc, Image image) {
            gc.drawImage(image, x, y, 10, 20);
        }

        public javafx.geometry.Rectangle2D getBounds() {
            return new javafx.geometry.Rectangle2D(x, y, 10, 20);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}