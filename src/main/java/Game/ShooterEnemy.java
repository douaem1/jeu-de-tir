package Game;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;

public class ShooterEnemy {
    private final GameManager gameManager;
    private final ImageView enemy;

    public ShooterEnemy(GameManager gameManager) {
        this.gameManager = gameManager;
        this.enemy = createShooterEnemy();
    }
    private ImageView createShooterEnemy() {
        ImageView shooter = null;

        try {
            // Charger l'image du tireur depuis les ressources
            InputStream imageStream = getClass().getResourceAsStream("/enemy_shooter.png");
            if (imageStream == null) {
                throw new IOException("Image resource not found");
            }

            Image image = new Image(imageStream);
            shooter = new ImageView(image);
            shooter.setFitWidth(80);
            shooter.setFitHeight(40);

            // Position aléatoire en haut de l'écran
            shooter.setX(Math.random() * (GameManager.WINDOW_WIDTH - shooter.getFitWidth()));
            shooter.setY(-shooter.getFitHeight()); // Commence juste hors de l'écran

            // Effet visuel distinctif
            DropShadow glow = new DropShadow(10, Color.PURPLE);
            glow.setSpread(0.5);
            shooter.setEffect(glow);

            // Ajouter une classe CSS pour identification
            shooter.getStyleClass().add("shooter-enemy");

            // Programmer les tirs seulement à partir du niveau 5
            if (gameManager != null && gameManager.currentLevel >= 5) {
                scheduleShots(shooter);
            }

        } catch (Exception e) {
            System.err.println("Error creating shooter enemy: " + e.getMessage());
            shooter = createFallbackShooter();
        }

        return shooter;
    }
    private void scheduleShots(ImageView shooter) {
        if (gameManager == null || !gameManager.gameRunning) return;

        Timeline shootingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(2), e -> {
                    if (gameManager.gameRunning && shooter.getParent() != null) {
                        shootLaser(shooter);
                    }
                })
        );
        shootingTimeline.setCycleCount(Animation.INDEFINITE);
        shootingTimeline.play();

        // Stocker l'animation pour pouvoir l'arrêter plus tard
        gameManager.activeAnimations.add(shootingTimeline);
    }

    private void shootLaser(ImageView shooter) {
        Rectangle laser = new Rectangle(4, 20, Color.PURPLE);
        laser.setEffect(new Glow(0.8));

        // Positionner le laser au centre de l'ennemi
        double x = shooter.getX() + shooter.getFitWidth()/2 - laser.getWidth()/2;
        double y = shooter.getY() + shooter.getFitHeight();
        laser.setX(x);
        laser.setY(y);

        gameManager.gamepane.getChildren().add(laser);

        // Animation du laser
        Timeline laserAnimation = new Timeline(
                new KeyFrame(Duration.millis(16), e -> {
                    laser.setY(laser.getY() + 5); // Vitesse du laser

                    // Vérifier les collisions avec le joueur
                    if (laser.getBoundsInParent().intersects(gameManager.player.getBoundsInParent())) {
                        gameManager.handlePlayerHit();
                        gameManager.gamepane.getChildren().remove(laser);
                    }

                    // Supprimer s'il sort de l'écran
                    if (laser.getY() > GameManager.WINDOW_HEIGHT) {
                        gameManager.gamepane.getChildren().remove(laser);
                    }
                })
        );
        laserAnimation.setCycleCount(Animation.INDEFINITE);
        laserAnimation.play();

        // Stocker l'animation
        gameManager.activeAnimations.add(laserAnimation);
    }

    private void fireProjectile(ImageView shooter) {
        Rectangle projectile = new Rectangle(5, 20, Color.PURPLE);
        projectile.setEffect(new Glow(0.8));
        projectile.setX(shooter.getX() + shooter.getFitWidth() / 2 - 2.5);
        projectile.setY(shooter.getY() + shooter.getFitHeight());

        gameManager.gamepane.getChildren().add(projectile);

        Timeline animation = new Timeline(
                new KeyFrame(Duration.millis(16), e -> {
                    if (!gameManager.gameRunning) {
                        ((Timeline) e.getSource()).stop();
                        return;
                    }

                    projectile.setY(projectile.getY() + 7);

                    if (gameManager.player != null && projectile.getBoundsInParent()
                            .intersects(gameManager.player.getBoundsInParent())) {
                        gameManager.handlePlayerHit();
                        removeProjectile(projectile);
                    }

                    if (projectile.getY() > GameManager.WINDOW_HEIGHT) {
                        removeProjectile(projectile);
                    }
                })
        );
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
        gameManager.activeAnimations.add(animation);
    }

    private void removeProjectile(Rectangle projectile) {
        Platform.runLater(() -> {
            gameManager.gamepane.getChildren().remove(projectile);
        });
    }

    private ImageView createFallbackShooter() {
        Rectangle enemyShape = new Rectangle(40, 20, Color.PURPLE);
        enemyShape.setArcWidth(15);
        enemyShape.setArcHeight(15);
        enemyShape.setStroke(Color.WHITE);
        enemyShape.setStrokeWidth(2);

        ImageView fallback = new ImageView(enemyShape.snapshot(null, null));
        fallback.setFitWidth(80);
        fallback.setFitHeight(40);
        fallback.setX(Math.random() * (GameManager.WINDOW_WIDTH - 80));
        fallback.setY(-40);

        return fallback;
    }

    public ImageView getEnemy() {
        return enemy;
    }
}