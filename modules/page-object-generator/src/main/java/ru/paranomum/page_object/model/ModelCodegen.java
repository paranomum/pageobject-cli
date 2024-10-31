package ru.paranomum.page_object.model;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class ModelCodegen {
	public String _package;
	public List<Map<String, String>> imports = new ArrayList<>();
	public List<VarModelCodegen> vars = new ArrayList<>();

	public void setImport(Map<String, String> imports) {
		this.imports.add(imports);
	}
}
