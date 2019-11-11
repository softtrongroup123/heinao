package com.softtron.pinmaoserver.domains;

public class TMapper {
	private String id;
	private Class resultType;
	private Class parameterType;
	private TMapperType TYPE;
	private String sql;
    
	public TMapper(String id, Class resultType, Class parameterType, TMapperType tYPE, String sql) {
		super();
		this.id = id;
		this.resultType = resultType;
		this.parameterType = parameterType;
		TYPE = tYPE;
		this.sql = sql;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Class getResultType() {
		return resultType;
	}

	public void setResultType(Class resultType) {
		this.resultType = resultType;
	}

	public Class getParameterType() {
		return parameterType;
	}

	public void setParameterType(Class parameterType) {
		this.parameterType = parameterType;
	}

	public TMapperType getTYPE() {
		return TYPE;
	}

	public void setTYPE(TMapperType tYPE) {
		TYPE = tYPE;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	@Override
	public String toString() {
		return "TMapper [id=" + id + ", resultType=" + resultType + ", parameterType=" + parameterType + ", TYPE="
				+ TYPE + ", sql=" + sql + "]";
	}
	

}
