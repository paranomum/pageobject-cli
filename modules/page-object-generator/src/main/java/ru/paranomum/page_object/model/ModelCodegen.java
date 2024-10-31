package ru.paranomum.page_object.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ModelCodegen {
	public String _package;
	public Set<String> imports = new TreeSet<>();
	public List<VarModelCodegen> vars = new ArrayList<>();
}
