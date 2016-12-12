package com.isp.bpm.util;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jayway.jsonpath.JsonPath;


public class BPMUtil {
	

	public static String toJSONString(Object obj) {
		
		String text = new JSONObject(obj).toString(3);
	
		return text; 
	}

	public static String removeMetaData(String jsonVariables ) {
		
		String startpart=null;
		String endpart=null;
		
		if (jsonVariables !=null && jsonVariables.length()!=0 && jsonVariables.contains("\"@metadata\"")){
			
			while (jsonVariables.contains("\"@metadata\"")) {
				
				int pos0= jsonVariables.indexOf("\"@metadata\"");
				int pos1=jsonVariables.indexOf("},", pos0);
				startpart=jsonVariables.substring(0,pos0);
				endpart=jsonVariables.substring(pos1+2,jsonVariables.length());
				jsonVariables=startpart+endpart;
			}
		}
		
		return jsonVariables;
		
	}
	
	public static String taskVariables(JSONObject obj) {
		
		String query_result=null;
		
		try {
			Object variablesJson =JsonPath.read(obj.toString(3), "$.data.data.variables");
			query_result=variablesJson.toString();
		}
		catch(Exception ex){
			//Do nothing
		}
		
		return  query_result;	
	}
	
}
