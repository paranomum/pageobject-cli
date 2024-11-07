package ru.paranomum.page_object.model;

public enum DataTypeEnum {
	STRING("String", null),
	INTEGER("Integer", null),
	BOOLEAN("Boolean", null),
	LIST_STRING("List<String>", "new ArrayList<>()");

	private final String stringType;
	private final String initData;

	private DataTypeEnum(String stringType, String initData) {
		this.stringType = stringType;
		this.initData = initData;
	}

	public boolean equals(String otherStringType) {
		return stringType.equals(otherStringType);
	}

	public String toString() {
		return this.stringType;
	}
	public String initData() {
		return this.initData;
	}


	public static DataTypeEnum valueOfLabel(String label) {
		if (label == null)
			return null;
		for (DataTypeEnum e : values()) {
			if (e.stringType.equals(label)) {
				return e;
			}
		}
		return null;
	}
}
