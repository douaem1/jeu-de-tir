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

public class ShooterEnemy {
    private final GameManager gameManager;
    private final ImageView enemy;

    public ShooterEnemy(GameManager gameManager) {
        this.gameManager = gameManager;
        this.enemy = createShooterEnemy();
    }
    private ImageView createShooterEnemy() {
        try {
            Image image = new Image(getClass().getResourceAsStream("enemy_shooter.png"));
            ImageView shooter = new ImageView(image);
            shooter.setFitWidth(80);
            shooter.setFitHeight(40);
            shooter.setX(Math.random() * (GameManager.WINDOW_WIDTH - 80));
            shooter.setY(-40);
            shooter.setEffect(new DropShadow(10, Color.PURPLE));

            if (gameManager.currentLevel >= 5) {
                scheduleShots(shooter);
            }

            return shooter;
        } catch (Exception e) {
            return createFallbackShooter();
        }
    }

    private void scheduleShots(ImageView shooter) {
        Timeline shotTimer = new Timeline(
                new KeyFrame(Duration.seconds(1.5 + Math.random() * 2), e -> {
                    if (gameManager.gameRunning && gameManager.currentLevel >= 5) {
                        fireProjectile(shooter);
                    }
                })
        );
        shotTimer.setCycleCount(Animation.INDEFINITE);
        shotTimer.play();
        gameManager.activeAnimations.add(shotTimer);
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