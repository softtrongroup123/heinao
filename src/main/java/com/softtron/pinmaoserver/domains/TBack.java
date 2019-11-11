package com.softtron.pinmaoserver.domains;

public class TBack {
	private String code;
	private Object ob;
	private String message;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Object getOb() {
		return ob;
	}

	public void setOb(Object ob) {
		this.ob = ob;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public TBack(String code, Object ob, String message) {
		super();
		this.code = code;
		this.ob = ob;
		this.message = message;
	}

}
