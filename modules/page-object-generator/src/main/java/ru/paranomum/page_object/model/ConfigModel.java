package ru.paranomum.page_object.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ConfigModel {
	public String packageName;
	public String outputDir;

	public List<WebElementTypes> configuration = new ArrayList<>();

}
