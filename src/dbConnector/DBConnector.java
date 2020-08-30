package dbConnector;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DBConnector {

	private static String DATABASE_NAME = "test";
//	private static String DATABASE_USER = "root";
	private static String DATABASE_USER = "JDBC";
//	private static String DATABASE_Password = "";
	private static String DATABASE_Password = "Forgettable";
	private static String SQLURL = "jdbc:mysql://tlehnertz.ddns.net";
//	private static String SQLURL = "jdbc:mysql://localhost";
	private static int DATABASE_PORT = 8457;
	private static String DATABASE_QUERY_STRING = "useJDBCCompliantTimezoneShift=true&useLegacyDatetimCode=false&serverTimezone=Europe/Berlin";
	private static boolean begun = false;
	
	static List<Entity<?>> registeredEntities = new ArrayList<Entity<?>>();
	
	static Connection con = null;
	
	private static DBConnector instance = new DBConnector();
	
	private DBConnector() {
		super();
	}
	
	public static DBConnector getInstance() {
		return instance;
	}
	
	public boolean begin() {
		return begin(getDATABASE_NAME());
	}
	
	public boolean begin(String name) {
		if(begun) {
			return false;
		}else {
			setDATABASE_NAME(name);
			boolean success = openConnection();
			success &= closeConnection();
			begun = success;
			return success;
		}
	}
	
	public static boolean registerEntity(Entity<?> entity) {
		System.out.println("registered: " + entity.getClass().getSimpleName());
		return registeredEntities.add(entity);
	}
	
	public static void saveAllEntities() {
		for (Entity<?> entity : registeredEntities) {
			entity.save();
		}
	}
	
	private static boolean openConnection() {
		return openConnection(true);
	}
	
	private static boolean openConnection(boolean firstTry) {
		try {
			con = DriverManager.getConnection(getDatabaseUrl(), getDATABASE_USER(), getDATABASE_Password());
			return true;
		} catch (SQLException e) {
			if(firstTry) {
				if(e.getMessage().contains("Unknown database")) {
					System.out.println("databse " + getDATABASE_NAME() + " is unknown trying to create " + getDATABASE_NAME());
					if(createDatabase()) {
						if(openConnection(false))
							return true;
					}
				}
			}
			e.printStackTrace();
			return false;
		}
	}
	
	private static boolean closeConnection() {
		if(con != null) {
			try {
				con.close();
				return con.isClosed();
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			return false;
		}
	}
	
	private static boolean createDatabase() {
		if(!openConnection(getSQLURL()))
			return false;
		try {
			Statement stmt = con.createStatement();
			stmt.execute("CREATE DATABASE " + getDATABASE_NAME());
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private static boolean openConnection(String url) {
		try {
			con = DriverManager.getConnection(url, getDATABASE_USER(), getDATABASE_Password());
			System.out.println("Connection to " + url + " successfull");
			return true;
		} catch (SQLException e) {
			System.out.println("Connection to " + url + " failed");
			e.printStackTrace();
			return false;
		}
	}
	
	/*
	 * 
	 */
	protected int executeStatement(String statement) {
		if(!openConnection() || begun == false)
			return -1;
		int id = -1;
		try {
			Statement stmt = con.createStatement();
			id = stmt.executeUpdate(statement, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stmt.getGeneratedKeys();
			if(rs.next())
                id = rs.getInt(1);
			stmt.close();
//			System.out.println("executed: " + statement + " || id = " + id);
		} catch (SQLException e) {
			System.out.println("failed: " + statement);
			e.printStackTrace();
			closeConnection();
			return -1;
		}
		closeConnection();
		return id;
	}
	
	public static List<HashMap<String, Object>> executeQuery (String query) {
		if(!openConnection())
			return null;
		Statement stmt;
		ResultSet rs;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return null;
		}
		List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
	    try {
			while (rs.next()) {
				list.add(new HashMap<String, Object>());
				for (int i = 1; i < rs.getMetaData().getColumnCount() + 1; i++) {						//	DISGUSTING
					list.get(list.size() - 1).put(rs.getMetaData().getColumnName(i), rs.getObject(i));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	    closeConnection();
		return list;
	}
	
	
	public boolean doesTableExist(String name) {
		if(!begun)
			return false;
		try {
			if(openConnection()) {
				DatabaseMetaData dbm = con.getMetaData();
			    ResultSet rs;
				rs = dbm.getTables(null, null, "employee", null);
				closeConnection();
				return rs.next();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void printSelectAll(String name) {
		if(!openConnection())
			return;
		try {
			ResultSet rs = con.createStatement().executeQuery("SELECT * FROM " + name);
			int columns = rs.getMetaData().getColumnCount();
			for(int i = 1; i<=columns; i++)
				System.out.print(String.format("%-16s", rs.getMetaData().getColumnLabel(i)));
			
			System.out.println();
			System.out.println("----------------------------------------------------------------");
			
			while(rs.next()) {
				for(int i = 1; i<=columns; i++)
					System.out.print(String.format("%-15s", rs.getString(i)) + "|");
				System.out.println();
			}
			
			rs.close();
		}catch(SQLException e) {
			e.printStackTrace();
		}
		closeConnection();
	}
	
	
	/*
	 * getters and Setters
	 */	
	
	public static String getDATABASE_NAME() {
		return DATABASE_NAME;
	}

	public static boolean setDATABASE_NAME(String dATABASE_NAME) {
		if(begun)
			return false;
		DATABASE_NAME = dATABASE_NAME;
		return true;
	}

	public static String getDATABASE_USER() {
		return DATABASE_USER;
	}

	public static boolean setDATABASE_USER(String dATABASE_USER) {
		if(begun)
			return false;
		DATABASE_USER = dATABASE_USER;
		return true;
	}

	public static String getDATABASE_Password() {
		return DATABASE_Password;
	}

	public static boolean setDATABASE_Password(String dATABASE_Password) {
		if(begun)
			return false;
		DATABASE_Password = dATABASE_Password;
		return true;
	}

	public static String getSQLURL() {
		return SQLURL;
	}

	public static boolean setSQLURL(String sQLURL) {
		if(begun)
			return false;
		SQLURL = sQLURL;
		return true;
	}

	public static int getDATABASE_PORT() {
		return DATABASE_PORT;
	}

	public static boolean setDATABASE_PORT(int dATABASE_PORT) {
		if(begun)
			return false;
		DATABASE_PORT = dATABASE_PORT;
		return true;
	}

	public static String getDATABASE_QUERY_STRING() {
		return DATABASE_QUERY_STRING;
	}

	public static boolean setDATABASE_QUERY_STRING(String dATABASE_QUERY_STRING) {
		if(begun)
			return false;
		DATABASE_QUERY_STRING = dATABASE_QUERY_STRING;
		return true;
	}

	public static String getDatabaseUrl() {
		return getSQLURL() + ":" + getDATABASE_PORT() + "/" + getDATABASE_NAME() + "?" + getDATABASE_QUERY_STRING();
	}
}