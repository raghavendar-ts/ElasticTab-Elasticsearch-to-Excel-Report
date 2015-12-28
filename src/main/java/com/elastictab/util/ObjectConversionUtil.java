package com.elastictab.util;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;

import com.elastictab.model.InputDataConfig;

public class ObjectConversionUtil {
	public static InputDataConfig stringToPOJO(String inputDataConfigString) {
		InputDataConfig inputDataConfig = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			inputDataConfig = mapper.readValue(inputDataConfigString, InputDataConfig.class);

		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return inputDataConfig;
	}

	public static String POJOToJSON(InputDataConfig inputDataConfig) {
		ObjectMapper mapper = new ObjectMapper();
		String json = null;
		try {
			json = mapper.writeValueAsString(inputDataConfig);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return json;
	}

	public static String MapToJSONString(Map<String, Object> jobData) {
		JSONObject json = new JSONObject(jobData);
		return json.getString("inputDataConfigString");
	}

}
