package com.isp.bpm.rest;

import com.isp.bpm.exception.LIBBPMException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

/**
 * 
 * The BPM connection credential are passed inside an object of type :
 * ConnectionDataBean
 * <p>
 * that contains this member variables:
 * <p>
 * BPM server name
 * <p>
 * port
 * <p>
 * user
 * <p>
 * password
 * <p>
 * this information are present in a property file passed to the jvm by the
 * option:
 * <p>
 * <h4>-Dbpm.connection.data</h4>
 * <p>
 * example:
 * <p>
 * -Dbpm.connection.data=c:\bpmconnection.property
 * 
 * @author Primeur
 * @version 1.0
 * 
 */

public class ConnectionDataBeanSingleton {

	private static ConnectionDataBeanSingleton instance = null;
	private String url;
	private String user;
	private String password;

	private String urlwsrr;
	private String userwsrr;
	private String passwordwsrr;

	private String endpointTrace;
	private int timeout;

	private String lastInvocationTs;
	private boolean trace;
	
	private String serverType;
	
	private Cache cache;

	private ConnectionDataBeanSingleton() throws LIBBPMException {

		String url = System.getProperty("LIBBPMURL");
		String user = System.getProperty("LIBBPMUSER");
		String password = System.getProperty("LIBBPMPASSWORD");
		
		// wsrr variables
		String urlwsrr = System.getProperty("LIBLKPWSRRURL");
		String userwsrr = System.getProperty("LIBLKPWSRRUSER");
		String passwordwsrr = System.getProperty("LIBLKPWSRRPASSWORD");

		String trace = System.getProperty("LIBBPMTRACE");
		
		String cache=System.getProperty("LIBLKPWSRRCACHECONFIG");
		String heap=System.getProperty("LIBLKPWSRRHEAPENTRIES");
		String ttl=System.getProperty("LIBLKPWSRRCACHETTLIVE");
		String tti=System.getProperty("LIBLKPWSRRCACHETTIDLE");
		String serverType=System.getProperty("LIBLKPWSRRSERVERTYPE");
		
		String errorurl = "";
		String erroruser = "";
		String errorpassword = "";
		String errorwsrrurl="";
		boolean error = false;
		
		CacheManager lkpWsrrCacheManager;
		Cache lkpWsrrCache=null;

		StringBuffer sb = new StringBuffer();

		if (url == null) {
			errorurl = " NO value found for LIBBPMURL environment variable ";
			error = true; 
		}
		if (user == null) {
			erroruser = " NO value found for LIBBPMUSER environment variable ";
			error = false; //errore non piu' segnalato
		}
		if (password == null) {
			errorpassword = " NO value found for LIBBPMPASSWORD environment variable ";
			error = false; //errore non piu' segnalato
		}

		if (!error) {
			

			if (trace == null || !trace.equalsIgnoreCase("N")) {

				if (urlwsrr != null) {

					if (urlwsrr.contains("https://")) {
						if (userwsrr == null) {
							erroruser = " NO value found for LIBLKPWSRRUSER environment variable ";
							error = true;
						}
						if (passwordwsrr == null) {
							errorpassword = " NO value found for LIBLKPWSRRPASSWORD environment variable ";
							error = true;
						}
					}
				}
				else {				

					errorwsrrurl="NO value found for LIBLKPWSRRURL environment variable";
					error = true;
				}

				if (!error) {
					this.setUrl(url);
					this.setUser(user);
					this.setPassword(password);

					this.setUrlwsrr(urlwsrr);
					this.setUserwsrr(userwsrr);
					this.setPasswordwsrr(passwordwsrr);
					this.setTrace(true);

					if (urlwsrr != null)
						this.setTrace(true);
					else
						this.setTrace(false);

				} else {

					throw new LIBBPMException(sb.append(errorurl).append(erroruser).append(errorpassword).append(errorwsrrurl).toString());
					
				}
			} else {
				this.setUrl(url);
				this.setUser(user);
				this.setPassword(password);
				this.setTrace(false);
			}

		} else {
			throw new LIBBPMException(sb.append(errorurl).append(erroruser).append(errorpassword).toString());
		}
		
		//set cache default info
		
		int  heapCurrent=1000;
		long ttlCurrent=600L;
		long ttiCurrent=300L;
		
		if (ttl !=null && ttl.length()!=0) {
			
			try {
				ttlCurrent=Long.parseLong(ttl);
			}catch(NumberFormatException ex) {
				//nothing todo
			}			
		}
		
		if (tti !=null && tti.length()!=0) {
			
			try {
				ttiCurrent=Long.parseLong(tti);
			}catch(NumberFormatException ex) {
				//nothing todo
			}			
		}

		if (heap !=null && heap.length()!=0) {
			
			try {
				heapCurrent=Integer.parseInt(heap);
			}catch(NumberFormatException ex) {
				//nothing todo
			}			
		}
			
		try {
			
		//System.out.println("cache file "+cache);
		
		if (cache !=null && cache.length() !=0) {
			
			lkpWsrrCacheManager = CacheManager.newInstance(cache);
			lkpWsrrCache = lkpWsrrCacheManager.getCache("wsrrlkpcache");		
			
		} else {
			
			 //create local cache configuration
		     lkpWsrrCacheManager = CacheManager.create();
		     lkpWsrrCache = new Cache("wsrrlkpcache", heapCurrent, false, false, ttlCurrent, ttiCurrent);
		     lkpWsrrCacheManager.addCache(lkpWsrrCache);
		     lkpWsrrCache = lkpWsrrCacheManager.getCache("wsrrlkpcache");
		     lkpWsrrCache.getCacheConfiguration().setMemoryStoreEvictionPolicy("LRU");
		}

		} catch (Exception ex) {
			
			throw new LIBBPMException(com.isp.bpm.commons.Messages.ERROR_5);
		}
		

		if (lkpWsrrCache != null) {

			this.setCache(lkpWsrrCache);
			
		} else {
			
			throw new LIBBPMException(com.isp.bpm.commons.Messages.ERROR_6);
		}
		
		if (serverType==null) serverType="BEA";
		else 
		serverType="OTHER";		
		this.serverType=serverType;

	}

	/**
	 * Return the insance with BPM connection data
	 * 
	 * @throws Exception
	 *             ( "Error in reading data from the property
	 *             -Dbpm.connection.data )
	 */
	public static synchronized ConnectionDataBeanSingleton setData() throws Exception {

		if (instance == null) {
			instance = new ConnectionDataBeanSingleton();
		}
		return instance;
	}

	public String getUrl() {
		return url;
	}

	private void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	private void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	private void setPassword(String password) {
		this.password = password;
	}

	public String getUrlwsrr() {
		return urlwsrr;
	}

	public void setUrlwsrr(String urlwsrr) {
		this.urlwsrr = urlwsrr;
	}

	public String getUserwsrr() {
		return userwsrr;
	}

	public void setUserwsrr(String userwsrr) {
		this.userwsrr = userwsrr;
	}

	public String getPasswordwsrr() {
		return passwordwsrr;
	}

	public void setPasswordwsrr(String passwordwsrr) {
		this.passwordwsrr = passwordwsrr;
	}

	public String getEndpointTrace() {
		return endpointTrace;
	}

	public void setEndpointTrace(String endpointTrace) {
		this.endpointTrace = endpointTrace;
	}

	public String getLastInvocationTs() {
		return lastInvocationTs;
	}

	public void setLastInvocationTs(String lastInvocationTs) {
		this.lastInvocationTs = lastInvocationTs;
	}

	public boolean getTrace() {
		return trace;
	}

	public void setTrace(boolean trace) {
		this.trace = trace;
	}

	public Cache getCache() {
		return cache;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
    public String getServerType() {
		return serverType;
	}

	protected void setServerType(String serverType) {
		this.serverType = serverType;
	}

}