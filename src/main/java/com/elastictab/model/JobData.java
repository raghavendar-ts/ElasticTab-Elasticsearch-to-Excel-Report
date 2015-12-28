package com.elastictab.model;

import java.util.List;

public class JobData {
	String name;
	List<TriggerData> triggerDataList;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<TriggerData> getTriggerDataList() {
		return triggerDataList;
	}

	public void setTriggerDataList(List<TriggerData> triggerDataList) {
		this.triggerDataList = triggerDataList;
	}

}
