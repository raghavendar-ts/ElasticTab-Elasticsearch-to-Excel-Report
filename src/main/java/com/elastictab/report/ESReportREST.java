package com.elastictab.report;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.poi.ss.usermodel.Workbook;
import org.elasticsearch.client.Client;
import org.json.JSONObject;
import org.quartz.SchedulerException;
import org.quartz.Trigger.TriggerState;

import com.elastictab.model.InputDataConfig;
import com.elastictab.model.JobData;
import com.elastictab.util.Constants;
import com.elastictab.util.ObjectConversionUtil;
import com.elastictab.util.QuartzUtil;
import com.elastictab.util.Util;

@Path("/esreport")
public class ESReportREST {

	@Context
	private ServletContext context;

	@POST
	@Path("report")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String report(InputDataConfig inputDataConfig) {
		Client esClient = (Client) context.getAttribute(Constants.ES_CLIENT);
		ESReport esReport = new ESReport();
		esReport.setData(inputDataConfig);
		try {
			Workbook wb = esReport.process(esClient);
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@POST
	@Path("download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response reportString(@FormParam("inputDataConfigString") String inputDataConfigString) {
		Client esClient = (Client) context.getAttribute(Constants.ES_CLIENT);
		InputDataConfig inputDataConfig = ObjectConversionUtil.stringToPOJO(inputDataConfigString);

		ESReport esReport = new ESReport();
		esReport.setData(inputDataConfig);

		Workbook wb = null;
		try {
			wb = esReport.process(esClient);
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] bytes = Util.WorkbookToByteArray(wb);
		ResponseBuilder response = Response.ok(bytes, MediaType.APPLICATION_OCTET_STREAM);
		response.header("Content-Disposition", "attachment; filename=" + inputDataConfig.getReport().getFileName() + ".xls");
		return response.build();
	}

	@POST
	@Path("scheduleReport")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String scheduleReport(InputDataConfig inputDataConfig) {
		JSONObject response = new JSONObject();

		response = Util.validate(inputDataConfig);

		if (response.getInt(Constants.STATUS_CODE) == 0) {
			return response.toString();
		}

		try {
			if (QuartzUtil.jobExist(inputDataConfig.getReport().getName())) {
				response.put(Constants.STATUS_CODE, 0);
				response.put(Constants.STATUS_MESSAGE, "Job " + inputDataConfig.getReport().getName() + " already exist");
			} else {
				QuartzUtil.createSchedule(inputDataConfig);
				response.put(Constants.STATUS_CODE, 1);
				response.put(Constants.STATUS_MESSAGE, "Job " + inputDataConfig.getReport().getName() + " scheduled successfully");
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
			response.put(Constants.STATUS_CODE, 0);
			response.put(Constants.STATUS_MESSAGE, "Error while scheduling Job " + inputDataConfig.getReport().getName());
		}
		return response.toString();
	}

	@POST
	@Path("mailReport")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String mailReport(InputDataConfig inputDataConfig) {
		Client esClient = (Client) context.getAttribute(Constants.ES_CLIENT);
		JSONObject response = new JSONObject();

		response = Util.validate(inputDataConfig);
		if (response.getInt(Constants.STATUS_CODE) == 0) {
			return response.toString();
		}

		inputDataConfig.getReportAccess().setScheduleReportEnabled(false);
		inputDataConfig.getReportAccess().setFileReportEnabled(false);

		ESReport esReport = new ESReport();
		esReport.setData(inputDataConfig);
		try {
			Workbook wb = esReport.process(esClient);
			response.put(Constants.STATUS_CODE, 1);
			response.put(Constants.STATUS_MESSAGE, "Mail sent successfully");
		} catch (MessagingException e) {
			response.put(Constants.STATUS_CODE, 0);
			response.put(Constants.STATUS_MESSAGE, "Error while sending mail");
			e.printStackTrace();
		} catch (IOException e) {
			response.put(Constants.STATUS_CODE, 0);
			response.put(Constants.STATUS_MESSAGE, "Error while sending mail");
			e.printStackTrace();
		}
		return response.toString();
	}

	@POST
	@Path("updateScheduleReport")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String updateScheduleReport(InputDataConfig inputDataConfig) {
		JSONObject response = new JSONObject();
		try {
			if (QuartzUtil.jobExist(inputDataConfig.getReport().getName())) {
				QuartzUtil.updateScheduleReport(inputDataConfig);
				response.put(Constants.STATUS_CODE, 1);
				response.put(Constants.STATUS_MESSAGE, "Job " + inputDataConfig.getReport().getName() + " updated successfully");
			} else {
				response.put(Constants.STATUS_CODE, 0);
				response.put(Constants.STATUS_MESSAGE, "Job " + inputDataConfig.getReport().getName() + " does not exist");
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
			response.put(Constants.STATUS_CODE, 0);
			response.put(Constants.STATUS_MESSAGE, "Error while updating job " + inputDataConfig.getReport().getName());
		}
		return response.toString();
	}

	@POST
	@Path("getScheduleConfig")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String getScheduleConfig(Map<String, String> input) {
		JSONObject response = new JSONObject();
		String reportName = input.get("reportName");
		try {
			boolean jobExist = QuartzUtil.jobExist(reportName);
			if (jobExist) {
				response.put(Constants.STATUS_CODE, 1);
				response.put(Constants.STATUS_MESSAGE, QuartzUtil.getJobData(reportName));
			} else {
				response.put(Constants.STATUS_CODE, 0);
				response.put(Constants.STATUS_MESSAGE, "Job does not exist");
			}
		} catch (SchedulerException e) {
			response.put(Constants.STATUS_CODE, 0);
			response.put(Constants.STATUS_MESSAGE, "Error while accessing the job details");
			e.printStackTrace();
		}
		return response.toString();
	}

	@POST
	@Path("runJob")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public String triggerJob(Map<String, String> input) {
		JSONObject response = new JSONObject();
		String jobName = input.get("jobName");

		try {
			boolean jobExist = QuartzUtil.jobExist(jobName);
			if (jobExist) {
				response.put(Constants.STATUS_CODE, 1);
				QuartzUtil.triggerJob(jobName);
				response.put(Constants.STATUS_MESSAGE, "Job Triggered Successfully");
			} else {
				response.put(Constants.STATUS_CODE, 0);
				response.put(Constants.STATUS_MESSAGE, "Job does not exist");
			}
		} catch (SchedulerException e) {
			response.put(Constants.STATUS_CODE, 0);
			response.put(Constants.STATUS_MESSAGE, "Error while running the job");
			e.printStackTrace();
		}
		return response.toString();
	}

	@POST
	@Path("deleteJob")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	public String deleteJob(Map<String, String> input) {
		try {
			boolean status = QuartzUtil.deleteJob(String.valueOf(input.get("jobName")));
			if (status) {
				return "Job " + input.get("jobName") + " deleted successfully";
			} else {
				return "Job " + input.get("jobName") + " not found";
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
			return "Error while deleting the job " + input.get("jobName");
		}
	}

	@GET
	@Path("listJobs")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public List<JobData> listJobs() {
		List<JobData> jobDateList = null;
		try {
			jobDateList = QuartzUtil.listJobs();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
		return jobDateList;
	}

	@POST
	@Path("updateJobState")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	public String updateJobState(Map<String, String> input) {
		String jobName = input.get("jobName");
		String triggerName = input.get("triggerName");
		boolean triggerState = Boolean.valueOf(input.get("triggerState"));
		String index = input.get(Constants.INDEX);

		TriggerState responseTriggerState = null;

		try {
			responseTriggerState = QuartzUtil.updateJobState(jobName, triggerName, triggerState);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
		if (responseTriggerState == null) {
			return "Unable to retrive the state for the job " + input.get("jobName") + "#" + index;
		}

		if (!triggerState && responseTriggerState.equals(TriggerState.NORMAL)) {
			return "Unable to pause the state for the job " + input.get("jobName") + "#" + index;
		} else if (triggerState && responseTriggerState.equals(TriggerState.PAUSED)) {
			return "Unable to resume the state for the job " + input.get("jobName") + "#" + index;
		} else if (!triggerState && responseTriggerState.equals(TriggerState.PAUSED)) {
			return "Job " + jobName + "#" + index + " paused successfully";
		} else if (triggerState && responseTriggerState.equals(TriggerState.NORMAL)) {
			return "Job " + jobName + "#" + index + " resumed successfully";
		} else {
			return "Unknown State";
		}
	}

	@POST
	@Path("getESFieldList")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public List<String> getESFieldList(Map<String, String> input) {

		Client esClient = (Client) context.getAttribute(Constants.ES_CLIENT);

		String index = input.get(Constants.INDEX);
		String type = input.get(Constants.TYPE);

		return Util.getESFields(esClient, index, type);

	}

	@POST
	@Path("getTypeListFromIndex")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public List<String> getTypeListFromIndex(Map<String, String> input) {
		String index = input.get(Constants.INDEX);
		Client esClient = (Client) context.getAttribute(Constants.ES_CLIENT);
		return Util.getTypeListFromIndex(esClient, index);
	}

	@GET
	@Path("getIndexTypeMapping")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Map<String, Object> getIndexTypeMapping() {
		Client esClient = (Client) context.getAttribute(Constants.ES_CLIENT);
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("indexTypeMapping", Util.getIndexTypeMapping(esClient));
		response.put("aliasList", Util.getESAliasList(esClient));
		response.put("indexList", Util.getESIndexList(esClient));

		return response;
	}

}