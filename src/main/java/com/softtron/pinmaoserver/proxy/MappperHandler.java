package com.softtron.pinmaoserver.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import com.softtron.pinmaoserver.annotations.Column;
import com.softtron.pinmaoserver.annotations.Key;
import com.softtron.pinmaoserver.annotations.Sons;
import com.softtron.pinmaoserver.controllers.CommonController;
import com.softtron.pinmaoserver.domains.TMapper;
import com.softtron.pinmaoserver.utils.JdbcUtil;
import com.sun.javafx.applet.Splash;

public class MappperHandler implements InvocationHandler {
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object ob = null;
		try {
			if (Object.class == method.getDeclaringClass()) {
				method.invoke(this, args);
			} else {
				// 方法名
				String methodName = method.getName();
				// 类的名字
				Class clazz = method.getDeclaringClass();
				TMapper mapper = (TMapper) CommonController.tMapperMaps.get(clazz.getName() + "/" + methodName);
				if (mapper != null) {
					Connection conn = null;
					PreparedStatement preparedStatement = null;
					try {
						conn = JdbcUtil.getConnection();
						String sql = mapper.getSql();
						// 处理输入参数
						Class parameterType = mapper.getParameterType();
						Set set = null;
						HashMap map = new HashMap();// 参数map
						Parameter[] parameters = method.getParameters();
						if (args != null) {
							for (int i = 0; i < args.length; i++) {
								Object object = args[i];
								if (object.getClass() == parameterType && parameterType == HashMap.class) {
									map = (HashMap) object;
									set = map.keySet();
								} else if (object.getClass() == parameterType
										&& (parameterType == Integer.class || parameterType == String.class)) {
									map.put(parameters[i].getName(), object);
								}
							}
							if (parameterType == HashMap.class) {
								// 将#{page}换成map对应的值
								for (Object object : set) {
									String pattern = "#\\{" + object + "\\}";
									sql = sql.replaceAll(pattern, String.valueOf(map.get(object)));
								}
							} else if (parameterType == Integer.class || parameterType == String.class) {
								// 将#{page}换成map对应的值
								Set<String> keys = map.keySet();
								for (String key : keys) {
									String pattern = "#\\{" + key + "\\}";
									if (parameterType == Integer.class) {
										sql = sql.replaceAll(pattern, String.valueOf(map.get(key)));
									}
									if (parameterType == String.class) {
										sql = sql.replaceAll(pattern, "'" + String.valueOf(map.get(key)) + "'");
									}
								}
							} else {
								// 类型
								Field[] fields = parameterType.getDeclaredFields();
								for (Field field : fields) {
									field.setAccessible(true);
									Annotation[] annotations = field.getDeclaredAnnotations();
									for (Annotation an : annotations) {
										if (an instanceof Column) {
											Column column = (Column) an;
											String key = column.name();
											// 将sql进行替换
											String pattern = "#\\{" + field.getName() + "\\}";
											Object fieldValue = field.get(args[0]);// 获取属性值
											if (fieldValue == null) {
												fieldValue = "";
											}
											// 在替换之前，判断sql中是否包含pattern
											if (sql.indexOf("#{" + field.getName() + "}") != -1) {// 包含
												if (field.getType() == String.class)
													sql = sql.replaceAll(pattern,
															"'" + String.valueOf(fieldValue) + "'");
												else if (field.getType() == Date.class) {
													Date date = (Date) fieldValue;
													long time = date.getTime();// 从时间戳里面拿到的所有毫秒数
													sql = sql.replaceAll(pattern, String.valueOf(
															"FROM_UNIXTIME(" + time / 1000 + ",'%Y-%m-%d %H:%i:%S')"));
												} else {
													sql = sql.replaceAll(pattern, String.valueOf(fieldValue));
												}
											}
										}
									}
								}
							}
							sql = sql.trim().replaceAll("\r", "").replaceAll("\n", "").replaceAll("\\s{2,}", " ");
						}
						System.out.println(sql);
						preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
						switch (mapper.getTYPE()) {
							case SELECT:
								ob = select(conn, sql, preparedStatement, mapper);
								break;
							case INSERT:
								ob = insert(conn, preparedStatement, mapper);
								break;
							case DELETE:
								ob = delete(conn, preparedStatement, mapper);
								break;
							case UPDATE:
								ob = update(conn, preparedStatement, mapper);
								break;
							default:
								break;
						}
					} catch (Exception e) {
						// conn.rollback();
						throw e;
					} finally {
						if (preparedStatement != null)
							preparedStatement.close();
						JdbcUtil.closeConnection();
					}
					
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return ob;
	}
	
	@SuppressWarnings(value = "unchecked")
	public Object select(Connection conn, String sql, PreparedStatement preparedStatement, TMapper mapper)
			throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException,
			IllegalArgumentException, ParseException {
		Class resultType = mapper.getResultType();
		
		ResultSet rs = preparedStatement.executeQuery();
		Set set = new LinkedHashSet();
		Field[] fields = resultType.getDeclaredFields();
		while (rs.next()) {
			// 判断返回类型是否为基本数据类型
			if (resultType == int.class || resultType == Integer.class) {
				return rs.getInt(1);
			}
			Object ob = resultType.newInstance();
			for (Field field : fields) {
				field.setAccessible(true);
				// 判断field类型
				Class fieldClazz = field.getType();
				Annotation[] annotations = field.getAnnotations();
				for (Annotation annotation : annotations) {
					if (annotation instanceof Column) {
						
						Column col = (Column) annotation;// 得到的列名
						// 判断该列中是否有当前字段,或者有*
						if ((sql.indexOf(col.name()) != -1) && !(sql.indexOf(col.name() + "=") != -1)
								|| sql.indexOf("*") != -1) {
							if (fieldClazz == int.class || fieldClazz == Integer.class) {
								field.set(ob, rs.getInt(col.name()));
							}
							if (fieldClazz == String.class) {
								field.set(ob, rs.getString(col.name()));
							}
							if (fieldClazz == double.class || fieldClazz == Double.class) {
								field.set(ob, rs.getDouble(col.name()));
							}
							if (fieldClazz == Date.class) {
								java.sql.Timestamp timeStamp = rs.getTimestamp(col.name());
								field.set(ob, timeStamp);
							}
						}
					}
					// 子类
					if (annotation instanceof Sons) {
						Sons sons = (Sons) annotation;
						Class clazz = Class.forName(sons.clazz());
						Object son = clazz.newInstance();
						Field[] sonfields = clazz.getDeclaredFields();
						// sons的主键
						Annotation[] sonClassAnnotation = clazz.getAnnotations();
						String keyname = null;
						for (Annotation an : sonClassAnnotation) {
							if (an instanceof Key) {
								Key sonKey = (Key) an;
								keyname = sonKey.name();
							}
						}
						// 根据主键有无判断是否创建子元素
						if (rs.getInt(keyname) != 0) {
							for (Field sonField : sonfields) {
								sonField.setAccessible(true);
								Annotation[] sonAnnotations = sonField.getAnnotations();
								Class sonfieldClazz = sonField.getType();
								for (Annotation an : sonAnnotations) {
									if (an instanceof Column) {
										Column sonColumn = (Column) an;
										String sonKey = sonColumn.name();
										if (sonfieldClazz == int.class || sonfieldClazz == Integer.class) {
											sonField.set(son, rs.getInt(sonKey));
										}
										if (sonfieldClazz == String.class) {
											sonField.set(son, rs.getString(sonKey));
										}
										if (sonfieldClazz == double.class || sonfieldClazz == Double.class) {
											sonField.set(son, rs.getDouble(sonKey));
										}
									}
								}
							}
							Set children = (Set) field.get(ob);
							children.add(son);
						}
					}
					
				}
				
			}
			set.add(ob);
			// int productId = rs.getInt("productId");
			// String productName = rs.getString("productName");
			
		}
		return set;
		
	}
	
	public Object insert(Connection conn, PreparedStatement preparedStatement, TMapper mapper) throws SQLException {
		Object ob = null;
		try {
			if (preparedStatement.executeUpdate() > 0) {
				ResultSet rs = preparedStatement.getGeneratedKeys();
				while (rs.next()) {
					ob = rs.getInt(1);
				}
			}
		} catch (SQLException e) {
			throw e;
		}
		return ob;
	}
	
	public Object update(Connection conn, PreparedStatement preparedStatement, TMapper mapper) throws SQLException {
		Integer result = 1;
		try {
			preparedStatement.execute();
		} catch (SQLException e) {
			result = 0;
			throw e;
		}
		return result;
	}
	
	public Object delete(Connection conn, PreparedStatement preparedStatement, TMapper mapper) throws SQLException {
		Integer result = 1;
		try {
			preparedStatement.execute();
		} catch (SQLException e) {
			result = 0;
			throw e;
		}
		return result;
	}
	
}
