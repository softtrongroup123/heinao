package com.softtron.pinmaoserver.utils;

import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;


public class JdbcUtil {
	public static ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
	static ThreadLocal<Connection> local = new ThreadLocal<>();
	//	static String url;
//	static String username;
//	static String password;
	static {
//		// 1、加载数据库驱动
//		Properties po = new Properties();
//		try {
//			URL fileUrl = JdbcUtil.class.getClassLoader().getResource("jdbc.properties");
//			po.load(new FileInputStream(new File(fileUrl.toURI())));
//			Class.forName(po.getProperty("jdbc.driver"));
//			// 2、获取数据库连接（用户名，密码，连接地址）
//			url = po.getProperty("jdbc.url");
//			username = po.getProperty("jdbc.username");
//			password = po.getProperty("jdbc.password");
//		} catch (ClassNotFoundException | IOException | URISyntaxException e) {
//			e.printStackTrace();
//		}
	
		
	}

	public static Connection getConnection() throws ClassNotFoundException, SQLException {
		//return DriverManager.getConnection(url, username, password);
		Connection conn = local.get();
		if(conn == null) {
			conn = comboPooledDataSource.getConnection();
			local.set(conn);
		}
		return conn;
	}
	public static void closeConnection() {
		try {
			Connection conn = getConnection();
			if(conn!=null) {
				conn.close();
				local.remove();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
