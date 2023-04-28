package my.framework.dao;

import java.util.HashMap;
import java.util.Map;

/**
 * 实现数据库类型与Java类型的映射
 */
public class TypeConstant {
	/***
	 * 数据库类型和Java类型映射，key为数据库类型，value为Java类型
	 */
	private static Map<String, Class<?>> typeMap = new HashMap<String, Class<?>>();

	static {
		typeMap.put("BIGINT", java.lang.Long.class);
		typeMap.put("INT", java.lang.Integer.class);
		typeMap.put("VARCHAR", java.lang.String.class);
		typeMap.put("TEXT", java.lang.String.class);
		typeMap.put("DATETIME", java.util.Date.class);
		typeMap.put("DECIMAL", java.lang.Double.class);
		typeMap.put("TINYINT", java.lang.Integer.class);
		typeMap.put("BIT", java.lang.Boolean.class);
		typeMap.put("TIMESTAMP", java.util.Date.class);
	}

	public static void addType(String columnType, Class<?> javaType) {
		typeMap.put(columnType, javaType);
	}

	public static Class<?> getJavaType(String columnType) {
		return typeMap.get(columnType);
	}
}
