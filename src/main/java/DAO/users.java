package DAO;

import java.sql.Statement;

public class users {
    private String username;
    private String password;

public users() {
}
public users(String username, String password) {
    this.username = username;
    this.password = password;
}
public void addUser (users user){
    try {
        Statement stm = ConnexionDB.seConnecter();
        stm.executeUpdate("INSERT INTO users VALUES('" + this.username + "','" + this.password + "')");
    }catch (Exception e){
        e.printStackTrace();
    }
}
public String getUsername() {
    return username;
}
public void setUsername(String username) {
    this.username = username;
}
public String getPassword() {
    return password;
}
public void setPassword(String password) {
    this.password = password;
}
public void DeleteUser(users user) {
    try {
        Statement stm = ConnexionDB.seConnecter();
        stm.executeUpdate("DELETE FROM users WHERE username='" + this.username + "'");
    } catch (Exception e) {
        e.printStackTrace();
    }
}
public void UpdateUser(users user) {
    try {
        Statement stm = ConnexionDB.seConnecter();
        stm.executeUpdate("UPDATE users SET password='" + this.password + "' WHERE username='" + this.username + "'");
    } catch (Exception e) {
        e.printStackTrace();
    }
}

}

