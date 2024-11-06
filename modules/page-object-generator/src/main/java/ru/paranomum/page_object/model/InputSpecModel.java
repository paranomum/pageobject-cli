package ru.paranomum.page_object.model;

import java.util.List;

public class InputSpecModel {
	public List<HtmlFiles> files;

	public static class HtmlFiles {
		public String packageName;
		public String className;
		public String pathToHtml;
	}
}
