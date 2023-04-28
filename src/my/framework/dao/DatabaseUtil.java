package my.framework.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库连接与关闭工具类。
 */
public class DatabaseUtil {
	private static String driver = ConfigManager.getProperty("driver");// 数据库驱动字符串
	private static String url = ConfigManager.getProperty("url");// 连接URL字符串
	private static String user = ConfigManager.getProperty("user"); // 数据库用户名
	private static String password = ConfigManager.getProperty("password"); // 用户密码

	static {
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static final ThreadLocal<Connection> threadLocal = new ThreadLocal<>();

	/**
	 * 获取数据库连接对象。
	 * 
	 * @throws SQLException
	 */
	public static Connection getConnection() throws SQLException {
		// 获取连接并捕获异常
		Connection connection = threadLocal.get();
		if (connection == null || connection.isClosed())
			try {
				connection = DriverManager.getConnection(url, user, password);
				if (connection.getAutoCommit())
					connection.setAutoCommit(false);
				threadLocal.set(connection);
			} catch (SQLException e) {
				e.printStackTrace();
				throw e;
			}
		return connection;// 返回连接对象
	}

	/**
	 * 关闭语句容器。
	 * 
	 * @param stmt
	 *            Statement对象
	 * @param rs
	 *            结果集
	 */
	public static void closeStatement(Statement stmt, ResultSet rs) {
		// 若结果集对象不为空，则关闭
		try {
			if (rs != null && !rs.isClosed())
				rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 若Statement对象不为空，则关闭
		try {
			if (stmt != null && !stmt.isClosed())
				stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 关闭数据库连接。
	 */
	public static void closeConnection() {
		Connection connection = (Connection) threadLocal.get();
		threadLocal.set(null);

		try {
			if (connection != null && !connection.isClosed())
				connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
