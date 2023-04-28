package my.framework.dao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import my.framework.dao.DatabaseUtil;

/**
 * 执行数据库操作的工具类。
 */
public class CommonDao {

	/**
	 * 增、删、改操作
	 * 
	 * @param sql
	 *            sql语句
	 * @param prams
	 *            参数数组
	 * @return 执行结果
	 * @throws SQLException
	 */
	public int executeUpdate(String sql, Object[] params) throws SQLException {
		int result = 0;
		PreparedStatement pstmt = null;
		try {
			pstmt = DatabaseUtil.getConnection().prepareStatement(sql);
			if (!(params == null || params.length == 0))
				for (int i = 0; i < params.length; i++) {
					pstmt.setObject(i + 1, params[i]);
				}
			result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			DatabaseUtil.closeStatement(pstmt, null);
		}
		return result;
	}

	public int executeUpdate(String sql, Object entity) throws SQLException {
		Map<String, Object[]> mappingResult = handleParameterMapping(sql, entity);
		Entry<String, Object[]> entry = mappingResult.entrySet().iterator().next();
		return executeUpdate(entry.getKey(), entry.getValue());
	}

	protected Map<String, Object[]> handleParameterMapping(String sql, Object entity) throws SQLException {
		Map<String, Object[]> mappingResult = new HashMap<String, Object[]>();
		List<String> placeholders = new ArrayList<String>();
		int offset = 0;
		while (true) {
			int start = sql.indexOf("#{", offset);
			if (start >= offset) {
				int end = sql.indexOf("}", start);
				placeholders.add(sql.substring(start + 2, end));
				offset = end + 1;
			} else {
				break;
			}
		}
		if (placeholders.size() > 0) {
			Object[] params = new Object[placeholders.size()];
			for (int i = 0; i < placeholders.size(); ++i) {
				String placeholder = placeholders.get(i);
				sql = sql.replaceFirst("\\#\\{" + placeholder + "\\}", "?");
				try {
					Method getter = entity.getClass()
							.getMethod("get" + placeholder.substring(0, 1).toUpperCase() + placeholder.substring(1));
					Object param = getter.invoke(entity);
					params[i] = param;
				} catch (NullPointerException | NoSuchMethodException | SecurityException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("无法为占位符 #{" + placeholder + "} 赋值！", e);
				}
			}
			System.out.println("===================================SQL: " + sql); // log
			String ps = "[\n";
			for (Object p : params) ps += "\t" + p + "\n";
			ps += "]";
			System.out.println("===================================Params: \n" + ps); // log
			mappingResult.put(sql, params);
		} else {
			mappingResult.put(sql, new Object[] {});
		}
		return mappingResult;
	}

	/**
	 * 查询操作
	 * 
	 * @param sql
	 *            sql语句
	 * @param params
	 *            参数数组
	 * @return 查询结果
	 * @throws SQLException
	 */
	public <T> List<T> executeQuery(Class<T> clz, String sql, Object[] params) throws SQLException {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DatabaseUtil.getConnection().prepareStatement(sql);
			if (!(params == null || params.length == 0))
				for (int i = 0; i < params.length; i++) {
					pstmt.setObject(i + 1, params[i]);
				}
			rs = pstmt.executeQuery();

			return handleResultMapping(clz, rs);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException("封装查询结果时出现错误！", e);
		} finally {
			DatabaseUtil.closeStatement(pstmt, rs);
		}
	}

	public <T> List<T> executeQuery(Class<T> clz, String sql, Object entity) throws SQLException {
		Map<String, Object[]> mappingResult = handleParameterMapping(sql, entity);
		Entry<String, Object[]> entry = mappingResult.entrySet().iterator().next();
		return executeQuery(clz, entry.getKey(), entry.getValue());
	}

	@SuppressWarnings("unchecked")
	protected <T> List<T> handleResultMapping(Class<T> clz, ResultSet rs) throws SQLException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (clz.equals(Integer.class)) {
			List<Integer> result = new ArrayList<Integer>();
			while (rs.next())
				result.add(rs.getInt(1));
			return (List<T>) result;
		}
		ResultSetMetaData metaData = rs.getMetaData();
		int count = metaData.getColumnCount();
		if (clz.equals(java.lang.Object[].class)) {
			List<Object[]> result = new ArrayList<Object[]>();
			Object[] row = null;
			while (rs.next()) {
				row = new Object[count];
				for (int i = 0; i < count; ++i) {
					Class<?> targetClz = TypeConstant.getJavaType(metaData.getColumnTypeName(i + 1));
					if (targetClz.equals(java.util.Date.class))
						row[i] = new java.util.Date(rs.getTimestamp(i + 1).getTime());
					else
						row[i] = rs.getObject(i + 1, targetClz);
				}
				result.add(row);
			}
			return (List<T>) result;
		}
		Method[] setters = new Method[count];
		Class<?>[] types = new Class[count];
		for (int i = 0, j = 1; i < count; ++i, ++j) {
			String lable = metaData.getColumnLabel(j);
			String type = metaData.getColumnTypeName(j);
			try {
				setters[i] = clz.getMethod("set" + lable.substring(0, 1).toUpperCase() + lable.substring(1),
						TypeConstant.getJavaType(type));
				types[i] = TypeConstant.getJavaType(type);
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace(); // log
				setters[i] = null;
				types[i] = null;
			}
		}
		List<T> result = new ArrayList<T>();
		T instance = null;
		while (rs.next()) {
			instance = clz.newInstance();
			for (int k = 0; k < count; ++k) {
				if (setters[k] == null)
					continue;
				else {
					System.out.println("===========================setter: " + setters[k].getName() + "( " + types[k].getTypeName() + " )");
					System.out.println("===========================param: " + (rs.getObject(k+1)==null?null:rs.getObject(k+1).getClass()));
					if (types[k].equals(java.util.Date.class))
						setters[k].invoke(instance, new java.util.Date(rs.getTimestamp(k + 1).getTime()));
					else
						setters[k].invoke(instance, rs.getObject(k + 1, types[k]));
				}
			}
			result.add(instance);
		}
		return result;
	}
}
