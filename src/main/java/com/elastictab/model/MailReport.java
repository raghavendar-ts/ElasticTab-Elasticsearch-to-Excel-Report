package com.elastictab.model;

import java.util.List;

public class MailReport {
	String subject;
	List<String> mailList;
	String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public List<String> getMailList() {
		return mailList;
	}

	public void setMailList(List<String> mailList) {
		this.mailList = mailList;
	}

}
