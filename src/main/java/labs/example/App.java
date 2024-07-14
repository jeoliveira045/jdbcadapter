package labs.example;

import lombok.AllArgsConstructor;

import java.sql.*;



/**
 * Hello world!
 *
 */
public class App 
{

    public static void main( String[] args ) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        DataAccess<Pessoa> dataAccess = new DataAccess<>(Pessoa.class);
        dataAccess.createTable(conn);
    }


}












