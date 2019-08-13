package at.ac.univie.jakobhirschl.contintcollector.db;

/**
 * Created by Jakob Hirschl on 13.11.2015.
 *
 * Contains all column names and commands
 *
 */
public interface DBInterface
{
    String TABLE_SESSION = "SESSION";
    String SESSION_COL1 = "ID";
    String SESSION_COL2 = "SESSIONID";
    String SESSION_COL3 = "COMMENT";
    String SESSION_COL4 = "USERSTUDY";

    String TABLE_DATA = "DATA";
    String DATA_COL1 = "SESSION_ID";
    String DATA_COL2 = "TIMESTAMP";
    String DATA_COL3 = "DATA";

    String GET_DATA = "SELECT * FROM " + TABLE_DATA + " WHERE " + DATA_COL1 + " = ?";
    String GET_SESSION = "SELECT * FROM " + TABLE_SESSION + " WHERE " + SESSION_COL1 + " = ?";

    String CREATE_TABLE_SESSION = "CREATE TABLE " + TABLE_SESSION  + "(" +
            SESSION_COL1 + " integer primary key autoincrement," +
            SESSION_COL2 + " text," +
            SESSION_COL3 + " text," +
            SESSION_COL4 + " text)";

    String CREATE_TABLE_DATA = "CREATE TABLE " + TABLE_DATA + "(" +
            DATA_COL1 + " integer NOT NULL," +
            DATA_COL2 + " integer NOT NULL," +
            DATA_COL3 + " text," +
            "FOREIGN KEY(" + DATA_COL1 + ") REFERENCES SESSION(" + SESSION_COL1 +")," +
            "PRIMARY KEY(" + DATA_COL1 + ", " + DATA_COL2 + "))";
}
