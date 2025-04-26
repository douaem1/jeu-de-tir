package design;

import javafx.animation.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;

import  Game.GameManager.*;
import static Game.GameManager.WINDOW_HEIGHT;
import static Game.GameManager.WINDOW_WIDTH;


public class design {
    private Stage primaryStage;
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    public   String[] BACKGROUND_PATHS = {"/img.jpg", "/background.jpg", "/backround.jpg"};
    public CopyOnWriteArrayList<Animation> activeAnimations = new CopyOnWriteArrayList<>();
//    private ImageView loadBestBackground() {
//        for (String path : BACKGROUND_PATHS) {
//            try (InputStream is = getClass().getResourceAsStream(path)) {
//                if (is != null) {
//                    Image image = new Image(is);
//                    ImageView view = new ImageView(image);
//                    setupBackgroundImage(view);
//                    return view;
//                }
//            } catch (Exception e) {
//                System.err.println("Erreur de chargement de l'image: " + path);
//            }
//        }
//        return createDefaultBackground();
//    }
    public ImageView createDefaultBackground() {
        Rectangle rect = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        rect.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1a2a6c")),
                new Stop(0.5, Color.web("#b21f1f")),
                new Stop(1, Color.web("#fdbb2d")))
        );
        return new ImageView(rect.snapshot(null, null));
    }
    public void setupBackgroundImage(ImageView imageView) {
        imageView.setFitWidth(WINDOW_WIDTH);
        imageView.setFitHeight(WINDOW_HEIGHT);
        imageView.setPreserveRatio(false);
        ColorAdjust adjust = new ColorAdjust();
        adjust.setBrightness(-0.1);
        adjust.setContrast(0.1);
        imageView.setEffect(adjust);
    }
    public Pane createParticleEffect() {
        Pane particlePane = new Pane();
        particlePane.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        for (int i = 0; i < 50; i++) {
            Circle particle = new Circle(Math.random() * 2 + 0.5);
            particle.setFill(Color.rgb(255, 255, 255, Math.random() * 0.6 + 0.2));
            particle.setCenterX(Math.random() * WINDOW_WIDTH);
            particle.setCenterY(Math.random() * WINDOW_HEIGHT);

            Glow glow = new Glow(0.8);
            particle.setEffect(glow);

            particlePane.getChildren().add(particle);

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(particle.opacityProperty(), Math.random() * 0.5 + 0.2)),
                    new KeyFrame(Duration.seconds(Math.random() * 3 + 1),
                            new KeyValue(particle.opacityProperty(), Math.random() * 0.8 + 0.5))
            );
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.setAutoReverse(true);
            timeline.play();
            activeAnimations.add(timeline);
        }

        return particlePane;
    }

    public void animateBackground(ImageView background) {
        ScaleTransition zoom = new ScaleTransition(Duration.seconds(30), background);
        zoom.setFromX(1.0);
        zoom.setFromY(1.0);
        zoom.setToX(1.05);
        zoom.setToY(1.05);
        zoom.setCycleCount(Animation.INDEFINITE);
        zoom.setAutoReverse(true);

        TranslateTransition pan = new TranslateTransition(Duration.seconds(40), background);
        pan.setFromX(0);
        pan.setToX(-20);
        pan.setCycleCount(Animation.INDEFINITE);
        pan.setAutoReverse(true);

        ParallelTransition parallelTransition = new ParallelTransition(zoom, pan);
        parallelTransition.play();
        activeAnimations.add(parallelTransition);
    }
    public Rectangle createOverlay() {
        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT);
        overlay.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.3, Color.rgb(0, 0, 0, 0.5)),
                new Stop(1, Color.rgb(0, 0, 0, 0.8))
        ));
        return overlay;
    }
    public ImageView loadBestBackground() {
        for (String path : BACKGROUND_PATHS) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    Image image = new Image(is);
                    ImageView view = new ImageView(image);
                    setupBackgroundImage(view);
                    return view;
                }
            } catch (Exception e) {
                System.err.println("Erreur de chargement de l'image: " + path);
            }
        }
        return createDefaultBackground();
    }

}

