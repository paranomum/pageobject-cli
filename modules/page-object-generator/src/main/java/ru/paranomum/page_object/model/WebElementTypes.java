package ru.paranomum.page_object.model;

import java.util.List;
import java.util.Map;

public class WebElementTypes {
	public String type;
	public String xpath;
	public String toImport;
	public List<String> attributeToInit;
	public List<String> innerXpathToInit;
	public String dataType;
	public Map<String, String> override;
}
