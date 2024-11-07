package ru.paranomum.page_object.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ModelCodegen {
	public String packageName;
	public String className;
	public List<Map<String, String>> imports = new ArrayList<>();
	public List<VarModelCodegen> vars = new ArrayList<>();

	public boolean hasDataVars = false;
	public String dataVarName;
	public List<DataVar> dataVars = new ArrayList<>();

	public void setImport(Map<String, String> imports) {
		this.imports.add(imports);
	}
}
