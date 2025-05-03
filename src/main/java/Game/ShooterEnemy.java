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
            InputStream imageStream = getClass().getResourceAsStream("/enemy_shooter.png");
            if (imageStream == null) {
                throw new IOException("Image resource not found");
            }

            Image image = new Image(imageStream);
            shooter = new ImageView(image);
            shooter.setFitWidth(80);
            shooter.setFitHeight(40);
            shooter.setX(Math.random() * (GameManager.WINDOW_WIDTH - shooter.getFitWidth()));
            shooter.setY(-shooter.getFitHeight());
            DropShadow glow = new DropShadow(10, Color.PURPLE);
            glow.setSpread(0.5);
            shooter.setEffect(glow);
            shooter.getStyleClass().add("shooter-enemy");
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
        gameManager.activeAnimations.add(shootingTimeline);
    }

    private void shootLaser(ImageView shooter) {
        Rectangle laser = new Rectangle(4, 20, Color.PURPLE);
        laser.setEffect(new Glow(0.8));
        double x = shooter.getX() + shooter.getFitWidth()/2 - laser.getWidth()/2;
        double y = shooter.getY() + shooter.getFitHeight();
        laser.setX(x);
        laser.setY(y);

        gameManager.gamepane.getChildren().add(laser);
        Timeline laserAnimation = new Timeline(
                new KeyFrame(Duration.millis(16), e -> {
                    laser.setY(laser.getY() + 5);
                    if (laser.getBoundsInParent().intersects(gameManager.player.getBoundsInParent())) {
                        gameManager.handlePlayerHit();
                        gameManager.gamepane.getChildren().remove(laser);
                    }
                    if (laser.getY() > GameManager.WINDOW_HEIGHT) {
                        gameManager.gamepane.getChildren().remove(laser);
                    }
                })
        );
        laserAnimation.setCycleCount(Animation.INDEFINITE);
        laserAnimation.play();

        gameManager.activeAnimations.add(laserAnimation);
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