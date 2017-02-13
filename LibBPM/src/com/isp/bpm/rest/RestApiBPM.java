package com.isp.bpm.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.isp.bpm.commons.Messages;
import com.isp.bpm.exception.LIBBPMException;
import com.isp.bpm.util.BPMUtil;
import com.jayway.jsonpath.JsonPath;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

/**
 * <h4>The class contains a series of methods that wraps specific api rest to
 * interact with the IBM BPM product</h4>
 * <p>
 * The BPM connection credential are passed inside an object of type :
 * ConnectionDataBeanSingleton
 * <p>
 * 
 * 
 * @author Primeur
 * @version 1.0
 * 
 */

// java.net.URL wsURL = new URL(null, wsLoc + “?command=login”,new
// sun.net.www.protocol.https.Handler());

// ---------------------->personale

public class RestApiBPM {

	public static final String aboutLib = "LibBPM V2.6";

	private ConnectionDataBeanSingleton cdb;

	public RestApiBPM() {

	}

	/**
	 * 
	 * BPM connection data rappresented by an object of class
	 * ConnectionDataBeanSingleton are passed during object construction.
	 * 
	 */
	public RestApiBPM(ConnectionDataBeanSingleton cdb) {
		super();
		this.cdb = cdb;
	}

	public String aboutLib() {
		return aboutLib;
	}

	private static String formatError(Exception e) {

		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));

		return sw.toString();
	}

	/**
	 * Start a particular service
	 * 
	 * @param String
	 *            ispHeader
	 * @param Object
	 *            serviceInputBO
	 * @param String
	 *            serviceName
	 * @return Json response
	 * @throws UnsupportedEncodingException
	 * 
	 *             <p>
	 *             <h4>Example:</h4>
	 *             <p>
	 *             JSONObject jo=null;
	 *             <p>
	 *             RestApiBPM ra=new RestApiBPM(cdb);
	 *             <p>
	 *             jo=ra.startService(ispHeader,serviceInputBO,
	 *             "EMON%40Visualizza%20Richiesta")
	 *             <p>
	 * 
	 *             if the serviceInputBO is not present pass null
	 *             <p>
	 */
	public JSONObject startService(String ispHeader, Object serviceInputBO, String serviceName, String user_,
			String password_) throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		Client client = null;
		JSONObject obj = null;

		String inputVar = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (serviceInputBO != null) {

			inputVar = BPMUtil.toJSONString(serviceInputBO);

		}

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			if (isHeaderISPinBO(inputVar)) {

				try {

					String userPassword = null;
					String encoding = null;

					userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
							+ RestApiBPM.getEffectivePassword(password, password_);
					encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

					String headerMessageValidation = checkISPHeader(ispHeader);

					sb.append("<function><startService>").append("<params>")
							.append("<BusinessObject><![CDATA[").append(inputVar).append("]]>")
							.append("</BusinessObject>").append("<serviceName>").append(serviceName)
							.append("</serviceName>").append("</params>").append("</startService></function>");

					if (headerMessageValidation.length() == 0) {

						// normalize Ts in ispHeader
						ispHeader = ispHeader.replaceAll(
								"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>", "<Timestamp>"
										+ RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

						Form form = new Form();
						form.param("action", "start");
						form.param("parts", "all");

						if (!(inputVar == null || inputVar.isEmpty()))
							form.param("params", inputVar);

						client = ClientBuilder.newClient();
						String result = null;

						result = client.target(urlServer).path("/rest/bpm/wle/v1/service/" + serviceName)
								.request("application/json").header("Authorization", "Basic " + encoding)
								.header("X-ISPWebServicesHeader", ispHeader)
								.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE),
										java.lang.String.class);

						obj = new JSONObject(result);

						sb1.append("<![CDATA[").append(result).append("]]>");

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
								formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""),
								this.cdb);

					} else {
						obj = new JSONObject(headerMessageValidation);

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
								sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

						throw new LIBBPMException(headerMessageValidation);
					}

				} catch (Exception e) {

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

					throw new LIBBPMException(RestApiBPM.formatError(e));

				} finally {

					if (client != null)
						client.close();
				}
			} else {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
						formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_4),
						this.cdb);

				throw new LIBBPMException(Messages.ERROR_4);
			}

		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" startService");
		}

		return obj;
	}

	/**
	 * Get process data filtered by process application-acronym set as default
	 * (isDefault=True)
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            processAcronym
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Esempio di utilizzo:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.exposedProcessbyDefaultAcronym(ispHeader,"FIN");
	 *         <p>
	 * @throws LIBBPMException
	 */

	@SuppressWarnings("unchecked")
	public JSONObject exposedProcessByDefaultAcronym(String ispHeader, String processAcronym, String user_,
			String password_)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		Client client = null;
		JSONObject obj = null;

		// /rest/bpm/wle/v1/exposed/process

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><exposedProcessByDefaultAcronym>").append("<params>")
						.append("<processAcronym>").append(processAcronym).append("</processAcronym>")
						.append("</params>").append("</exposedProcessByDefaultAcronym></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					client = ClientBuilder.newClient();
					String result_ = null;

					result_ = client.target(urlServer).path("/rest/bpm/wle/v1/exposed/process")
							.request("application/json") // Expected
							// response mime
							// type
							.header("Authorization", "Basic " + encoding).header("X-ISPWebServicesHeader", ispHeader)
							.get(java.lang.String.class);

					obj = new JSONObject(result_);

					String xml = XML.toString(obj);

					xml = "<start_appended>" + xml + "</start_appended>";

					InputSource source = new InputSource(new StringReader(xml));

					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

					DocumentBuilder db = dbf.newDocumentBuilder();

					Document document = db.parse(source);

					XPathFactory xpathFactory = XPathFactory.newInstance();

					XPath xpath = xpathFactory.newXPath();

					XPathExpression expr = xpath.compile("/start_appended/data/exposedItemsList");

					Object result = expr.evaluate(document, XPathConstants.NODESET);

					NodeList nodes = (NodeList) result;

					@SuppressWarnings("rawtypes")
					HashMap service = new HashMap();

					@SuppressWarnings("rawtypes")
					HashMap queryService = new HashMap();

					for (int i = 0; i < nodes.getLength(); i++) {

						if (null != nodes.item(i)) {

							NodeList nodeList = nodes.item(i).getChildNodes();

							for (int k = 0; null != nodeList && k < nodeList.getLength(); k++) {

								Node nod = nodeList.item(k);

								if (nod.getNodeType() == Node.ELEMENT_NODE)

									service.put(nodeList.item(k).getNodeName(), nod.getFirstChild().getNodeValue());

							}

							if (service.get("processAppAcronym").equals(processAcronym)
									& service.get("isDefault").equals("true")) {

								queryService.put("processAppAcronym", service);

								service = new HashMap<String, Boolean>();

							}

							service.clear();

						}
					}

					if (queryService.size() != 0) {

						obj = new JSONObject(queryService);

						sb1.append("<![CDATA[").append(queryService).append("]]>");

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
								formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""),
								this.cdb);

					} else {

						obj = new JSONObject("{}");

						sb1.append("<![CDATA[").append("{}").append("]]>");

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
								formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""),
								this.cdb);
					}

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}
			}

			catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			}

			finally {

				if (client != null)
					client.close();
			}

		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" exposedProcessByDefaultAcronym");
		}

		return obj;
	}

	private String formatXmltraceData(String ispHeader, String input, String output, String executionTS, String error) {

		StringBuffer sb = new StringBuffer();
		sb.append("<trace>").append("<header>").append(ispHeader).append("</header>");
		sb.append("<otherDataInput>").append("<action>").append(input).append("</action>").append("</otherDataInput>");
		// sb.append("<output><response><![CDATA[").append(output).append("]]></response>");
		sb.append("<output><response>").append(output).append("</response>");
		if (error != null && error.length() != 0) {
			sb.append("<errorMessage><![CDATA[").append(error).append("]]></errorMessage>");
			sb.append("</output>");
			sb.append("<returnCode>KO</returnCode>");
		} else {
			sb.append("</output>");
			sb.append("<returnCode>OK</returnCode>");
		}
		sb.append("<messageType>BPM</messageType>");
		sb.append("<hostName>").append(RestApiBPM.getHostName()).append("</hostName>")
				.append("<environment></environment>").append("<libVersion>").append(aboutLib)
				.append("</libVersion>");
		sb.append("<initialtimeStamp>").append(executionTS).append("</initialtimeStamp>");
		sb.append("<finaltimeStamp>").append(new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date()))
				.append("</finaltimeStamp>");
		sb.append("</trace>");

		return sb.toString();

	}

	/**
	 * Get process data filtered by process application-acronym and snapshot
	 * version (that is the name of snapshot)
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            processAcronym
	 * @param String
	 *            snapshotversion
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.xposedProcessbyAcronymAndVersionName(ispHeader,"FIN",
	 *         "version1");
	 *         <p>
	 * @throws LIBBPMException
	 */

	// /////////////////////////////////////////////////////////
	@SuppressWarnings("unchecked")
	public JSONObject exposedProcessbyAcronymAndVersionName(String ispHeader, String processAcronym,
			String snapshotversion, String user_, String password_)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		Client client = null;
		JSONObject obj = null;

		// "/rest/bpm/wle/v1/exposed/process"

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String userPassword = null;
				String encoding = null;
				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><exposedProcessbyAcronymAndVersionName>").append("<params>")
						.append("<processAcronym>").append(processAcronym).append("</processAcronym>")
						.append("<snapShotVersion>").append(snapshotversion).append("</snapShotVersion>")
						.append("</params></exposedProcessbyAcronymAndVersionName></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					client = ClientBuilder.newClient();
					String result_ = null;

					result_ = client.target(urlServer).path("/rest/bpm/wle/v1/exposed/process")
							.request("application/json") // Expected
							// response
							// mime type
							.header("Authorization", "Basic " + encoding).header("X-ISPWebServicesHeader", ispHeader)
							.get(java.lang.String.class);

					obj = new JSONObject(result_);

					String xml = XML.toString(obj);

					xml = "<start_appended>" + xml + "</start_appended>";

					InputSource source = new InputSource(new StringReader(xml));

					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

					DocumentBuilder db = dbf.newDocumentBuilder();

					Document document = db.parse(source);

					XPathFactory xpathFactory = XPathFactory.newInstance();

					XPath xpath = xpathFactory.newXPath();

					XPathExpression expr = xpath.compile("/start_appended/data/exposedItemsList");

					Object result = expr.evaluate(document, XPathConstants.NODESET);

					NodeList nodes = (NodeList) result;

					@SuppressWarnings("rawtypes")
					HashMap service = new HashMap();

					@SuppressWarnings("rawtypes")
					HashMap queryService = new HashMap();

					for (int i = 0; i < nodes.getLength(); i++) {

						if (null != nodes.item(i)) {

							NodeList nodeList = nodes.item(i).getChildNodes();

							for (int k = 0; null != nodeList && k < nodeList.getLength(); k++) {

								Node nod = nodeList.item(k);

								if (nod.getNodeType() == Node.ELEMENT_NODE)

									service.put(nodeList.item(k).getNodeName(), nod.getFirstChild().getNodeValue());

							}

							if (service.get("processAppAcronym").equals(processAcronym)
									&& service.get("snapshotName") != null
									&& service.get("snapshotName").equals(snapshotversion)) {

								queryService.put("processAppAcronym", service);

								service = new HashMap<String, Boolean>();
							}
							service.clear();

						}
					}

					if (queryService.size() != 0) {

						obj = new JSONObject(queryService);

						sb1.append("<![CDATA[").append(queryService).append("]]>");

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
								formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""),
								this.cdb);

					} else {
						obj = new JSONObject("{}");

						sb1.append("<![CDATA[").append("{}").append("]]>");

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
								formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""),
								this.cdb);
						;
					}
				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}
			} catch (Exception e) {

				sb1.append("<output>").append("<![CDATA[").append(RestApiBPM.formatError(e)).append("]]></output>");

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (client != null)
					client.close();
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8 + " exposedProcessbyAcronymAndVersionName");
		}

		return obj;
	}

	/**
	 * Get process data filtered by snapshtotID
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            snapshotId
	 * @return Json response
	 * 
	 *         < p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.exposedProcessbySnapShotID(ispHeader,
	 *         "2064.9edce2b6-0a2c-4868-a35f-4b9f90fd11c4");
	 *         <p>
	 * @throws LIBBPMException
	 */

	@SuppressWarnings("unchecked")
	public JSONObject exposedProcessbySnapShotID(String ispHeader, String snapshotid, String user_, String password_)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		Client client = null;
		JSONObject obj = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			// /rest/bpm/wle/v1/exposed/process

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><exposedProcessbySnapShotID>").append("<params>").append("<snapshotId>")
						.append(snapshotid).append("</snapshotId>").append("</params>").append("</exposedProcessbySnapShotID></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					client = ClientBuilder.newClient();
					String result_ = null;

					result_ = client.target(urlServer).path("/rest/bpm/wle/v1/exposed/process")
							.request("application/json") // Expected
							// response
							// mime type
							.header("Authorization", "Basic " + encoding).header("X-ISPWebServicesHeader", ispHeader)
							.get(java.lang.String.class);

					obj = new JSONObject(result_);

					String xml = XML.toString(obj);

					xml = "<start_appended>" + xml + "</start_appended>";

					InputSource source = new InputSource(new StringReader(xml));

					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

					DocumentBuilder db = dbf.newDocumentBuilder();

					Document document = db.parse(source);

					XPathFactory xpathFactory = XPathFactory.newInstance();

					XPath xpath = xpathFactory.newXPath();

					XPathExpression expr = xpath.compile("/start_appended/data/exposedItemsList");

					Object result = expr.evaluate(document, XPathConstants.NODESET);

					NodeList nodes = (NodeList) result;

					@SuppressWarnings("rawtypes")
					HashMap service = new HashMap();

					@SuppressWarnings("rawtypes")
					HashMap queryService = new HashMap();

					for (int i = 0; i < nodes.getLength(); i++) {

						if (null != nodes.item(i)) {

							NodeList nodeList = nodes.item(i).getChildNodes();

							for (int k = 0; null != nodeList && k < nodeList.getLength(); k++) {

								Node nod = nodeList.item(k);

								if (nod.getNodeType() == Node.ELEMENT_NODE)

									service.put(nodeList.item(k).getNodeName(), nod.getFirstChild().getNodeValue());

							}

							if (service.get("snapshotID") != null && service.get("snapshotID").equals(snapshotid)) {

								queryService.put("processAppAcronym", service);
								service = new HashMap<String, Boolean>();

							}

							service.clear();

						}
					}

					if (queryService.size() != 0) {

						obj = new JSONObject(queryService);

						sb1.append("<![CDATA[").append(queryService).append("]]>");

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
								formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""),
								this.cdb);

					} else {
						obj = new JSONObject("{}");

						sb1.append("<![CDATA[").append("{}").append("]]>");

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
								formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""),
								this.cdb);
					}

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}
			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (client != null)
					client.close();
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" exposedProcessbySnapShotID");
		}

		return obj;
	}

	/**
	 * Get all processes exposed by BPM
	 *
	 * @param String
	 *            ispHeader
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.exposedProcess(ispHeader);
	 *         <p>
	 * @throws LIBBPMException
	 */

	public JSONObject exposedProcesses(String ispHeader, String user_, String password_) throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		Client client = null;
		JSONObject obj = null;

		// /rest/bpm/wle/v1/exposed/process";

		HttpsURLConnection con = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><exposedProcesses>").append("<params>").append("</params>")
						.append("</exposedProcesses></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					client = ClientBuilder.newClient();
					String result = null;

					result = client.target(urlServer).path("/rest/bpm/wle/v1/exposed/process")
							.request("application/json") // Expected
							// response
							// mime type
							.header("Authorization", "Basic " + encoding).header("X-ISPWebServicesHeader", ispHeader)
							.get(java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}

			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (con != null)
					con.disconnect();
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8 + " exposedProcesses");
		}

		return obj;

	}

	/**
	 * Return list of all task available owned by the current user and
	 * associated to a specific application name
	 * 
	 * @param String
	 *            ispHeader
	 * @param int
	 *            countMax
	 * @param String
	 *            applicationName
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.getAssesAvailableTaksList(ispHeader,500,"my application");
	 *         <p>
	 * @throws LIBBPMException,UnsupportedEncodingException
	 */

	public JSONObject getAssesAvailableTaksList(String ispHeader, int countMax, String application_name, String user_,
			String password_) throws LIBBPMException, UnsupportedEncodingException {

		return executeQueryIBMDEFAULTALLTASKSLIST_75("ASSESS_AVAILABLE", ispHeader, countMax, application_name,
				"ASSESS_AVAILABLE", false, user_, password_);
	}

	/**
	 * Return list of all task on work owned by the current user and associated
	 * to a specific application name
	 * 
	 * @param String
	 *            ispHeader
	 * @param int
	 *            countMax
	 * @param String
	 *            applicationName
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.getWorkOnTaskList(ispHeader,500,"my application");
	 *         <p>
	 * @throws LIBBPMException,UnsupportedEncodingException
	 */
	public JSONObject getWorkOnTaskList(String ispHeader, int countMax, String application_name, String user_,
			String password_) throws LIBBPMException, UnsupportedEncodingException {

		return executeQueryIBMDEFAULTALLTASKSLIST_75("WORK_ON", ispHeader, countMax, application_name, "WORK_ON", false,
				user_, password_);
	}

	/**
	 * Return list of all task on work (and not suspended) owned by the current
	 * user and associated to a specific application name
	 * 
	 * @param String
	 *            ispHeader
	 * @param int
	 *            countMax
	 * @param String
	 *            applicationName
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.getWorkOnActiveTaskList(ispHeader,500,"my application");
	 *         <p>
	 * @throws LIBBPMException,UnsupportedEncodingException
	 */
	public JSONObject getWorkOnActiveTaskList(String ispHeader, int countMax, String application_name, String user_,
			String password_) throws LIBBPMException, UnsupportedEncodingException {

		return executeQueryIBMDEFAULTALLTASKSLIST_75("WORK_ON_ACTIVE", ispHeader, countMax, application_name,
				"WORK_ON_ACTIVE", false, user_, password_);
	}

	/**
	 * Return list of all task on work and all task available owned by the
	 * current user and associated to a specific application name
	 * 
	 * @param String
	 *            ispHeader
	 * @param int
	 *            countMax
	 * @param String
	 *            applicationName
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.getAssesAndWorkOnTaskList(ispHeader,500,"my application");
	 *         <p>
	 * @throws LIBBPMException,UnsupportedEncodingException
	 */
	public JSONObject getAssesAndWorkOnTaskList(String ispHeader, int countMax, String application_name, String user_,
			String password_) throws LIBBPMException, UnsupportedEncodingException {

		return executeQueryIBMDEFAULTALLTASKSLIST_75("ASSESS_AND_WORK_ON", ispHeader, countMax, application_name,
				"ASSESS_AND_WORK_ON", false, user_, password_);
	}

	/**
	 * Return list of all task completed owned by the current user and
	 * associated to a specific application name
	 * 
	 * @param String
	 *            ispHeader
	 * @param int
	 *            countMax
	 * @param String
	 *            applicationName
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.getCheckCompletedTaskList(ispHeader,500,"my application");
	 *         <p>
	 * @throws LIBBPMException,UnsupportedEncodingException
	 */
	public JSONObject getCompletedTaskList(String ispHeader, int countMax, String application_name, String user_,
			String password_) throws LIBBPMException, UnsupportedEncodingException {

		return executeQueryIBMDEFAULTALLTASKSLIST_75("CHECK_COMPLETED", ispHeader, countMax, application_name,
				"CHECK_COMPLETED", false, user_, password_);
	}

	/**
	 * Return list of all task (state is not filtered) owned by the current user
	 * and associated to a specific application name
	 * 
	 * @param String
	 *            ispHeader
	 * @param int
	 *            countMax
	 * @param String
	 *            applicationName
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.getCheckCompletedTaskList(ispHeader,500,"my application");
	 *         <p>
	 * @throws LIBBPMException,UnsupportedEncodingException
	 */

	public JSONObject getAllStateTaskList(String ispHeader, int countMax, String application_name, String user_,
			String password_) throws LIBBPMException, UnsupportedEncodingException {

		return executeQueryIBMDEFAULTALLTASKSLIST_75("ALL_STATE", ispHeader, countMax, application_name, "", true,
				user_, password_);
	}

	private JSONObject executeQueryIBMDEFAULTALLTASKSLIST_75(String function_name, String ispHeader, int countMax,
			String application_name, String interaction_filter, boolean base, String user_, String password_)
			throws LIBBPMException, UnsupportedEncodingException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		Client client = null;
		JSONObject obj = null;

		// /rest/bpm/wle/v1/exposed/process";

		HttpsURLConnection con = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			String application_name_encoded = URLEncoder.encode(application_name, "UTF-8");

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><").append(function_name).append("><params><applicationName>")
						.append(application_name).append("</applicationName>").append("</params>").append("</").append(function_name).append(">")
						.append("</function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					client = ClientBuilder.newClient();
					String result = null;
					String composedUrl = null;

					if (base) {
						composedUrl = "rest/bpm/wle/v1/tasks/query/IBM.DEFAULTALLTASKSLIST_75?processAppName="
								+ application_name_encoded + "&size=" + countMax
								+ "&filterByCurrentUser=true&calcStats=false";
					} else {
						composedUrl = "rest/bpm/wle/v1/tasks/query/IBM.DEFAULTALLTASKSLIST_75?interactionFilter="
								+ interaction_filter + "&processAppName=" + application_name_encoded + "&size="
								+ countMax + "&filterByCurrentUser=true&calcStats=false";
					}

					// result=
					// client.target(urlServer+"rest/bpm/wle/v1/tasks/query/IBM.DEFAULTALLTASKSLIST_75?interactionFilter=WORK_ON&processAppName=Nuovo%20Censimento%20di%20Servizio%20NBP%20REPS0&size=500&filterByCurrentUser=true&calcStats=false")
					result = client.target(urlServer + composedUrl)

							// .path("?size=500&filterByCurrentUser=true&calcStats=false")
							.request("application/json") // Expected
							// response
							// mime type
							.header("Authorization", "Basic " + encoding).header("X-ISPWebServicesHeader", ispHeader)
							.get(java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}

			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (con != null)
					con.disconnect();
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8 +" "+function_name);
		}

		return obj;

	}

	/**
	 * Start a particular process
	 * 
	 * @param String
	 *            ispHeader
	 * @param Object
	 *            processInputBO
	 * @param String
	 *            bpdId
	 * @param String
	 *            processAppId
	 * @return Json response
	 * @throws UnsupportedEncodingException
	 * 
	 *             <p>
	 *             <h4>Example:</h4>
	 *             <p>
	 *             JSONObject jo=null;
	 *             <p>
	 *             RestApiBPM ra=new RestApiBPM(cdb);
	 *             <p>
	 *             jo=ra.startProcess(BOInputTask,
	 *             "25.551d7c63-a143-42be-9b9b-f6b5a1e7431f"
	 *             ,"2066.a99af49f-5741-4666-b0c0-4aeca4efe9a2")
	 *             <p>
	 *             if the input processInputBO is not present pass null
	 *             <p>
	 * @throws LIBBPMException
	 */

	public JSONObject startProcess(String ispHeader, Object processInputBO, String bpdId, String processAppId,
			String user_, String password_) throws UnsupportedEncodingException, LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		String inputVar = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		JSONObject obj = null;
		Client client = null;

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			if (processInputBO != null) {

				inputVar = BPMUtil.toJSONString(processInputBO);

				System.out.println(inputVar);
			}

			if (isHeaderISPinBO(inputVar)) {

				// /rest/bpm/wle/v1/process?action=start&bpdId=" + bpdId +
				// "&processAppId=" + processAppId;

				try {

					String userPassword = null;
					String encoding = null;

					userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
							+ RestApiBPM.getEffectivePassword(password, password_);
					encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

					String headerMessageValidation = checkISPHeader(ispHeader);

					sb.append("<function><startProcess>").append("<params>")
							.append("<BusinessObject><![CDATA[").append(inputVar).append("]]>")
							.append("</BusinessObject>").append("<bpdId>").append(bpdId).append("</bpdId>")
							.append("<processAppId>").append(processAppId).append("</processAppId>").append("</params>")
							.append("</startProcess></function>");

					if (headerMessageValidation.length() == 0) {

						// normalize Ts in ispHeader
						ispHeader = ispHeader.replaceAll(
								"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>", "<Timestamp>"
										+ RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

						Form form = new Form();
						form.param("action", "start");
						form.param("bpdId", bpdId);
						form.param("processAppId", processAppId);

						if (!(inputVar == null || inputVar.isEmpty())) {
							form.param("params", inputVar);
						}
						form.param("parts", "all");

						client = ClientBuilder.newClient();
						String result = null;

						if (user != null && password != null)
							result = client.target(urlServer).path("/rest/bpm/wle/v1/process")
									// .queryParam("action",
									// "start").queryParam("bpdId",
									// bpdId).queryParam("processAppId",
									// processAppId).queryParam("params",
									// inputVarEncoded).queryParam("parts",
									// "all")
									.request("application/json").header("Authorization", "Basic " + encoding)
									.header("X-ISPWebServicesHeader", ispHeader)
									.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE),
											java.lang.String.class);

						obj = new JSONObject(result);

						sb1.append("<![CDATA[").append(result).append("]]>");

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
								formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""),
								this.cdb);

					} else {
						obj = new JSONObject(headerMessageValidation);

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
								sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

						throw new LIBBPMException(headerMessageValidation);
					}
				} catch (Exception e) {

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

					throw new LIBBPMException(RestApiBPM.formatError(e));

				} finally {

					if (client != null)
						client.close();
				}
			} else {

				sb1.append("<output>").append("<![CDATA[").append(Messages.ERROR_4).append("]]></output>");

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
						formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_4),
						this.cdb);

				throw new LIBBPMException(Messages.ERROR_4);
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" startProcess");
		}

		return obj;

	}

	/**
	 * Finish a task
	 * 
	 * @param String
	 *            ispHeader
	 * @param taskId
	 * @param taskBOOutput
	 *            (Business Object)
	 * @return Json response
	 * @throws UnsupportedEncodingException
	 * 
	 *             <p>
	 *             <h4>Example:</h4>
	 *             <p>
	 *             JSONObject jo=null;
	 *             <p>
	 *             RestApiBPM ra=new RestApiBPM(cdb);
	 *             <p>
	 *             jo=ra.finishTask("976",taskBO);
	 * 
	 *             if taskBOOutput is not present pass null
	 *             <p>
	 * @throws LIBBPMException
	 */
	public JSONObject finishTask(String ispHeader, String taskId, Object taskBOOutput, String taskName, String user_,
			String password_) throws UnsupportedEncodingException, LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		String inputVar = null;

		if (taskBOOutput != null) {

			inputVar = BPMUtil.toJSONString(taskBOOutput);
		}

		Client client = null;
		JSONObject obj = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			Form form = new Form();
			form.param("action", "finish");

			if (isHeaderISPinBO(inputVar) || (taskName != null && taskName.contains(""))) {

				// /rest/bpm/wle/v1/task/" + taskId + "?action=finish

				if (!(inputVar == null || inputVar.isEmpty()))
					form.param("params", inputVar); // not encoded

				form.param("parts", "all");

				try {

					String userPassword = null;
					String encoding = null;

					userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
							+ RestApiBPM.getEffectivePassword(password, password_);
					encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

					String headerMessageValidation = checkISPHeader(ispHeader);

					sb.append("<function><finishTask>").append("<params>").append("<taskId>").append(taskId)
							.append("</taskId>").append("<BusinessObject><![CDATA[").append(inputVar).append("]]>")
							.append("</BusinessObject>").append("</params>").append("</finishTask></function>");

					if (headerMessageValidation.length() == 0) {

						// normalize Ts in ispHeader
						ispHeader = ispHeader.replaceAll(
								"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>", "<Timestamp>"
										+ RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

						client = ClientBuilder.newClient();
						String result = null;

						result = client.target(urlServer).path("/rest/bpm/wle/v1/task/" + taskId)
								.request("application/json").header("X-ISPWebServicesHeader", ispHeader)
								.header("Authorization", "Basic " + encoding)
								.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE),
										java.lang.String.class);

						obj = new JSONObject(result);

						sb1.append("<![CDATA[").append(result).append("]]>");

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
								formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""),
								this.cdb);

					} else {

						obj = new JSONObject(headerMessageValidation);

						trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
								sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

						throw new LIBBPMException(headerMessageValidation);
					}
				} catch (Exception e) {

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

					throw new LIBBPMException(RestApiBPM.formatError(e));

				} finally {

					if (client != null)
						client.close();

				}

			} else {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
						formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_4),
						this.cdb);

				throw new LIBBPMException(Messages.ERROR_4);
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" finishTask");
		}

		return obj;

	}

	/**
	 * Get all instances associated to a process with a specific appShortName
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            appShortName
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.processInstancesSearch("FIN"));
	 *         <p>
	 * @throws LIBBPMException
	 */
	public JSONObject processInstancesSearch(String ispHeader, String appShortName, String user_, String password_)
			throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		Client client = null;
		JSONObject obj = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			// /rest/bpm/wle/v1/processes/search?searchFilter=" + appShortName +
			// "&searchFilterScope=AppShortName

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><processIntancesSearch>").append("<params>").append("<appShortName>")
						.append(appShortName).append("</appShortName>").append("</params>").append("</processIntancesSearch></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					client = ClientBuilder.newClient();
					String result = null;

					result = client.target(urlServer).path("/rest/bpm/wle/v1/processes/search")
							.queryParam("searchFilter", appShortName).queryParam("searchFilterScope", "AppShortName")
							.request("application/json") // Expected
							// response
							// mime
							// type
							.header("X-ISPWebServicesHeader", ispHeader).header("Authorization", "Basic " + encoding)
							.get(java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}
			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (client != null)
					client.close();
			}

		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" processInstancesSearch");
		}

		return obj;

	}

	/**
	 * Get all tasks associated to a specific process instance
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            instanceId
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.queryTasks("99"));
	 *         <p>
	 * @throws LIBBPMException
	 */

	public JSONObject queryTasks(String ispHeader, String instanceId, String user_, String password_)
			throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		Client client = null;
		JSONObject obj = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><queryTasks>").append("<params>").append("<instanceId>")
						.append(instanceId).append("</instanceId>").append("</params>").append("</queryTasks></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					Form form = new Form();
					form.param("condition", "instanceId|" + instanceId); // %7c
					form.param("organization", "byTask");
					// form.param("sort","byTask");
					form.param("run", "true");
					form.param("shared", "false");
					form.param("filterByCurrentUser", "false");

					client = ClientBuilder.newClient();
					String result = null;

					result = client.target(urlServer).path("/rest/bpm/wle/v1/search/query")
							// .queryParam("condition",
							// "instanceId%7C"+instanceId).property("organization",
							// "byTask").queryParam("run",
							// "true").queryParam("shared",
							// "false").queryParam("filterByCurrentUser",
							// "false")
							///// .queryParam("condition",
							// "instanceId%7C"+instanceId).property("organization",
							// "byTask").queryParam("sort",
							// "byTask").queryParam("run",
							// "true").queryParam("shared",
							// "false").queryParam("filterByCurrentUser",
							// "false")
							.queryParam("condition", "instanceId|" + instanceId).queryParam("organization", "byTask")
							.queryParam("run", "true").queryParam("shared", "false")
							.queryParam("filterByCurrentUser", "false").request("application/json")
							.header("Authorization", "Basic " + encoding)
							.header("Cache-Control", "no-cache, no-store, must-revalidate").header("Pragma", "no-cache")
							.header("Expires", "0").header("X-ISPWebServicesHeader", ispHeader)

							.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED), // se
									// metto
									// put
									// non
									// va
									java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);

				}
			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (client != null)
					client.close();
			}

		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" queryTasks");
		}

		return obj;

	}

	/**
	 * 
	 * Get the details of a task
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            taskid
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.taskDetails("334")
	 *         <p>
	 * @throws LIBBPMException
	 */

	public JSONObject taskDetails(String ispHeader, String taskId, String user_, String password_)
			throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		// urlServer + "/rest/bpm/wle/v1/task/" + taskId + "?parts=data";

		Client client = null;
		JSONObject obj = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><taskDetails>").append("<params>").append("<taskId>").append(taskId)
						.append("</taskId>").append("</params>").append("</taskDetails></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					client = ClientBuilder.newClient();
					String result = null;

					result = client.target(urlServer).path("/rest/bpm/wle/v1/task/" + taskId)
							.queryParam("parts", "data").request("application/json")
							.header("X-ISPWebServicesHeader", ispHeader).header("Authorization", "Basic " + encoding)
							.get(java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}
			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (client != null)
					client.close();
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" taskDetails");
		}

		return obj;

	}

	/**
	 * <p>
	 * 
	 * Assign a task to current user
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            taskid
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.assignTaskToCurrent("560");
	 *         <p>
	 * @throws LIBBPMException
	 */
	public JSONObject assignTaskToCurrent(String ispHeader, String taskId, String user_, String password_)
			throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		// /rest/bpm/wle/v1/task/" + taskId +
		// "?action=assign&toMe=true&parts=all

		JSONObject obj = null;

		String userPassword = null;
		String encoding = null;

		userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
				+ RestApiBPM.getEffectivePassword(password, password_);
		encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

		Client client = null;
		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><assignTaskToCurrent>").append("<params>").append("<taskId>").append(taskId)
						.append("</taskId>").append("</params></assignTaskToCurrent></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");
					Form form = new Form();
					form.param("action", "assign");
					form.param("toMe", "true");
					form.param("parts", "all");

					client = ClientBuilder.newClient();
					String result = null;

					if (user != null && password != null)
						result = client.target(urlServer).path("/rest/bpm/wle/v1/task/" + taskId)
								.queryParam("action", "assign").queryParam("toMe", "true").queryParam("parts", "all")
								.request("application/json").header("Authorization", "Basic " + encoding)
								.header("Cache-Control", "no-cache, no-store, must-revalidate")
								.header("Pragma", "no-cache").header("Expires", "0")
								.header("X-ISPWebServicesHeader", ispHeader)
								.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED), // se
										// metto
										// put
										// non
										// va
										java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				}

				else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}
			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			}

			finally {

				if (client != null)
					client.close();
			}

		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" assignTaskToCurrent");
		}

		return obj;

	}

	// 29102016

	/**
	 * <p>
	 * Reassign task to the lane
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            taskid
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.reassignTaks(ispHeader"560");
	 *         <p>
	 * @throws LIBBPMException
	 */
	public JSONObject reassignTaks(String ispHeader, String taskId, String user_, String password_)
			throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		// https://win-ntastb0i3iv:9443/rest/bpm/wle/v1/task/3957?action=assign&back=true&parts=all

		JSONObject obj = null;

		String userPassword = null;
		String encoding = null;

		userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
				+ RestApiBPM.getEffectivePassword(password, password_);
		encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

		Client client = null;
		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><reassignTask>").append("<params>").append("<taskId>").append(taskId)
						.append("</taskId>").append("</params></reassignTask></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					Form form = new Form();
					form.param("action", "assign");
					form.param("back", "true");
					form.param("parts", "all");

					client = ClientBuilder.newClient();
					String result = null;

					if (user != null && password != null)
						result = client.target(urlServer).path("/rest/bpm/wle/v1/task/" + taskId)
								.queryParam("action", "assign").queryParam("back", "true").queryParam("parts", "all")
								.request("application/json").header("Authorization", "Basic " + encoding)
								.header("Cache-Control", "no-cache, no-store, must-revalidate")
								.header("Pragma", "no-cache").header("Expires", "0")
								.header("X-ISPWebServicesHeader", ispHeader)
								.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED), // se
										// metto
										// put
										// non
										// va
										java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				}

				else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}
			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			}

			finally {

				if (client != null)
					client.close();
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" reassignTaks");
		}

		return obj;

	}

	/**
	 * 
	 * Assign a task to a user
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            userbpm
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.assignTaskToCurrent(ispHeader,"400",Jack);
	 *         <p>
	 * @throws LIBBPMException
	 */

	public JSONObject assignTaskToUser(String ispHeader, String taskId, String userbpm, String user_, String password_)
			throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		// /rest/bpm/wle/v1/task/" + taskId + "?action=assign&toUser=" + userbpm
		// + "&parts=all

		Client client = null;
		JSONObject obj = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><assignTaskToUser>").append("<params>").append("<taskId>").append(taskId)
						.append("</taskId>").append("<user>").append(userbpm).append("</user>")
						.append("</params></assignTaskToUser></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					Form form = new Form();
					form.param("action", "assign");
					form.param("toUser", userbpm);
					form.param("parts", "all");

					String result = null;
					client = ClientBuilder.newClient();
					if (user != null && password != null)
						result = client.target(urlServer).path("/rest/bpm/wle/v1/task/" + taskId)
								.queryParam("action", "assign").queryParam("toUser", userbpm).queryParam("parts", "all")
								.request("application/json").header("Authorization", "Basic " + encoding)
								.header("Cache-Control", "no-cache, no-store, must-revalidate")
								.header("Pragma", "no-cache").header("Expires", "0")
								.header("X-ISPWebServicesHeader", ispHeader)
								.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED), // se
										// metto
										// put
										// non
										// va
										java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}
			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (client != null)
					client.close();
			}

		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" assignTaskToUser");
		}

		return obj;

	}

	/**
	 * 
	 * Assign a task to a group
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            group
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.assignTaskToCurrent(ispHeader,"580","Office1");
	 *         <p>
	 * @throws LIBBPMException
	 */
	public JSONObject assignTaskToGroup(String ispHeader, String taskId, String group, String user_, String password_)
			throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		// /rest/bpm/wle/v1/task/" + taskId + "?action=assign&toGroup=" + group
		// + "&parts=all";

		JSONObject obj = null;
		Client client = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><assignTaskToGroup>").append("<params>").append("<taskId>").append(taskId)
						.append("</taskId>").append("<group>").append(group).append("</group>")
						.append("</params></assignTaskToGroup></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					Form form = new Form();
					form.param("action", "assign");
					form.param("toGroup", group);
					form.param("parts", "all");

					client = ClientBuilder.newClient();
					String result = null;

					if (user != null && password != null)
						result = client.target(urlServer).path("/rest/bpm/wle/v1/task/" + taskId)
								.queryParam("action", "assign").queryParam("toGroup", group).queryParam("parts", "all")
								.request("application/json").header("Authorization", "Basic " + encoding)
								.header("Cache-Control", "no-cache, no-store, must-revalidate")
								.header("Pragma", "no-cache").header("Expires", "0")
								.header("X-ISPWebServicesHeader", ispHeader)
								.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED), // se
										// metto
										// put
										// non
										// va
										java.lang.String.class);
					else

						obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				} else {
					obj = new JSONObject(headerMessageValidation);

					sb1.append("<output>").append("<![CDATA[").append(headerMessageValidation).append("]]></output>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}
			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			}

			finally {

				if (client != null)
					client.close();
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" assignTaskToGroup");
		}

		return obj;
	}

	/**
	 * 
	 * Get users associated with the group
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            group
	 * @return Json response
	 * 
	 *         Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.usersInGroup(ispHeader,"Office1");
	 *         <p>
	 * @throws LIBBPMException
	 */
	public JSONObject usersInGroup(String ispHeader, String group, String user_, String password_)
			throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		// "/rest/bpm/wle/v1/groups?filter=" + group +
		// "&includeDeleted=false&parts=all";

		Client client = null;
		JSONObject obj = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><userInGroup>").append("<params>").append("<group>").append(group)
						.append("</group").append("</params>").append("</userInGroup></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					client = ClientBuilder.newClient();
					String result = null;

					result = client.target(urlServer).path("/rest/bpm/wle/v1/groups").queryParam("filter", group)
							.queryParam("includeDeleted", "false").queryParam("parts", "all")
							.request("application/json").header("X-ISPWebServicesHeader", ispHeader)
							.header("Authorization", "Basic " + encoding).get(java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}
			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (client != null)
					client.close();
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" usersInGroup");
		}

		return obj;

	}

	/**
	 * 
	 * Get user information
	 * 
	 * @param String
	 *            ispHeader
	 * @param String
	 *            utente
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.usersInformation(ispHeader,"Jack")
	 *         <p>
	 * @throws LIBBPMException
	 */
	public JSONObject usersInformation(String ispHeader, String usertarget, String user_, String password_)
			throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		// "/rest/bpm/wle/v1/user/"+ usertarget+
		// "?includeInternalMemberships=false&refreshUser=false&parts=memberships";

		Client client = null;
		JSONObject obj = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><userInformation>").append("<params>").append("<user>").append(usertarget)
						.append("</user").append("</params>").append("</userInformation></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					client = ClientBuilder.newClient();
					String result = null;

						result = client.target(urlServer).path("/rest/bpm/wle/v1/user/" + usertarget)
								.queryParam("includeInternalMemberships", "false").queryParam("refreshUser", "false")
								.queryParam("parts", "memberships").request("application/json")
								.header("X-ISPWebServicesHeader", ispHeader)
								.header("Authorization", "Basic " + encoding).get(java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}

			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (client != null)
					client.close();
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" usersInformation");
		}

		return obj;

	}

	/**
	 * 
	 * Get all tasks in state New or Received
	 * 
	 * @param String
	 *            ispHeader
	 * 
	 * @return Json response
	 * 
	 *         <p>
	 *         <h4>Example:</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.getInbox(ispHeader);
	 *         <p>
	 * @throws LIBBPMException
	 */
	public JSONObject getInbox(String ispHeader, String user_, String password_) throws LIBBPMException {

		String urlServer = cdb.getUrl();
		String user = cdb.getUser();
		String password = cdb.getPassword();

		Form form = new Form();
		form.param("columns",
				"instanceId%2CbpdName%2CassignedToRole%2CinstanceStatus%2CinstanceProcessApp%2CassignedToUser%2CassignedToRole%2CtaskStatus%2CtaskDueDate%2CtaskPriority%2CtaskReceivedDate%2CtaskActivityName");
		form.param("condition", "taskStatus%7CNew_or_Received");
		form.param("sort", "byInstance");
		form.param("secondSort", "instanceId");
		form.param("organization", "byTask");
		form.param("run", "true");
		form.param("shared", "false");
		form.param("filterByCurrentUser", "false");

		// /rest/bpm/wle/v1/search/query";

		Client client = null;
		JSONObject obj = null;

		StringBuffer sb = new StringBuffer();
		StringBuffer sb1 = new StringBuffer();

		String executionTS = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS").format(new Date());

		if (RestApiBPM.checkUsernamePassword(user, user_, password, password_)) {

			try {

				String userPassword = null;
				String encoding = null;

				userPassword = RestApiBPM.getEffectiveUser(user, user_) + ":"
						+ RestApiBPM.getEffectivePassword(password, password_);
				encoding = new String(Base64.encodeBase64(userPassword.getBytes()));

				String headerMessageValidation = checkISPHeader(ispHeader);

				sb.append("<function><getInbox>").append("<params>").append("</params>").append("</getInbox></function>");

				if (headerMessageValidation.length() == 0) {

					// normalize Ts in ispHeader
					ispHeader = ispHeader.replaceAll(
							"<Timestamp>" + RestApiBPM.getTagTimestamp(ispHeader) + "</Timestamp>",
							"<Timestamp>" + RestApiBPM.getTagTimestampCheckAndNormalize(ispHeader) + "</Timestamp>");

					client = ClientBuilder.newClient();
					String result = null;

					result = client.target(urlServer).path("/rest/bpm/wle/v1/search/query").request("application/json")
							.header("X-ISPWebServicesHeader", ispHeader).header("Authorization", "Basic " + encoding)
							.put(Entity.entity(form, MediaType.TEXT_PLAIN_TYPE), java.lang.String.class);

					obj = new JSONObject(result);

					sb1.append("<![CDATA[").append(result).append("]]>");

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
							formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, ""), this.cdb);

				} else {
					obj = new JSONObject(headerMessageValidation);

					trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
							sb.toString(), sb1.toString(), executionTS, headerMessageValidation), this.cdb);

					throw new LIBBPMException(headerMessageValidation);
				}

			} catch (Exception e) {

				trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader), formatXmltraceData(ispHeader,
						sb.toString(), sb1.toString(), executionTS, RestApiBPM.formatError(e)), this.cdb);

				throw new LIBBPMException(RestApiBPM.formatError(e));

			} finally {

				if (client != null)
					client.close();
			}
		} else {

			trace(ispHeader, executionTS, getTagApplicationIDValue(ispHeader),
					formatXmltraceData(ispHeader, sb.toString(), sb1.toString(), executionTS, Messages.ERROR_8),
					this.cdb);

			throw new LIBBPMException(Messages.ERROR_8+" getInbox");
		}

		return obj;

	}

	/**
	 * 
	 * Get all tasks in a particular state specified by the caller
	 * 
	 * @param String
	 *            status
	 * @return JsonArray
	 * 
	 *         <p>
	 *         <h4>Example</h4>
	 *         <p>
	 *         JSONObject jo=null;
	 *         <p>
	 *         JSONArray jsa=null;
	 *         <p>
	 *         RestApiBPM ra=new RestApiBPM(cdb);
	 *         <p>
	 *         jo=ra.queryTasks(ispHeader,"381");
	 *         <p>
	 *         jo=ra.getTaskInfobyStatus(jo, "Received");
	 *         <p>
	 *         jo=(JSONObject)jsa.getJSONObject(0); //get first element for
	 *         example
	 *         <p>
	 *         String taskId=ra.jsonQuery(jo,"$.taskId"); //get taskId
	 *         <p>
	 * @throws LIBBPMException
	 * 
	 */
	@SuppressWarnings("unchecked")
	public JSONArray getTaskInfobyStatus(JSONObject obj, String status) throws LIBBPMException {

		JSONArray jsa = null;

		try {
			String xml = XML.toString(obj);

			xml = "<start_appended>" + xml + "</start_appended>";

			InputSource source = new InputSource(new StringReader(xml));

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

			DocumentBuilder db = dbf.newDocumentBuilder();

			Document document = db.parse(source);

			XPathFactory xpathFactory = XPathFactory.newInstance();

			XPath xpath = xpathFactory.newXPath();

			XPathExpression expr = xpath.compile("/start_appended/data/data");

			Object result = expr.evaluate(document, XPathConstants.NODESET);

			NodeList nodes = (NodeList) result;

			@SuppressWarnings("rawtypes")
			HashMap service = new HashMap();

			@SuppressWarnings("rawtypes")
			HashMap queryService = new HashMap();
			@SuppressWarnings("rawtypes")
			ArrayList data = new ArrayList();
			int count = 0;

			for (int i = 0; i < nodes.getLength(); i++) {

				if (null != nodes.item(i)) {

					NodeList nodeList = nodes.item(i).getChildNodes();

					for (int k = 0; null != nodeList && k < nodeList.getLength(); k++) {

						Node nod = nodeList.item(k);

						if (nod.getNodeType() == Node.ELEMENT_NODE)

							service.put(nodeList.item(k).getNodeName(), nod.getFirstChild().getNodeValue());

					}

					if (service.get("taskStatus").equals(status)) {

						queryService.put(service.get("taskId"), // taskID
								service);

						data.add(count, service);
						count++;

						service = new HashMap<String, Boolean>();
					}

					service.clear();

				}
			}

			if (queryService.size() != 0) {

				jsa = new JSONArray(data);
			} else {

				jsa = new JSONArray("[{}]");
			}

		} catch (Exception ex) {
			throw new LIBBPMException(RestApiBPM.formatError(ex));
		}
		return jsa;

	}

	/**
	 * Execute an xpath query on a json object
	 * 
	 * @param Object
	 *            obj
	 * @param String
	 *            query
	 * @return Json response
	 * @throws LIBBPMException
	 * @throws Exception
	 * 
	 *             <p>
	 *             <h4>Example:</h4>
	 *             <p>
	 *             JSONObject jo=null;
	 *             <p>
	 *             JSONObject jo1=null;
	 *             <p>
	 *             RestApiBPM ra=new RestApiBPM(cdb);
	 *             <p>
	 *             jo= ra.startProcess(null, bpdId, processAppId);
	 *             <p>
	 *             jo1=ra.jsonQuery(jo,"$.data.piid");
	 *             <p>
	 *             <p>
	 */
	public String jsonQuery(JSONObject obj, String query) throws LIBBPMException {

		String query_result = null;
		if (query == null)
			query = "";

		try {
			Object variablesJson = JsonPath.read(obj.toString(3), query);
			query_result = variablesJson.toString();
		} catch (Exception ex) {
			throw new LIBBPMException(RestApiBPM.formatError(ex));
		}
		return query_result;
	}

	private String checkISPHeader(String ispHeader) {

		String result = "";

		if (checkISPHeaderValidity(ispHeader)) {

			if (!isTagServiceIDValid(ispHeader) || !isTagApplicationIDValid(ispHeader)
					|| !isTagTimestampValid(ispHeader)) {

				result = Messages.ERROR_1;

			} else {

				if (getServiceIDTagValue(ispHeader).length() != 0
						&& getTagApplicationIDValue(ispHeader).length() != 0) {

					if (getTagTimestampCheckAndNormalize(ispHeader) == null)
						result = Messages.ERROR_3;

				} else {
					result = Messages.ERROR_2;
				}
			}

		} else {
			result = Messages.ERROR_1;
		}

		return result;

	}

	private static boolean checkISPHeaderValidity(String ispHeader) {

		boolean result = true;

		if (ispHeader == null || ispHeader == "" || ispHeader.indexOf("<ServiceID>") == -1

				|| ispHeader.indexOf("</ServiceID>") == -1 || ispHeader.indexOf("<ApplicationID>") == -1

				|| ispHeader.indexOf("</ApplicationID>") == -1 || ispHeader.indexOf("<Timestamp>") == -1
				|| ispHeader.indexOf("</Timestamp") == -1) {

			result = false;
		}

		return result;

	}

	private static boolean isTagServiceIDValid(String ispHeader) {

		boolean result = false;

		int startindex = ispHeader.indexOf("<ServiceID>");

		int endindex = ispHeader.indexOf("</ServiceID>");

		if (startindex < endindex)
			result = true;

		return result;

	}

	private static String getServiceIDTagValue(String ispHeader) {

		String response = "";

		int startindex = ispHeader.indexOf("<ServiceID>");

		int endindex = ispHeader.indexOf("</ServiceID>");

		response = ispHeader.substring(startindex + 11, endindex);

		return response;

	}

	private static boolean isTagApplicationIDValid(String ispHeader) {

		boolean result = false;

		int startindex = ispHeader.indexOf("<ApplicationID>");

		int endindex = ispHeader.indexOf("</ApplicationID>");

		if (startindex < endindex)
			result = true;

		return result;

	}

	private static String getTagApplicationIDValue(String ispHeader) {

		String response = "";

		int startindex = ispHeader.indexOf("<ApplicationID>");

		int endindex = ispHeader.indexOf("</ApplicationID>");

		if (startindex == -1 || endindex == -1) {

			response = "?";

		} else {

			response = ispHeader.substring(startindex + 15, endindex);

		}

		return response;

	}

	private static boolean isTagTimestampValid(String ispHeader) {

		boolean result = false;

		int startindex = ispHeader.indexOf("<Timestamp>");

		int endindex = ispHeader.indexOf("</Timestamp>");

		if (startindex < endindex)
			result = true;

		return result;

	}

	private static String getTagTimestampCheckAndNormalize(String ispHeader) {

		String ts = "";

		int startindex = ispHeader.indexOf("<Timestamp>");

		int endindex = ispHeader.indexOf("</Timestamp>");

		ts = ispHeader.substring(startindex + 11, endindex);

		return RestApiBPM.checkTimestamp(ts);

	}

	private static String getTagTimestamp(String ispHeader) {

		String ts = "";

		int startindex = ispHeader.indexOf("<Timestamp>");

		int endindex = ispHeader.indexOf("</Timestamp>");

		ts = ispHeader.substring(startindex + 11, endindex);

		return ts;

	}

	private boolean isHeaderISPinBO(String bo) {

		boolean result = false;

		System.out.println(bo);

		// if (bo != null && bo.indexOf("\"ISPWebServiceHeader\"", 0) != -1)
		if (bo != null && bo.indexOf("<ISPWebserviceHeader>", 0) != -1)
			result = true;

		// result=true;ISPWebserviceHeader

		return result;

	}

	private void trace(String ispHeader, String executionTs, String applicationId, String data,
			com.isp.bpm.rest.ConnectionDataBeanSingleton cdb) throws LIBBPMException {

		String url = "";
		int timeout = 0;
		String[] endPointAndTimeout = null;
		String nowTraceTs = "";
		StringBuffer sb = new StringBuffer();

		if (cdb.getTrace()) {

			if (applicationId == null)
				applicationId = "?"; // if header is invalid for example

			Cache cache = cdb.getCache();

			@SuppressWarnings("rawtypes")
			HashMap current = null;

			Element element = cache.get("NBPLIBTRC");

			if (element == null) {

				sb.append("<ServiceID>").append("NBPLIBTRC").append("</ServiceID>").append("<ApplicationID>")
						.append(applicationId).append("</ApplicationID>").append("<Timestamp>").append(executionTs)
						.append("</Timestamp>");

				endPointAndTimeout = new com.isp.wsrr.lookup.utility.WsrrUtility().getTraceCatalogEndpointAndTimeout(
						sb.toString(), cdb.getServerType(), cdb.getUrlwsrr(), cdb.getUserwsrr(), cdb.getPasswordwsrr());

				cache.put(new Element("NBPLIBTRC", endPointAndTimeout));

			} else {
				endPointAndTimeout = (String[]) element.getObjectValue();
			}

			if (endPointAndTimeout != null) {

				url = endPointAndTimeout[0];
				try {
					timeout = Integer.parseInt(endPointAndTimeout[1]);
				} catch (NumberFormatException ex) {
					timeout = 3;
				}

				if (url != null && url.length() != 0) {
					if (!url.contains("void://")) {

						sb.delete(0, sb.length());
						sb.append("<input>").append("<header><![CDATA[").append(ispHeader).append("]]></header>")
								.append(data).append("<hostName>").append(RestApiBPM.getHostName()).append("</hostName>")
								.append("<environment></environment>").append("<libVersion>").append(aboutLib)
								.append("</libVersion>");
						sb.append("<beginTimestamp>").append(executionTs).append("</beginTimestamp>>")
								.append("<endTimestamp>").append(nowTraceTs).append("</endTimestamp>")
								.append("</input>");

						new com.isp.wsrr.lookup.utility.WsrrUtility().restClientTracingServicePost(url, timeout, data);
					}

				} else
					throw new LIBBPMException(Messages.ERROR_7);
			}
		}

	}

	private static String checkTimestamp(String input) {

		boolean iscorrect = false;

		String ts = null;

		String other = null;

		if (input != null && input.length() >= 17 & input.length() <= 20) {

			String zero = "00000000";

			ts = input.substring(0, 14);

			other = input.substring(14, input.length());

			DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

			formatter.setLenient(false);

			try {

				formatter.parse(ts);

				Integer.parseInt(other);

				ts = ts + other;

				int difflen = 20 - ts.length();

				ts = ts.concat(zero.substring(0, difflen));

				iscorrect = true;

			} catch (Exception e) {

				iscorrect = false;

			}

		}

		if (iscorrect)
			return ts;

		else
			return null;

	}

	private static String getHostName() {

		String hostName = "";
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// not todo
		}
		return hostName;

	}

	private static boolean checkUsernamePassword(String username, String username_, String password, String password_) {

		boolean result = true;

		if (username_ == null || password_ == null) {

			if (username == null || password == null)
				result = false;

		}

		return result;
	}

	private static String getEffectiveUser(String username, String username_) {

		String user = null;

		if (username != null)
			user = username;
		if (username_ != null)
			user = username_;

		return user;
	}

	private static String getEffectivePassword(String password, String password_) {

		String passwd = null;

		if (password != null)
			passwd = password;
		if (password_ != null)
			passwd = password_;

		return passwd;
	}
}