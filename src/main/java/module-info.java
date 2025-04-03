module org.example.jeu {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.jeu to javafx.fxml;
    exports org.example.jeu;
}