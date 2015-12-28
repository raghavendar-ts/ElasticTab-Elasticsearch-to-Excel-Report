package com.elastictab.model;

import java.io.Serializable;
import java.util.List;

public class InputDataConfig implements Serializable{

	Report report;
	Elasticsearch elasticsearch;
	List<ReportColumnConfig> reportColumnConfig;
	ReportAccess reportAccess;

	String nullValue;

	InputDataConfig() {
		nullValue = "";
	}

	public Report getReport() {
		return report;
	}

	public void setReport(Report report) {
		this.report = report;
	}

	public Elasticsearch getElasticsearch() {
		return elasticsearch;
	}

	public void setElasticsearch(Elasticsearch elasticsearch) {
		this.elasticsearch = elasticsearch;
	}

	public List<ReportColumnConfig> getReportColumnConfig() {
		return reportColumnConfig;
	}

	public void setReportColumnConfig(List<ReportColumnConfig> reportColumnConfig) {
		this.reportColumnConfig = reportColumnConfig;
	}

	public ReportAccess getReportAccess() {
		return reportAccess;
	}

	public void setReportAccess(ReportAccess reportAccess) {
		this.reportAccess = reportAccess;
	}

	public String getNullValue() {
		return nullValue;
	}

	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

}
