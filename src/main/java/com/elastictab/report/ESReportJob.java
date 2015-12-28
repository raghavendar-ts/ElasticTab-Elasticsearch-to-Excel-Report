package com.elastictab.report;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.MessagingException;

import org.apache.poi.ss.usermodel.Workbook;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.elastictab.model.InputDataConfig;
import com.elastictab.util.ObjectConversionUtil;

public class ESReportJob implements Job {

	private static Client getESClient() {
		String jarPath = System.getProperties().getProperty("user.dir");

		String hostname = "localhost";
		Properties properties = new Properties();
		try {
			InputStream input = new FileInputStream(jarPath + "\\properties\\elasticsearch.properties");
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!properties.get("hostname").equals(null) && !properties.get("hostname").equals("")) {
			hostname = (String) properties.get("hostname");
		}

		Builder builder = ImmutableSettings.settingsBuilder();
		builder.put("client.transport.sniff", true);
		if (!properties.get("clustername").equals(null) && !properties.get("clustername").equals("")) {
			builder.put("cluster.name", (String) properties.get("clustername"));
		}

		Settings settings = builder.build();
		Client esClient = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(hostname, 9300));
		return esClient;
	}

	public void execute(JobExecutionContext context) throws JobExecutionException {
		System.out.println("Job Started");
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		String inputDataConfigString = (String) jobDataMap.get("inputDataConfigString");

		InputDataConfig inputDataConfig = ObjectConversionUtil.stringToPOJO(inputDataConfigString);

		ESReport esReport = new ESReport();
		esReport.setData(inputDataConfig);
		try {
			Workbook wb = esReport.process(getESClient());
		} catch (MessagingException e) {
			System.out.println("Error while sending mail");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error while sending mail");
			e.printStackTrace();
		}

		System.out.println("Job Finished");
	}
}