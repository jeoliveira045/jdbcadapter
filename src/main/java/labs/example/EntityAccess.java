package labs.example;

import lombok.AllArgsConstructor;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class DataAccess<T> {

    private Class<T> classType;

    private String getSQLType(String type){
        Map<String, String> SQLtypeMap = new HashMap<>();
        SQLtypeMap.put("Long", "INTEGER");
        SQLtypeMap.put("String", "VARCHAR(255)");
        SQLtypeMap.put("LocalDate", "DATE");

        return SQLtypeMap.get(type);
    }

    private String setCommaOrBreakLine(Integer iteration, Integer fieldListLength){
        if(fieldListLength - 1 == iteration){
            return "\n";
        }else{
            return ",\n";
        }
    }

    public void alterTable(Connection conn) throws SQLException {
        DatabaseMetaData databaseMetaData = conn.getMetaData();
        readingMetadataTable(databaseMetaData, classType, conn);
    }

    private void comparingClassToTable(
            Map<Integer, String> declaredFieldsMapping,
            Map<Integer, String> tableFields,
            Connection conn
    ) throws SQLException {
        if(declaredFieldsMapping.size() == tableFields.size()){
            for(int i = 0; i < declaredFieldsMapping.size(); i++){
                if(!declaredFieldsMapping.get(i).equalsIgnoreCase(tableFields.get(i))){
                    StringBuilder updateStatement = new StringBuilder();
                    updateStatement.append("ALTER TABLE "
                            + classType.getSimpleName().toUpperCase()
                            + "CHANGE " + tableFields.get(i) + " " + declaredFieldsMapping.get(i));
                    conn.prepareStatement(updateStatement.toString()).executeUpdate();
                }
            }
        }else if(declaredFieldsMapping.size() > tableFields.size()){
            for(int i = 0; i < declaredFieldsMapping.size(); i++){
                if(tableFields.get(i) == null){
                    StringBuilder updateStatement = new StringBuilder();
                    updateStatement.append("ALTER TABLE "
                            + classType.getSimpleName().toUpperCase()
                            + "ADD " + declaredFieldsMapping.get(i) + " "
                            + getSQLType(Pessoa.class.getDeclaredFields()[i].getType().getSimpleName())
                    );
                    conn.prepareStatement(updateStatement.toString()).executeUpdate();
                }
            }
        }else {
            for(int i = 0; i < tableFields.size(); i++){
                if(declaredFieldsMapping.get(i) == null){
                    StringBuilder updateStatement = new StringBuilder();
                    updateStatement.append("ALTER TABLE "
                            + classType.getSimpleName().toUpperCase()
                            + "DROP COLUMN " + tableFields.get(i)
                    );
                    conn.prepareStatement(updateStatement.toString()).executeUpdate();
                }
            }
        }
    }

    private void readingMetadataTable(DatabaseMetaData databaseMetaData, Class<T> classType, Connection conn) throws SQLException {
        ResultSet rsTables = databaseMetaData.getTables(null,null, null, new String[]{"TABLE"});

        while (rsTables.next()) {
            String tableName = rsTables.getString("TABLE_NAME");
            if (classType.getSimpleName().equalsIgnoreCase(tableName)) {
                Map<Integer, String> declaredFieldsMapping = new HashMap<>();
                Map<Integer, String> tableFields = new HashMap<>();
                ResultSet rsColumns = databaseMetaData.getColumns(null, null, tableName, null);
                var columnNumber = 0;
                while (rsColumns.next()) {
                    tableFields.put(columnNumber,rsColumns.getString("COLUMN_NAME"));
                    columnNumber++;
                }
                for(int i = 0; i < Pessoa.class.getDeclaredFields().length; i++){
                    declaredFieldsMapping.put(i, Pessoa.class.getDeclaredFields()[i].getName());
                }
                comparingClassToTable(declaredFieldsMapping, tableFields, conn);
                rsColumns.close();
            }
        }
        rsTables.close();
    }

    public  void createTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        try{

            StringBuilder query = new StringBuilder();

            query.append("CREATE TABLE ").append(classType.getSimpleName().toUpperCase()).append(" (\n");

            for(int i = 0; i < classType.getDeclaredFields().length; i++){
                var fieldName = Pessoa.class.getDeclaredFields()[i].getName().toUpperCase();
                var fieldType = getSQLType(classType.getDeclaredFields()[i].getType().getSimpleName());
                var commaOrBreakLine = setCommaOrBreakLine(i, classType.getDeclaredFields().length);
                query.append(fieldName).append(" ").append(fieldType).append(commaOrBreakLine);
            }
            query.append(");");

            statement.execute(query.toString());
        } catch (SQLException e){
            if(e.getMessage().contains("relation \"" + classType.getSimpleName().toLowerCase() + "\" already exists")){
                alterTable(connection);
            }
        }
    }


    public void dropTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();

        statement.execute(String.format("DROP TABLE IF EXISTS %s", classType.getSimpleName().toUpperCase()));
    }
}
