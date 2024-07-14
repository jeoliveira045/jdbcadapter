package labs.example;

import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@AllArgsConstructor
public class EntityAccess<T> {

    private Class<T> classType;

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

    public void alterTable(Connection conn) throws SQLException {
        DatabaseMetaData databaseMetaData = conn.getMetaData();
        readingMetadataTable(databaseMetaData, classType, conn);
    }

    public void dropTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();

        statement.execute(String.format("DROP TABLE IF EXISTS %s", classType.getSimpleName().toUpperCase()));
    }

    public void insertData(T entityData, Connection conn) throws SQLException{
        StringBuilder insertStatement = new StringBuilder();

        insertStatement.append("INSERT INTO " + entityData.getClass().getSimpleName().toUpperCase() + "(");
        for(int i = 0; i < entityData.getClass().getDeclaredFields().length; i++){
            insertStatement.append(entityData.getClass().getDeclaredFields()[i].getName().toUpperCase()).append(setCommaOrFinalParentesis(i, entityData.getClass().getDeclaredFields().length));
        }
        insertStatement.append(" VALUES").append("(");
        for(int i = 0; i < entityData.getClass().getDeclaredFields().length; i++) {
            insertStatement.append("?").append(setCommaOrFinalParentesis(i, entityData.getClass().getDeclaredFields().length));
        }
        PreparedStatement statement = conn.prepareStatement(insertStatement.toString());
        for(int i = 1; i <= entityData.getClass().getDeclaredFields().length;i++) {
            try {
                var fieldName = entityData.getClass().getDeclaredFields()[i-1].getName();
                Field valor = entityData.getClass().getDeclaredField(fieldName);
                valor.setAccessible(true);
                statement.setObject(i, valor.get(entityData));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        };
        statement.executeUpdate();

    }


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
    private String setCommaOrFinalParentesis(Integer iteration, Integer fieldListLength){
        if(fieldListLength - 1 == iteration){
            return ")";
        }else{
            return ",";
        }
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
                            + " ADD " + declaredFieldsMapping.get(i) + " "
                            + getSQLType(Pessoa.class.getDeclaredFields()[i].getType().getSimpleName())
                    );
                    System.out.println(updateStatement);
                    conn.prepareStatement(updateStatement.toString()).executeUpdate();
                }
            }
        }else {
            for(int i = 0; i < tableFields.size(); i++){
                if(declaredFieldsMapping.get(i) == null){
                    StringBuilder updateStatement = new StringBuilder();
                    updateStatement.append("ALTER TABLE "
                            + classType.getSimpleName().toUpperCase()
                            + " DROP COLUMN " + tableFields.get(i)
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





}
