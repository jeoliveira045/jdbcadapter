package labs.example;

import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

@AllArgsConstructor
public class EntityAccess<T> {

    private Class<T> classType;
    
    private Connection conn;

    private static final List<Class<?>> primitiveClassType = Arrays.asList(int.class, long.class, boolean.class,byte.class, short.class, boolean.class, float.class, char.class);

    public  void createTable() throws SQLException {
        Statement statement = conn.createStatement();
        try{

            StringBuilder query = new StringBuilder();

            String sequenceName = (classType.getSimpleName().toUpperCase() + "_ID_SEQUENCE");

            DatabaseMetaData databaseMetaData  = conn.getMetaData();
            ResultSet resultSet = databaseMetaData.getTables(null, null, sequenceName, new String[] {"SEQUENCE"});

            if(resultSet.next()){
                createSequence();
            }


            query.append("CREATE TABLE ").append(classType.getSimpleName().toUpperCase()).append(" (\n");
            for(int i = 0; i < classType.getDeclaredFields().length; i++){
                int finalI1 = i;
                var primitive = Arrays.stream(classType.getDeclaredFields()[finalI1].getType().getDeclaredFields()).anyMatch(classAttributeType -> {
                    return primitiveClassType.stream().anyMatch(primitiveClassTypeItem -> {
                        return primitiveClassTypeItem.isAssignableFrom(classAttributeType.getType());
                    });
                });
                var fieldName = classType.getDeclaredFields()[i].getName().toUpperCase();
                var fieldType = getSQLType(classType.getDeclaredFields()[i].getType().getSimpleName(), !primitive);
                var commaOrBreakLine = setCommaOrBreakLine(i, classType.getDeclaredFields().length);
                if(fieldName.equalsIgnoreCase("ID")){
                    query.append(fieldName).append(" ").append(fieldType).append(" PRIMARY KEY DEFAULT nextval('").append(classType.getSimpleName().toUpperCase() + "_ID_SEQUENCE')").append(commaOrBreakLine);
                }else {
                    query.append(fieldName).append(" ").append(fieldType).append(commaOrBreakLine);
                }
            }
            query.append(");");

            statement.execute(query.toString());
        } catch (SQLException e) {
            if(e.getMessage().contains("relation \"" + classType.getSimpleName().toLowerCase() + "\" already exists")){
                alterTable();
            }
        }
    }

    private void createSequence() throws SQLException{
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CREATE SEQUENCE " + classType.getSimpleName().toUpperCase() + "_ID_SEQUENCE START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1");

        conn.prepareStatement(stringBuilder.toString()).execute();
    }

    public void alterTable() throws SQLException {
        DatabaseMetaData databaseMetaData = conn.getMetaData();
        readingMetadataTable(databaseMetaData);
    }

    public void dropTable() throws SQLException {
        Statement statement = conn.createStatement();

        statement.execute(String.format("DROP TABLE IF EXISTS %s", classType.getSimpleName().toUpperCase()));
    }

    public void saveData(T entityData) throws NoSuchFieldException, IllegalAccessException, SQLException {
        StringBuilder queryStatement = new StringBuilder();
        Field idField = entityData.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        if(idField.get(entityData) != null){
            queryStatement.append("SELECT * FROM " + entityData.getClass().getSimpleName().toUpperCase() + " WHERE ID = ?");
            var stmt = conn.prepareStatement(queryStatement.toString());
            stmt.setObject(1, idField.get(entityData));
            stmt.execute();

            ResultSet queryList = stmt.getResultSet();
            if(queryList.next()){
                updateData(entityData);
            }else {
                idField.set(entityData, null);
                insertData(entityData);
            }

        }else {
            insertData(entityData);
        }
    }

    private void insertData(T entityData) throws SQLException, NoSuchFieldException, IllegalAccessException{
        StringBuilder insertStatement = new StringBuilder();

        insertStatement.append("INSERT INTO " + entityData.getClass().getSimpleName().toUpperCase() + "(");
        for(int i = 1; i < entityData.getClass().getDeclaredFields().length; i++){
            insertStatement.append(entityData.getClass().getDeclaredFields()[i].getName().toUpperCase()).append(setCommaOrFinalParentesis(i, entityData.getClass().getDeclaredFields().length));
        }
        insertStatement.append(" VALUES").append("(");
        for(int i = 1; i < entityData.getClass().getDeclaredFields().length; i++) {
            insertStatement.append("?").append(setCommaOrFinalParentesis(i, entityData.getClass().getDeclaredFields().length));
        }
        PreparedStatement statement = conn.prepareStatement(insertStatement.toString());
        for(int i = 1; i < entityData.getClass().getDeclaredFields().length;i++) {
                var fieldName = entityData.getClass().getDeclaredFields()[i].getName();
                Field valor = entityData.getClass().getDeclaredField(fieldName);
                valor.setAccessible(true);
                statement.setObject(i, valor.get(entityData));
        };
        statement.executeUpdate();
    }

    private void updateData(T entityData) throws SQLException{
        StringBuilder updateStatement = new StringBuilder();

        updateStatement.append("UPDATE " + classType.getSimpleName().toUpperCase() + " SET ");
        for(int i = 0; i < classType.getDeclaredFields().length; i++){
            updateStatement.append(classType.getDeclaredFields()[i].getName().toUpperCase() + " = ?" + setCommaOrBreakLine(i, classType.getDeclaredFields().length));
        }
        updateStatement.append("WHERE ID = ?");
        PreparedStatement statement = conn.prepareStatement(updateStatement.toString());
        for(int i = 1; i <= entityData.getClass().getDeclaredFields().length+1;i++) {
            try {
                if(i <= entityData.getClass().getDeclaredFields().length){
                    var fieldName = entityData.getClass().getDeclaredFields()[i-1].getName();
                    Field valor = entityData.getClass().getDeclaredField(fieldName);
                    valor.setAccessible(true);
                    statement.setObject(i, valor.get(entityData));
                } else {
                    var fieldName = entityData.getClass().getDeclaredFields()[i-(entityData.getClass().getDeclaredFields().length+1)].getName();
                    Field valor = entityData.getClass().getDeclaredField(fieldName);
                    valor.setAccessible(true);
                    statement.setObject(i, valor.get(entityData));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        };
        statement.executeUpdate();
    }


    private String getSQLType(String type, Boolean hasForeignKey){
        Map<String, String> SQLtypeMap = new HashMap<>();
        SQLtypeMap.put("Long", "INTEGER");
        SQLtypeMap.put("String", "VARCHAR(255)");
        SQLtypeMap.put("BigDecimal", "DECIMAL");
        SQLtypeMap.put("Double", "DOUBLE");

        if(hasForeignKey){
            return "INTEGER REFERENCES " + type.toUpperCase() + " (ID)";
        }

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
            Map<Integer, String> tableFields
    ) throws SQLException {
        try {
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
                    var primitive = Arrays.stream(classType.getDeclaredFields()[i].getType().getDeclaredFields()).anyMatch(classAttributeType ->
                            primitiveClassType.stream().anyMatch(primitiveClassTypeItem -> primitiveClassTypeItem.isAssignableFrom(classAttributeType.getType()))
                    );
                    if(tableFields.get(i) == null){
                        StringBuilder updateStatement = new StringBuilder();
                        updateStatement.append("ALTER TABLE "
                                + classType.getSimpleName().toUpperCase()
                                + " ADD " + declaredFieldsMapping.get(i) + " "
                                + getSQLType(classType.getDeclaredFields()[i].getType().getSimpleName(), !primitive)
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
                                + " DROP COLUMN " + tableFields.get(i)
                        );
                        conn.prepareStatement(updateStatement.toString()).executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {

        }

    }

    private void readingMetadataTable(DatabaseMetaData databaseMetaData) throws SQLException {
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
                for(int i = 0; i < classType.getDeclaredFields().length; i++){
                    declaredFieldsMapping.put(i, classType.getDeclaredFields()[i].getName());
                }
                comparingClassToTable(declaredFieldsMapping, tableFields);
                rsColumns.close();
            }
        }
        rsTables.close();
    }
}
