package chat_Client_Serveur;


public class GameClient {

    public GameClient() {

    }


      private static class Player {
        String name;
        int x;
        int y;
        String aircraft;
        int health;
        int score;

        public Player(String name, int x, int y, String aircraft, int health, int score) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.aircraft = aircraft;
            this.health = health;
            this.score = score;
        }
    }

    private static class Enemy {
        int x;
        int y;
        int health;

        public Enemy(int x, int y, int health) {
            this.x = x;
            this.y = y;
            this.health = health;
        }
    }

    public static void main(String[] args) {
        GameClient client = new GameClient();

    }
}