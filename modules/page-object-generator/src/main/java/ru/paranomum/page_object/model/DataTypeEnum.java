package ru.paranomum.page_object.model;

import java.util.List;

public enum DataTypeEnum {
	STRING("String", null, null),
	INTEGER("Integer", null, null),
	BOOLEAN("Boolean", null, null),
	LIST_STRING("List<String>", "new ArrayList<>()",
			List.of("java.util.ArrayList",
			"java.util.List"));

	private final String stringType;
	private final String initData;
	private final List<String> imports;

	private DataTypeEnum(String stringType, String initData, List<String> imports) {
		this.stringType = stringType;
		this.initData = initData;
		this.imports = imports;
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
	public List<String> imports() {
		return this.imports;
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
