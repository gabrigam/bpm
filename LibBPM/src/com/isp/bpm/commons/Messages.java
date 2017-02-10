package com.isp.bpm.commons;

@SuppressWarnings("unused")

public final class Messages {
	
	public static final String ERROR_1 = "{\"errore_LIBBPM\":\"tags <ServiceID></ServiceID> e/o <ApplicationID></ApplicationID> e/o <Timestamp></Timestamp> non trovati nel Header ISP\"}";

	public static final String ERROR_2 = "{\"errore_LIBBPM\":\"tags <ServiceID></ServiceID> e/o <ApplicationID></ApplicationID> e/o <Timestamp></Timestamp> non hanno un valore associato nel Header ISP\"}";

	public static final String ERROR_3 = "{\"errore_LIBBPM\":\"tag <Timestamp> nel Header ISP non ha un valore valido\"}";

	public static final String ERROR_4 = "{\"errore_LIBBPM\":\"Non trovata la struttura ISPWebServicesHeader nel Business Object associato alla chiamata\"}";
	
	public static final String ERROR_5 = "{\"errore_LIBBPM\":\"Il file di configurazione per il gestore cache (EHCACHE) non e' valido\"}";
	
	public static final String ERROR_6 = "{\"errore_LIBBPM\":\"Il file di configurazione per il gestore cache (EHCACHE) non contiene una definizione di cache con nome : wsrrlkpcache\"}";

	public static final String ERROR_7 = "{\"errore_LIBBPM\":\"Endpoint del servizio di tracciatura non specificato\"}";
	//V2.6
	public static final String ERROR_8 = "{\"errore_LIBBPM\":\"Impostare utente e password per eseguire la funzione\"}";
}
