package ru.paranomum.page_object.model;

import lombok.Getter;

@Getter
public class VarModelCodegen {

	public String type = "";

	public String varName = "";
	public String toInit = "";

	public boolean needIndex = false;
	public long index;

}
