package DAO;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class users {
    private String username;
    private String password;

    public users() {
    }

    public users(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void addUser(users user) {
        try {
            Statement stm = ConnexionDB.seConnecter();
            stm.executeUpdate("INSERT INTO users VALUES('" + this.username + "','" + this.password + "')");
            if (stm == null) {
                System.out.println("erreur de connexion");
                return;
            }
        } catch (Exception e) {
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

    public List<users> getAllUsers() {
        List<users> ListUsers = new ArrayList<users>();
        try {
            Statement stm = ConnexionDB.seConnecter();
            ResultSet rs = stm.executeQuery("select * from users");
            while (rs.next()) {
                String username = rs.getString(1);
                String password = rs.getString(2);
                ListUsers.add(new users(username, password));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ListUsers;
    }

    public boolean verifyUser(String username, String password) {
        List<users> ListUsers = getAllUsers();
        for (users user : ListUsers) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }

    public boolean userExists(String username) {
        List<users> ListUsers = getAllUsers();
        for (users user : ListUsers) {
            if (user.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }
}