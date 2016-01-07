package com.elastictab.report;

import java.io.IOException;

import javax.mail.MessagingException;

import org.apache.poi.ss.usermodel.Workbook;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.elastictab.model.InputDataConfig;
import com.elastictab.util.ObjectConversionUtil;

public class ESReportJob implements Job {

	public void execute(JobExecutionContext context) throws JobExecutionException {
		System.out.println("Job Started");
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		String inputDataConfigString = (String) jobDataMap.get("inputDataConfigString");

		InputDataConfig inputDataConfig = ObjectConversionUtil.stringToPOJO(inputDataConfigString);

		ESReport esReport = new ESReport();
		esReport.setData(inputDataConfig);
		try {
			Workbook wb = esReport.process();
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