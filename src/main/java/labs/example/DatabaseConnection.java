package labs.example;

import lombok.Data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Data
class DatabaseConnection{
    private static String USERNAME = "jdbc";
    private static String PASSWORD = "jdbc";
    private static String URL = "jdbc:postgresql://localhost:5432/jdbc";


    public static Connection getConnection() {
        try{
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL,USERNAME, PASSWORD);
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
