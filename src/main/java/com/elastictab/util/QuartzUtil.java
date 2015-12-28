package com.elastictab.util;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import com.elastictab.model.InputDataConfig;
import com.elastictab.model.JobData;
import com.elastictab.model.TriggerData;
import com.elastictab.report.ESReportJob;

public class QuartzUtil {
	static Properties quartzClientProperties = new Properties();

	static Scheduler getScheduler() {
		String jarPath = System.getProperties().getProperty("user.dir");

		Scheduler scheduler = null;
		try {
			InputStream input = new FileInputStream(jarPath + "\\properties\\quartzClient.properties");
			quartzClientProperties.load(input);
			scheduler = new StdSchedulerFactory(quartzClientProperties).getScheduler();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
		return scheduler;
	}

	public static void createSchedule(InputDataConfig inputDataConfig) throws SchedulerException {
		Set<Trigger> triggerList = new HashSet<Trigger>();

		JobDataMap jobDataMap = new JobDataMap();
		String inputDataConfigString = ObjectConversionUtil.POJOToJSON(inputDataConfig);

		jobDataMap.put("inputDataConfigString", inputDataConfigString);

		JobDetail job = newJob(ESReportJob.class).withIdentity(inputDataConfig.getReport().getName(), quartzClientProperties.getProperty("group")).setJobData(jobDataMap).build();

		int i = 0;
		for (String cronExpression : inputDataConfig.getReportAccess().getScheduleReport().getCronExpressionList()) {
			String triggerName = inputDataConfig.getReport().getName() + "_" + String.valueOf(i);
			Trigger trigger = newTrigger().withIdentity(triggerName, quartzClientProperties.getProperty("group")).startNow().withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build();
			i++;
			triggerList.add(trigger);

		}
		getScheduler().scheduleJob(job, triggerList, false);
	}

	public static void updateScheduleReport(InputDataConfig inputDataConfig) throws SchedulerException {
		Set<Trigger> triggerList = new HashSet<Trigger>();

		JobDataMap jobDataMap = new JobDataMap();
		String inputDataConfigString = ObjectConversionUtil.POJOToJSON(inputDataConfig);

		jobDataMap.put("inputDataConfigString", inputDataConfigString);

		JobDetail job = newJob(ESReportJob.class).withIdentity(inputDataConfig.getReport().getName(), quartzClientProperties.getProperty("group")).setJobData(jobDataMap).build();

		int i = 0;
		for (String cronExpression : inputDataConfig.getReportAccess().getScheduleReport().getCronExpressionList()) {
			String triggerName = inputDataConfig.getReport().getName() + "_" + String.valueOf(i);
			Trigger trigger = newTrigger().withIdentity(triggerName, quartzClientProperties.getProperty("group")).startNow().withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build();
			i++;
			triggerList.add(trigger);

		}
		getScheduler().scheduleJob(job, triggerList, true);
	}

	public static List<JobData> listJobs() throws SchedulerException {
		List<JobData> jobDataList = new ArrayList<JobData>();

		for (String group : getScheduler().getJobGroupNames()) {

			for (JobKey jobKey : getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(group))) {
				JobData jobData = new JobData();
				jobData.setName(jobKey.getName());

				List<Trigger> triggers = (List<Trigger>) getScheduler().getTriggersOfJob(jobKey);

				List<TriggerData> triggerDataList = new ArrayList<TriggerData>();

				for (Trigger trigger : triggers) {
					TriggerData triggerData = new TriggerData();

					triggerData.setName(trigger.getKey().getName());
					triggerData.setCronExpression(((CronTrigger) trigger).getCronExpression());
					triggerData.setNextFireTime(trigger.getNextFireTime());
					triggerData.setPreviousFireTime(trigger.getPreviousFireTime());
					triggerData.setTriggerState(getScheduler().getTriggerState(new TriggerKey(trigger.getKey().getName(), quartzClientProperties.getProperty("group"))));

					triggerDataList.add(triggerData);
				}
				jobData.setTriggerDataList(triggerDataList);
				jobDataList.add(jobData);
			}
		}
		return jobDataList;
	}

	public static TriggerState updateJobState(String jobName, String triggerName, boolean triggerState) throws SchedulerException {
		if (triggerState) {
			getScheduler().resumeTrigger(new TriggerKey(triggerName, quartzClientProperties.getProperty("group")));
		} else {
			getScheduler().pauseTrigger(new TriggerKey(triggerName, quartzClientProperties.getProperty("group")));
		}
		return getScheduler().getTriggerState(new TriggerKey(triggerName, quartzClientProperties.getProperty("group")));

	}

	public static boolean deleteJob(String jobName) throws SchedulerException {
		return getScheduler().deleteJob(new JobKey(jobName, quartzClientProperties.getProperty("group")));
	}

	public static boolean jobExist(String jobName) throws SchedulerException {
		return getScheduler().checkExists(new JobKey(jobName, quartzClientProperties.getProperty("group")));
	}

	public static String getJobData(String reportName) throws SchedulerException {
		Map<String, Object> jobData = getScheduler().getJobDetail(new JobKey(reportName, quartzClientProperties.getProperty("group"))).getJobDataMap();
		return ObjectConversionUtil.MapToJSONString(jobData);
	}

	public static void triggerJob(String jobName) throws SchedulerException {
		JobKey jobKey = new JobKey(jobName, quartzClientProperties.getProperty("group"));
		getScheduler().triggerJob(jobKey);
	}
}
