package com.elastictab.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.poi.ss.usermodel.Workbook;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.json.JSONObject;
import org.quartz.CronExpression;

import com.elastictab.model.InputDataConfig;
import com.elastictab.report.ESReport;

public class Util {

	public static List<String> getESFields(String index, String type) {
		List<String> fieldList = new ArrayList<String>();
		boolean indexExist = ESReport.getESClient().admin().indices().prepareExists(index).execute().actionGet().isExists();
		ClusterStateResponse resp = ESReport.getESClient().admin().cluster().prepareState().execute().actionGet();
		boolean typeExist = resp.getState().metaData().index(index).mappings().containsKey(type);

		if (indexExist && typeExist) {
			ClusterState cs = ESReport.getESClient().admin().cluster().prepareState().setIndices(index).execute().actionGet().getState();
			IndexMetaData imd = cs.getMetaData().index(index);
			MappingMetaData mdd = imd.mapping(type);
			Map<String, Object> map = null;
			try {
				map = mdd.getSourceAsMap();
			} catch (IOException e) {
				e.printStackTrace();
			}
			fieldList = getList("", map);
		}
		return fieldList;
	}

	private static List<String> getList(String fieldName, Map<String, Object> mapProperties) {
		List<String> fieldList = new ArrayList<String>();
		Map<String, Object> map = (Map<String, Object>) mapProperties.get("properties");
		Set<String> keys = map.keySet();
		for (String key : keys) {
			if (((Map<String, Object>) map.get(key)).containsKey("type")) {
				fieldList.add(fieldName + "" + key);
			} else {
				List<String> tempList = getList(fieldName + "" + key + ".", (Map<String, Object>) map.get(key));
				fieldList.addAll(tempList);
			}
		}
		return fieldList;
	}

	public static List<String> getESIndexList() {
		String[] indexList = ESReport.getESClient().admin().cluster().prepareState().execute().actionGet().getState().getMetaData().concreteAllIndices();
		return Arrays.asList(indexList);
	}

	public static List<String> getESAliasList() {
		List<String> aliasList = new ArrayList<String>();
		ImmutableOpenMap<String, ImmutableOpenMap<String, AliasMetaData>> object = ESReport.getESClient().admin().cluster().prepareState().execute().actionGet().getState().getMetaData().getAliases();
		Object[] aliasKeyObjectArray = object.keys().toArray();

		for (Object aliasObject : aliasKeyObjectArray) {
			aliasList.add(aliasObject.toString());
		}
		return aliasList;
	}

	public static List<String> getTypeListFromIndex(String index) {
		List<String> typeList = new ArrayList<String>();
		try {
			GetMappingsResponse res = ESReport.getESClient().admin().indices().getMappings(new GetMappingsRequest().indices(index)).get();
			ImmutableOpenMap<String, MappingMetaData> mapping = res.mappings().get(index);
			for (ObjectObjectCursor<String, MappingMetaData> c : mapping) {
				typeList.add(c.key);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return typeList;
	}

	public static Map<String, List<String>> getIndexTypeMapping() {
		Map<String, List<String>> indexTypeMapping = new HashMap<String, List<String>>();
		List<String> typeList = null;

		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> f = ESReport.getESClient().admin().indices().getMappings(new GetMappingsRequest()).actionGet().getMappings();

		Object[] indexList = f.keys().toArray();
		for (Object indexObj : indexList) {
			String index = indexObj.toString();
			ImmutableOpenMap<String, MappingMetaData> mapping = f.get(index);
			typeList = new ArrayList<String>();
			for (ObjectObjectCursor<String, MappingMetaData> c : mapping) {
				typeList.add(c.key);
			}
			indexTypeMapping.put(index, typeList);
		}
		return indexTypeMapping;
	}

	public static JSONObject validate(InputDataConfig inputDataConfig) {
		JSONObject response = new JSONObject();

		if (inputDataConfig.getReportAccess().getMailReport().getMailList().size() == 0) {
			response.put(Constants.STATUS_CODE, 0);
			response.put(Constants.STATUS_MESSAGE, "Enter atleast 1 E-Mail ID");
			return response;
		}

		List<String> mailIDList = inputDataConfig.getReportAccess().getMailReport().getMailList();

		for (String mailID : mailIDList) {
			try {
				InternetAddress emailAddr = new InternetAddress(mailID);
				emailAddr.validate();
			} catch (AddressException e) {
				response.put(Constants.STATUS_CODE, 0);
				response.put(Constants.STATUS_MESSAGE, mailID + " is not a valid E-Mail ID");
				return response;
			}
		}

		if (inputDataConfig.getReportAccess().isScheduleReportEnabled()) {
			List<String> cronExpressionList = inputDataConfig.getReportAccess().getScheduleReport().getCronExpressionList();
			for (String cronExpression : cronExpressionList) {
				if (!CronExpression.isValidExpression(cronExpression)) {
					response.put(Constants.STATUS_CODE, 0);
					response.put(Constants.STATUS_MESSAGE, "CRON expression " + cronExpression + "is invalid");
					return response;
				}
			}
		}

		response.put(Constants.STATUS_CODE, 1);
		return response;
	}

	public static byte[] WorkbookToByteArray(Workbook wb) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			wb.write(bos);
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
		return bos.toByteArray();
	}
}