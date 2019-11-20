package org.ciyam.at;

public class FunctionData {
	public final int paramCount;
	public final Long value1;
	public final Long value2;
	public final boolean returnValueExpected;
	public Long returnValue;

	private FunctionData(int paramCount, Long value1, Long value2, boolean returnValueExpected) {
		this.paramCount = paramCount;
		this.value1 = value1;
		this.value2 = value2;
		this.returnValueExpected = returnValueExpected;
		this.returnValue = null;
	}

	public FunctionData(boolean returnValueExpected) {
		this(0, null, null, returnValueExpected);
	}

	public FunctionData(Long value, boolean returnValueExpected) {
		this(1, value, null, returnValueExpected);
	}

	public FunctionData(Long value1, Long value2, boolean returnValueExpected) {
		this(2, value1, value2, returnValueExpected);
	}
}
