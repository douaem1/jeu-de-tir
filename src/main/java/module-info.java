module org.example.jeu {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.media;  // Essentiel pour MediaPlayer
  requires javafx.graphics;

  opens org.example.jeu to javafx.fxml;
  exports org.example.jeu;
}