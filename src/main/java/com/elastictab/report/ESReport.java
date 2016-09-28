package com.elastictab.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.json.JSONArray;
import org.json.JSONObject;

import com.elastictab.model.FileReport;
import com.elastictab.model.InputDataConfig;
import com.elastictab.model.MailReport;
import com.elastictab.model.ReportColumnConfig;
import com.elastictab.util.Constants;
import com.elastictab.util.MailUtil;

public class ESReport {

	static Client esClient;
	
	MailUtil mailAPI = new MailUtil();

	public static void initializeESClient() {
		String hostname = "localhost";
		String jarPath = System.getProperties().getProperty("user.dir");

		Properties properties = new Properties();
		try {
			InputStream input = new FileInputStream(jarPath + File.separatorChar + "properties" + File.separatorChar + "elasticsearch.properties");
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!properties.get("hostname").equals(null) && !properties.get("hostname").equals("")) {
			hostname = (String) properties.get("hostname");
		}

		//Builder builder = ImmutableSettings.settingsBuilder();
		Builder builder = Settings.settingsBuilder();
		
		
		builder.put("client.transport.sniff", true);
		if (!properties.get("clustername").equals(null) && !properties.get("clustername").equals("")) {
			builder.put("cluster.name", (String) properties.get("clustername"));
		}

		Settings settings = builder.build();		
		try {
			esClient = TransportClient.builder().settings(settings).build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostname), 9300));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public static Client getESClient() {
		return esClient;
	}

	Properties prop = new Properties();

	int i = 0;
	Workbook wb = new XSSFWorkbook();
	Sheet sheet;
	Row row;
	Cell cell;
	Font font = wb.createFont();
	CellStyle dataStyle;
	CellStyle titleStyle;
	CellStyle headerStyle;
	String[] fields;

	ScriptEngineManager mgr = new ScriptEngineManager();
	ScriptEngine engine = mgr.getEngineByName("JavaScript");

	int rownumber = 0;

	InputDataConfig inputDataConfig;
	List<ReportColumnConfig> reportColumnConfigList;

	// INPUT PARAMETERS
	String config;
	String nullValue;
	JSONObject queryObj;
	Map<String, Object> customMapping;

	int k = 0;
	long hitscount = 0;
	int rows_fetched = 0;
	int y = 0;

	ESReport() {
		setStyles();
		k = 0;
		hitscount = 0;
		rows_fetched = 0;
	}

	private void initializeDefaultParameters() {

		String jarPath = System.getProperties().getProperty("user.dir");

		try {
			InputStream input = new FileInputStream(jarPath + File.separatorChar + "properties" + File.separatorChar + "elastictab.properties");
			prop.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		FileReport fileReport = new FileReport();
		inputDataConfig.getReportAccess().setFileReport(fileReport);

		inputDataConfig.getReportAccess().getFileReport().setSaveLocation(prop.getProperty("save.location"));

		if (prop.getProperty("batch.size") != null && StringUtils.isNumeric(prop.getProperty("batch.size"))) {
			inputDataConfig.getElasticsearch().setBatchSize(Integer.valueOf(prop.getProperty("batch.size")));
		}
	}

	public void setData(InputDataConfig inputDataConfig) {
		this.inputDataConfig = inputDataConfig;
		nullValue = inputDataConfig.getNullValue();
		this.inputDataConfig.getReportAccess().getMailReport().setDescription(inputDataConfig.getReport().getDescription());
		reportColumnConfigList = inputDataConfig.getReportColumnConfig();
		ObjectMapper mapper = new ObjectMapper();

		// customMapping = inputDataConfig.getElasticsearch().getValueMapping();
		if (inputDataConfig.getElasticsearch().getValueMapping() != null) {
			try {
				customMapping = mapper.readValue(inputDataConfig.getElasticsearch().getValueMapping(), new TypeReference<Map<String, Object>>() {
				});
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		queryObj = new JSONObject(inputDataConfig.getElasticsearch().getQuery());
		
		fields = getFields(queryObj.getJSONArray(Constants.FIELDS));

		initializeDefaultParameters();
	}

	private void setStyles() {
		setDataStyle();
		setTitleStyle();
		setHeaderStyle();
	}

	private void setHeaderStyle() {
		headerStyle = wb.createCellStyle();
		font.setFontHeightInPoints((short) 11);
		font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		font.setColor(HSSFColor.WHITE.index);
		headerStyle.setFont(font);
		headerStyle.setFillForegroundColor(HSSFColor.LIGHT_BLUE.index);
		headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
	}

	private void setTitleStyle() {
		titleStyle = wb.createCellStyle();
		font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		font.setColor(HSSFColor.WHITE.index);
		font.setFontHeightInPoints((short) 14);
		titleStyle.setFont(font);
		titleStyle.setFillForegroundColor(HSSFColor.LIGHT_BLUE.index);
		titleStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
	}

	private void setDataStyle() {
		dataStyle = wb.createCellStyle();
		dataStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		dataStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);
		dataStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);
		dataStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);
	}

	public Workbook process() throws MessagingException, IOException {
		Workbook wb = createWorkbook();
		reportAccess(wb);
		return wb;
	}

	private Workbook createWorkbook() {
		i = 0;
		System.out.println("Process Started");
		sheet = wb.createSheet(inputDataConfig.getReport().getName());

		setTitle();
		setHeaders();

		System.out.println("Building Excel Report");
		SearchResponse response = null;

		String index = inputDataConfig.getElasticsearch().getIndex();
		if (inputDataConfig.getElasticsearch().isUseAlias()) {
			index = inputDataConfig.getElasticsearch().getAlias();
		}
		SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch(index).setTypes(inputDataConfig.getElasticsearch().getType()).setSource(queryObj.toString());
		if (inputDataConfig.getElasticsearch().getRouting() != null) {
			searchRequestBuilder.setRouting((String[]) inputDataConfig.getElasticsearch().getRouting().toArray());
		}
		
		//searchRequestBuilder.setFrom(queryObj.getInt("from"));
		searchRequestBuilder.setScroll(new TimeValue(60000));
		searchRequestBuilder.setSize(inputDataConfig.getElasticsearch().getBatchSize());
		searchRequestBuilder.addFields(fields);

		response = searchRequestBuilder.execute().actionGet();
		while (true) {
			SearchHits hits = response.getHits();
			hitscount = hits.totalHits();
			buildDataLayout(hits);
			
			System.out.println("Processed " + Integer.valueOf((inputDataConfig.getElasticsearch().getBatchSize() * k) + inputDataConfig.getElasticsearch().getBatchSize()) + " of " + hitscount);
			k++;
			response = esClient.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
		    if (response.getHits().getHits().length == 0) {
		        break;
		    }
		}
		
		/*
		do {
			queryObj.put("from", inputDataConfig.getElasticsearch().getBatchSize() * k);
			SearchResponse response = null;

			String index = inputDataConfig.getElasticsearch().getIndex();
			if (inputDataConfig.getElasticsearch().isUseAlias()) {
				index = inputDataConfig.getElasticsearch().getAlias();
			}

			SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch(index).setTypes(inputDataConfig.getElasticsearch().getType()).setSource(queryObj.toString());
			if (inputDataConfig.getElasticsearch().getRouting() != null) {
				searchRequestBuilder.setRouting((String[]) inputDataConfig.getElasticsearch().getRouting().toArray());
			}
			
			searchRequestBuilder.setFrom(queryObj.getInt("from"));
			searchRequestBuilder.setSize(inputDataConfig.getElasticsearch().getBatchSize());
			searchRequestBuilder.addFields(fields);

			response = searchRequestBuilder.execute().actionGet();

			SearchHits hits = response.getHits();
			hitscount = hits.totalHits();
	
			buildDataLayout(hits);
			System.out.println("Processed " + Integer.valueOf((inputDataConfig.getElasticsearch().getBatchSize() * k) + inputDataConfig.getElasticsearch().getBatchSize()) + " of " + hitscount);
			k++;
			rows_fetched = inputDataConfig.getElasticsearch().getBatchSize() * k;
		} while (rows_fetched < hitscount);
		System.out.println("Finished processing data");
		*/
		formatExcelSheet();

		return wb;
	}

	private static String[] getFields(JSONArray jsonArray) {
		String[] fields = new String[jsonArray.length()];
		for(int i=0;i<jsonArray.length();i++){
			fields[i]=jsonArray.getString(i);			
		}
		return fields;
	}

	private void setTitle() {
		System.out.println("Setting Title and Headers");
		row = sheet.createRow(rownumber);
		rownumber++;

		cell = row.createCell((short) 0);
		cell.setCellValue(inputDataConfig.getReport().getName());
		cell.setCellStyle(titleStyle);

		for (int i = 1; i < inputDataConfig.getReportColumnConfig().size(); i++) {
			cell = row.createCell((short) i);
			cell.setCellStyle(titleStyle);
		}
	}

	private void setHeaders() {
		List<ReportColumnConfig> reportColumnConfigList = inputDataConfig.getReportColumnConfig();

		for (int i = 1; i < reportColumnConfigList.size(); i++) {
			cell = row.createCell((short) i);
			cell.setCellStyle(titleStyle);
		}

		row = sheet.createRow(rownumber);
		for (int i = 0; i < reportColumnConfigList.size(); i++) {
			cell = row.createCell((short) i);
			ReportColumnConfig reportColumnConfig = reportColumnConfigList.get(i);
			cell.setCellValue(reportColumnConfig.getTitle());
			cell.setCellStyle(headerStyle);
		}
		rownumber++;

	}

	private void buildDataLayout(SearchHits hits) {
		// For each row
		for (int i = 0; i < hits.getHits().length; i++) {
			// Row n
			Map<String, SearchHitField> responseFields = hits.getAt(i).getFields();
			row = sheet.createRow(rownumber);
			for (int j = 0; j < reportColumnConfigList.size(); j++) {
				ReportColumnConfig reportColumnConfig = reportColumnConfigList.get(j);
				String format = reportColumnConfig.getExpression();
				format = getExprValue(responseFields, format);
	
				cell = row.createCell((short) j);
				cell.setCellValue(format);
			}
			rownumber++;
		}
	}

	private String getExprValue(Map<String, SearchHitField> responseFields, String format) {
		String exprTemp = format;
		int exprIndexSize = 0;

		int startIndexCount = StringUtils.countMatches(exprTemp, "[");
		int endIndexCount = StringUtils.countMatches(exprTemp, "]");

		if (startIndexCount == endIndexCount) {
			exprIndexSize = startIndexCount;
		}

		for (int i = 0; i < exprIndexSize; i++) {
			Map<String, Integer> exprIndex = getExprIndex(exprTemp);
			String elementeryExpr = exprTemp.substring(exprIndex.get(Constants.START_INDEX) + 1, exprIndex.get(Constants.END_INDEX));

			String[] elementeryExprArray = elementeryExpr.split(",");

			// 0 getValue
			// 1 getDerivedValue
			// 2 Length
			// 3 Format Number Length
			// 4 Sub String
			// 5 Character at index
			// 6 Calculate
			// 7 Range
			// 8 Array indexOf(int value)
			// 9 Array indexOf(String value)
			// 10 Array valueAt(index)

			if (elementeryExprArray[0].equals("0")) {
				String t = getValue(responseFields, elementeryExprArray[1]);
				t = getExprValue(responseFields, t);
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), t);
			}

			if (elementeryExprArray[0].equals("1")) {
				String t = getDerivedValue(responseFields, elementeryExprArray[1], elementeryExprArray[2]);
				t = getExprValue(responseFields, t);
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), t);
			}

			if (elementeryExprArray[0].equals("2")) {
				String t = getStringLength(elementeryExprArray[1]);
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), t);
			}

			if (elementeryExprArray[0].equals("3")) {
				String t = getFormatNumberLength(elementeryExprArray[1], Integer.valueOf(elementeryExprArray[2]));
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), t);
			}

			if (elementeryExprArray[0].equals("4")) {
				String t = getSubString(elementeryExprArray[1], Integer.valueOf(elementeryExprArray[2]), Integer.valueOf(elementeryExprArray[3]));
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), t);
			}

			if (elementeryExprArray[0].equals("5")) {
				String t = getCharacter(elementeryExprArray[1], Integer.valueOf(elementeryExprArray[2]));
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), t);
			}

			if (elementeryExprArray[0].equals("6")) {
				String t = getComputedString(elementeryExprArray[1]);
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), t);
			}

			if (elementeryExprArray[0].equals("7")) {
				String t = getRange(elementeryExprArray[1], elementeryExprArray[2]);
				t = getExprValue(responseFields, t);
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), t);
			}

			if (elementeryExprArray[0].equals("8")) {
				String t = getArrayIndexOf(responseFields, elementeryExprArray[1], Integer.valueOf(elementeryExprArray[2]));
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), String.valueOf(t));
			}

			if (elementeryExprArray[0].equals("9")) {
				String t = getArrayIndexOf(responseFields, elementeryExprArray[1], elementeryExprArray[2]);
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), String.valueOf(t));
			}

			if (elementeryExprArray[0].equals("10")) {
				String t = getArrayValueAt(responseFields, elementeryExprArray[1], Integer.valueOf(elementeryExprArray[2]));
				exprTemp = exprTemp.replaceFirst(Pattern.quote(exprTemp.substring(exprIndex.get(Constants.START_INDEX), exprIndex.get(Constants.END_INDEX) + 1)), t);
			}
		}

		return exprTemp;
	}

	// ProcessType: 0
	private String getValue(Map<String, SearchHitField> responseFields, String fieldName) {
		if (responseFields.containsKey(fieldName)) {
			SearchHitField fieldValueObj = responseFields.get(fieldName);
			return fieldValueObj.getValue().toString();
		} else {
			return nullValue;
		}
	}

	// ProcessType: 1
	private String getDerivedValue(Map<String, SearchHitField> responseFields, String valueMappingKey, String value) {
		if (customMapping.containsKey(valueMappingKey)) {
			Map<String, Object> tempCustomMapping = (Map<String, Object>) customMapping.get(valueMappingKey);

			if (tempCustomMapping.containsKey(value)) {
				return String.valueOf(tempCustomMapping.get(value));
			} else if (tempCustomMapping.containsKey("default")) {
				return String.valueOf(tempCustomMapping.get("default"));
			} else {
				return nullValue;
			}
		} else {
			return nullValue;
		}
	}

	// ProcessType: 2
	private String getStringLength(String fieldValue) {
		if (!fieldValue.equals(nullValue)) {
			return String.valueOf(fieldValue.length());
		} else {
			return nullValue;
		}
	}

	// ProcessType: 3
	private String getFormatNumberLength(String fieldValue, Integer formatNumberLength) {
		String format = StringUtils.repeat("0", formatNumberLength);
		DecimalFormat mFormat = new DecimalFormat(format);
		if (StringUtils.isNumeric(fieldValue)) {
			return mFormat.format(Integer.valueOf(fieldValue));
		} else {
			return nullValue;
		}
	}

	// ProcessType: 4
	private String getSubString(String fieldValue, int from, int end) {
		if (!fieldValue.equals("-")) {
			return fieldValue.substring(from, end);
		} else {
			return fieldValue;
		}
	}

	// ProcessType: 5
	private String getCharacter(String fieldValue, int index) {
		if (index < fieldValue.length() && !fieldValue.equals(nullValue)) {
			return String.valueOf(fieldValue.charAt(index));
		} else {
			return nullValue;
		}
	}

	// ProcessType: 6
	private String getComputedString(String fieldValue) {
		if (!fieldValue.equals(nullValue) && !fieldValue.equals("")) {
			try {
				return String.valueOf(engine.eval(fieldValue));
			} catch (ScriptException e) {
				return nullValue;
			}
		}
		return nullValue;
	}

	// ProcessType: 7
	private String getRange(String valueMappingKey, String fieldValue) {
		if (customMapping.containsKey(valueMappingKey)) {
			Map<String, Object> tempCustomMapping = (Map<String, Object>) customMapping.get(valueMappingKey);

			if (!fieldValue.equals(nullValue) && !fieldValue.equals("")) {
				Set<String> keys = tempCustomMapping.keySet();

				for (String key : keys) {
					String keyTemp = key;
					key = key.replace("x", fieldValue);
					try {
						if ((Boolean) engine.eval(key)) {
							return String.valueOf(tempCustomMapping.get(keyTemp));
						}
					} catch (ScriptException e) {
						return nullValue;
					}
				}

			}
			if (tempCustomMapping.containsKey("default")) {
				return String.valueOf(tempCustomMapping.get("default"));
			}
			return nullValue;
		} else {
			return nullValue;
		}
	}

	// ProcessType: 8
	private String getArrayIndexOf(Map<String, SearchHitField> responseFields, String fieldName, int value) {
		try {
			return String.valueOf(responseFields.get(fieldName).getValues().indexOf(value));
		} catch (Exception e) {
			return nullValue;
		}
	}

	// ProcessType: 9
	private String getArrayIndexOf(Map<String, SearchHitField> responseFields, String fieldName, String value) {
		try {
			return String.valueOf(responseFields.get(fieldName).getValues().indexOf(value));
		} catch (Exception e) {
			return nullValue;
		}
	}

	// ProcessType 10
	private String getArrayValueAt(Map<String, SearchHitField> responseFields, String fieldName, int arrayIndex) {
		try {
			return String.valueOf(responseFields.get(fieldName).getValues().get(arrayIndex));
		} catch (Exception e) {
			return nullValue;
		}
	}

	private Map<String, Integer> getExprIndex(String exprTemp) {
		int startIndex = 0;
		int endIndex = 0;

		for (int i = 0; i < exprTemp.length(); i++) {
			if (exprTemp.substring(i, i + 1).equals("[")) {
				startIndex = i;
				continue;
			}
			if (exprTemp.substring(i, i + 1).equals("]")) {
				endIndex = i;
				break;
			}
		}

		Map<String, Integer> exprIndex = new HashMap<String, Integer>();
		exprIndex.put(Constants.START_INDEX, startIndex);
		exprIndex.put(Constants.END_INDEX, endIndex);

		return exprIndex;
	}

	public void reportAccess(Workbook wb) throws MessagingException, IOException {
		DecimalFormat mFormat = new DecimalFormat("00");
		Calendar date = new GregorianCalendar();
		String fileName = inputDataConfig.getReport().getFileName();

		fileName += "_" + date.get(Calendar.YEAR) + mFormat.format(Integer.valueOf(date.get(Calendar.MONTH) + 1)) + date.get(Calendar.DAY_OF_MONTH) + "_"
				+ mFormat.format(date.get(Calendar.HOUR_OF_DAY)) + mFormat.format(date.get(Calendar.MINUTE));

		if (inputDataConfig.getReportAccess().isFileReportEnabled()) {
			System.out.println("Saving file for FTP access");
			reportAccessTypeFile(wb, inputDataConfig.getReportAccess().getFileReport().getSaveLocation(), fileName);

		}
		if (inputDataConfig.getReportAccess().isMailReportEnabled()) {
			System.out.println("Sending E-Mail...");
			reportAccessTypeEMail(wb, inputDataConfig.getReportAccess().getMailReport(), fileName);
		}
	}

	public void reportAccessTypeEMail(Workbook localwb, MailReport email, String fileName) throws MessagingException, IOException {		
		// mailAPI.setFrom(fromEMail);
		if (email.getMailList().size() > 0) {
			mailAPI.initializeMessage();
			mailAPI.setSubject(email.getSubject());
			mailAPI.setText(email.getDescription());
			mailAPI.addRecipients(email.getMailList());
			mailAPI.attachWB(localwb, fileName);
			mailAPI.send();
			System.out.println("E-Mail Sent");
		}
	}

	public void reportAccessTypeFile(Workbook localWB, String reportSavePath, String filename) {
		FileOutputStream out;
		try {
			out = new FileOutputStream(reportSavePath + File.separatorChar + filename + ".xls");
			localWB.write(out);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void formatExcelSheet() {
		for (int i = 0; i < reportColumnConfigList.size(); i++) {
			try {
				sheet.autoSizeColumn((short) i);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
