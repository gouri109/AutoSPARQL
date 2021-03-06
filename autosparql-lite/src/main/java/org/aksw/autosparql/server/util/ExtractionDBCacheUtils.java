package org.aksw.autosparql.server.util;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.aksw.autosparql.server.Defaults;
import org.apache.log4j.Logger;
import org.dllearner.kb.sparql.ExtractionDBCache;

public final class ExtractionDBCacheUtils
{
	private static final Logger log = Logger.getLogger(ExtractionDBCacheUtils.class);	
	static
	{
		log.info("registering org.h2.Driver");
		try{Class.forName("org.h2.Driver");}
		catch (ClassNotFoundException e){throw new RuntimeException("Couldn't initialize org.h2.Driver classs.",e);}	
	}
	
	static String cacheDir = System.getProperty("java.io.tmpdir");
	static {log.info("ExtractionDBCache will use cacheDir \""+System.getProperty("java.io.tmpdir")+"\"");}
	static private final String param = ";CACHE_SIZE=100000;AUTO_SERVER=TRUE"; 
	//public static void setCacheDir(String cacheDir) {ExtractionDBCacheUtils.cacheDir=cacheDir;log.info("ExtractionDBCacheUtils cacheDirectory set to "+cacheDir);}
	
	private ExtractionDBCacheUtils() {throw new AssertionError();}
	
	// see http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html
	private static String key(String endpoint, String graph)
	{return endpoint.replaceAll("\\W", "")+'_'+graph.replaceAll("\\W", "");}
	
	public static ExtractionDBCache getDBpediaCache() throws SQLException {return getCache(Defaults.endpointURL(),Defaults.graphURL());}
	public static ExtractionDBCache getOxfordCache() throws SQLException {return getCache(Defaults.oxfordEndpointURL(),Defaults.oxfordGraphURL());}	
	
	public static ExtractionDBCache getCache(String endpoint, String graph) throws SQLException
	{return getDiskCache(endpoint,graph);}
		
	@SuppressWarnings("unused")
	private static ExtractionDBCache getMemCache(String endpoint, String graph) throws SQLException
	{
		return new ExtractionDBCache(DriverManager.getConnection("jdbc:h2:mem"+key(endpoint,graph)+param, "", ""));
		}
	
	private static ExtractionDBCache getDiskCache(String endpoint, String graph) throws SQLException
	{
		if(cacheDir==null) {throw new RuntimeException("cache dir not set");}
		log.debug("getting disk cache residing in cacheDir \""+cacheDir+'"');
		return new ExtractionDBCache(DriverManager.getConnection("jdbc:h2:"+cacheDir+'/'+key(endpoint,graph)+param, "", ""));
		} 
}