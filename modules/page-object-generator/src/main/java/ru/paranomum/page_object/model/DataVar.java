package ru.paranomum.page_object.model;

import lombok.Getter;

@Getter
public class DataVar {
	public String type;
	public String name;
	public boolean needToInit = false;
	public String init;
}
