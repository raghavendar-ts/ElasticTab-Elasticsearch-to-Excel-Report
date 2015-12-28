package com.elastictab.model;

public class ReportAccess {

	boolean fileReportEnabled;
	FileReport fileReport;
	boolean mailReportEnabled;
	MailReport mailReport;
	boolean scheduleReportEnabled;
	ScheduleReport scheduleReport;

	public boolean isFileReportEnabled() {
		return fileReportEnabled;
	}

	public void setFileReportEnabled(boolean fileReportEnabled) {
		this.fileReportEnabled = fileReportEnabled;
	}

	public FileReport getFileReport() {
		return fileReport;
	}

	public void setFileReport(FileReport fileReport) {
		this.fileReport = fileReport;
	}

	public boolean isMailReportEnabled() {
		return mailReportEnabled;
	}

	public void setMailReportEnabled(boolean mailReportEnabled) {
		this.mailReportEnabled = mailReportEnabled;
	}

	public MailReport getMailReport() {
		return mailReport;
	}

	public void setMailReport(MailReport mailReport) {
		this.mailReport = mailReport;
	}

	public boolean isScheduleReportEnabled() {
		return scheduleReportEnabled;
	}

	public void setScheduleReportEnabled(boolean scheduleReportEnabled) {
		this.scheduleReportEnabled = scheduleReportEnabled;
	}

	public ScheduleReport getScheduleReport() {
		return scheduleReport;
	}

	public void setScheduleReport(ScheduleReport scheduleReport) {
		this.scheduleReport = scheduleReport;
	}

}
