package com.elastictab.model;

import java.util.List;

public class ScheduleReport {
	boolean isSchedule;
	List<String> cronExpressionList;

	public boolean isSchedule() {
		return isSchedule;
	}

	public void setSchedule(boolean isSchedule) {
		this.isSchedule = isSchedule;
	}

	public List<String> getCronExpressionList() {
		return cronExpressionList;
	}

	public void setCronExpressionList(List<String> cronExpressionList) {
		this.cronExpressionList = cronExpressionList;
	}

}
