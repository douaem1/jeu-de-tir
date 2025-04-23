package Menu;
/*

public class MenuManager {
    private VBox createMainContainer() {
        VBox container = new VBox(25);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40, 60, 40, 60));
        container.setMaxWidth(750);

        Label title = createTitleLabel();
        Label subtitle = createSubtitleLabel();
        VBox actionButtons = createActionButtons();

        container.getChildren().addAll(title, subtitle, actionButtons);
        animateMenuEntrance(container);

        return container;
    }

    private Label createTitleLabel() {
        Label label = new Label("JET FIGHTERS");
        label.setFont(Font.font(FONT_FAMILIES[0], FontWeight.EXTRA_BOLD, 74));
        label.setTextFill(COLORS.get("LIGHT"));

        DropShadow glow = new DropShadow(15, COLORS.get("PRIMARY"));
        glow.setSpread(0.3);
        Bloom bloom = new Bloom(0.3);

        label.setEffect(new Blend(BlendMode.SCREEN, bloom, glow));
        animateTextGlow(label, glow);

        return label;
    }
    private Label createSubtitleLabel() {
        Label label = new Label("Only the Fastest Survive the Sky");
        label.setFont(Font.font(FONT_FAMILIES[2], FontWeight.SEMI_BOLD, 35));
        label.setTextFill(COLORS.get("LIGHT"));

        FadeTransition fade = new FadeTransition(Duration.seconds(3), label);
        fade.setFromValue(0.7);
        fade.setToValue(1.0);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(2), label);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1.02);
        scale.setToY(1.02);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setAutoReverse(true);

        ParallelTransition parallel = new ParallelTransition(fade, scale);
        parallel.play();
        activeAnimations.add(parallel);

        return label;
    }
    private VBox createActionButtons() {
        VBox buttonBox = new VBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setFillWidth(true);

        Button startBtn = createActionButton("START AS A GUEST", "PRIMARY");
        Button signUpBtn = createActionButton("SIGN UP", "ACCENT");
        Button signInBtn = createActionButton("SIGN IN", "SECONDARY");
        Button quitBtn = createActionButton("EXIT", "DANGER");

        startBtn.setPrefSize(300, 60);
        signUpBtn.setPrefSize(300, 60);
        signInBtn.setPrefSize(300, 60);
        quitBtn.setPrefSize(300, 60);

        startBtn.setMaxWidth(Double.MAX_VALUE);
        signUpBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        quitBtn.setMaxWidth(Double.MAX_VALUE);

        startBtn.setOnAction(e -> {
            playButtonPressAnimation(startBtn);
            transitionToScene(() -> startGame());
        });

        signUpBtn.setOnAction(e -> {
            playButtonPressAnimation(signUpBtn);
            transitionToScene(() -> showSignUpScene());
        });

        signInBtn.setOnAction(e -> {
            playButtonPressAnimation(signInBtn);
            transitionToScene(() -> showSignInScene());
        });

        quitBtn.setOnAction(e -> {
            playButtonPressAnimation(quitBtn);
            fadeOutAndClose();
        });

        buttonBox.getChildren().addAll(startBtn, signUpBtn, signInBtn, quitBtn);
        return buttonBox;
    }
    private void animateMenuEntrance(VBox container) {
        FadeTransition fade = new FadeTransition(Duration.seconds(1.2), container);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.seconds(1), container);
        slide.setFromY(30);
        slide.setToY(0);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(1.2), container);
        scale.setFromX(0.95);
        scale.setFromY(0.95);
        scale.setToX(1.0);
        scale.setToY(1.0);

        ParallelTransition parallel = new ParallelTransition(fade, slide, scale);
        parallel.play();
        activeAnimations.add(parallel);
    }

}*/