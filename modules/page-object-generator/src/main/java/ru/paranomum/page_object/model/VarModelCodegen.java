package ru.paranomum.page_object.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class VarModelCodegen {

	public String type;

	public String varName;
	public String toInit;

	public boolean needIndex = false;
	public long index;

	public boolean needToChangeData = false;
	public List<OverrideData> override = new ArrayList<>();

}
