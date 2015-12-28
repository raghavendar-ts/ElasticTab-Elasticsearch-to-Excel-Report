package com.elastictab.model;

import java.util.Date;

import org.quartz.Trigger.TriggerState;

public class TriggerData {
	String name;
	String cronExpression;
	Date nextFireTime;
	Date previousFireTime;
	TriggerState triggerState;

	public TriggerState getTriggerState() {
		return triggerState;
	}

	public void setTriggerState(TriggerState triggerState) {
		this.triggerState = triggerState;
	}

	public Date getNextFireTime() {
		return nextFireTime;
	}

	public void setNextFireTime(Date nextFireTime) {
		this.nextFireTime = nextFireTime;
	}

	public Date getPreviousFireTime() {
		return previousFireTime;
	}

	public void setPreviousFireTime(Date previousFireTime) {
		this.previousFireTime = previousFireTime;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
