package ru.paranomum.page_object.model;

public enum VariableTypesEnum {
	//byte, short, int, long, char, float, double и boolean
	BYTE("byte"),
	SHORT("short"),
	INT("int"),
	LONG("long"),
	CHAR("char"),
	FLOAT("float"),
	DOUBLE("double"),
	BOOLEAN("boolean");

	private final String stringType;

	private VariableTypesEnum(String stringType) {
		this.stringType = stringType;
	}

	public boolean equals(String otherStringType) {
		return stringType.equals(otherStringType);
	}

	public String toString() {
		return this.stringType;
	}

	public static VariableTypesEnum valueOfLabel(String label) {
		if (label == null)
			return null;
		for (VariableTypesEnum e : values()) {
			if (e.stringType.equals(label)) {
				return e;
			}
		}
		return null;
	}
}
