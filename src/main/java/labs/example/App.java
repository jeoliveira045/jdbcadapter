package labs.example;

import java.sql.*;
import java.time.LocalDate;


/**
 * Hello world!
 *
 */
public class App 
{

    public static void main( String[] args ) throws SQLException, NoSuchFieldException, IllegalAccessException {
        Connection conn = DatabaseConnection.getConnection();
        EntityAccess<Pessoa> entityAccess = new EntityAccess<>(Pessoa.class);

        entityAccess.createTable(conn);
        Pessoa pessoa = new Pessoa();
        pessoa.setId(9L);
        pessoa.setNome("Mariana");
        pessoa.setCpf("111.222.111-11");
        pessoa.setEndereco("Rua das Rosas");
        pessoa.setDataNascimento(LocalDate.now());
        entityAccess.saveData(pessoa, conn);
//        entityAccess.dropTable(conn);
    }


}












