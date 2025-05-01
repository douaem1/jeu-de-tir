package DAO;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class scores {

    private String username;
    private int score;
    private int round_duration;
    private String round_date;

    public scores(){
    }

    public scores( String username, int score, int round_duration, String round_date) {

        this.username = username;
        this.score = score;
        this.round_duration = round_duration;
        this.round_date = round_date;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public int getScore() {
        return score;
    }
    public void setScore(int score) {
        this.score = score;
    }
    public int getRoundDuration() {
        return round_duration;
    }
    public String getRound_date() {
        return round_date;
    }
    public void setRound_date(String round_date) {
        this.round_date = round_date;
    }
    public void setRoundDuration(int round_duration) {
        this.round_duration = round_duration;
    }

    // ajouter un score
    public void addScore(scores new_score){
        try {
            Statement stm = ConnexionDB.seConnecter();
            stm.executeUpdate("INSERT INTO scores (username, score, round_duration, round_date) VALUES ('" + new_score.getUsername() + "'," + new_score.getScore() + "," + new_score.getRoundDuration() + ", CURRENT_TIMESTAMP)");
            if (stm == null) {
                System.out.println("Erreur de connexion à la base de données ! ");
                return;
            }
        }catch (Exception erreur){
            erreur.printStackTrace();
        }
    }

    // recuperer tous les scores par user

    public List<scores> UsernameScore(String username){
        List<scores> ListScores = new ArrayList<scores>();
        try{
            // ouvrir la connexion a la base de données
            // Statement eest pour executer des requete SQL
            // recuperer les resultats de excuteQuery par ResultSet
            //stm.excuteQuery
            Statement stm = ConnexionDB.seConnecter();
            ResultSet rs=stm.executeQuery("SELECT * From scores WHERE username='"+username+"'ORDER BY round_date DESC");
            // tant qu'il reste des resultats a lire
            while (rs.next()){
                scores score = new scores();
                score.setUsername(rs.getString("username"));
                score.setScore(rs.getInt("score"));
                score.setRoundDuration(rs.getInt("round_duration"));
                score.setRound_date(rs.getString("round_date"));
                ListScores.add(score);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return ListScores;
    }
    //supprimmer un score de la liste
    public void DeleteScore(scores score){
        try {
            Statement stm = ConnexionDB.seConnecter();
            stm.executeUpdate("DELETE FROM scores WHERE username='" + score.getUsername() + "'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}




