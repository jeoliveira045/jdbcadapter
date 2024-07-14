package labs.example;

import java.sql.*;
import java.time.LocalDate;


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
        Pessoa pessoa = new Pessoa();
        pessoa.setId(1L);
        pessoa.setNome("João");
        pessoa.setCpf("111.111.111-11");
        pessoa.setEndereco("Rua das Flores");
//        pessoa.setDataNascimento(LocalDate.now());
        entityAccess.insertData(pessoa, conn);
    }


}












