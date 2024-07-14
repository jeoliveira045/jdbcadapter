package labs.example;

import java.sql.*;



/**
 * Hello world!
 *
 */
public class App 
{

    public static void main( String[] args ) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        EntityAccess<Pessoa> entityAccess = new EntityAccess<>(Pessoa.class);
        entityAccess.createTable(conn);
    }


}












