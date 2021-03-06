package org.aksw.autosparql.server;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import org.aksw.autosparql.client.SPARQLService;
import org.aksw.autosparql.client.exception.AutoSPARQLException;
import org.aksw.autosparql.client.exception.SPARQLQueryException;
import org.aksw.autosparql.client.model.Endpoint;
import org.aksw.autosparql.client.model.Example;
import org.aksw.autosparql.client.model.StoredSPARQLQuery;
import org.aksw.autosparql.server.search.QuestionProcessor;
import org.aksw.autosparql.server.store.SimpleFileStore;
import org.aksw.autosparql.server.store.Store;
import org.apache.log4j.Logger;
import org.ini4j.Ini;
import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class SPARQLServiceImpl extends RemoteServiceServlet implements SPARQLService{
	
	enum SessionKeywords{
		AUTOSPARQL_SESSION
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1448196614767491966L;
	
	private static final String AUTOSPARQL_SESSION = "autosparql_session";
	
	private List<StoredSPARQLQuery> storedSPARQLQueries;
	
	private Map<Endpoint, Dataset> endpoint2DatasetMap;
	
	private Store store;
	
	private static final Logger logger = Logger.getLogger(SPARQLServiceImpl.class);
	
	private String storeDir;
	private String cacheDir;
	
	private String question;
	
	private QuestionProcessor questionProcessor;
	
	public SPARQLServiceImpl(){
		super();
//		java.util.logging.Logger.getLogger("org.apache.solr").setLevel(Level.WARNING);
		questionProcessor = new QuestionProcessor();
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
//		ApplicationContext ctx = new ClassPathXmlApplicationContext("autosparql.xml", "autosparql-session.xml");
//		AutoSPARQLConfig aConfig = (AutoSPARQLConfig) ctx.getBean("autosparqlconfig");
//		AutoSPARQLSession aSession = (AutoSPARQLSession) ctx.getBean("autosparql.session");
		
		String configPath = config.getInitParameter("configPath");
		loadConfig(configPath);
		loadDatasets();
		loadSPARQLQueriesFromFile();
		
	}
	
	private void loadConfig(String path){
		logger.debug("Loading config file");
		try {
			InputStream is = getServletContext().getResourceAsStream(path);
			Ini ini = new Ini(is);
			storeDir = ini.get("storeDir").get("path");
			cacheDir = ini.get("cacheDir").get("path");
		} catch (Exception e){
			e.printStackTrace();
		}
		if(storeDir != null && !storeDir.startsWith("/")){
			storeDir = getServletContext().getRealPath(storeDir);
		}
		if(cacheDir != null && !cacheDir.startsWith("/")){
			cacheDir = getServletContext().getRealPath(cacheDir);
		}
	}
	
	private void loadDatasets(){
		logger.debug("Loading datasets from file: " + getServletContext().getRealPath("app/datasets.xml"));
		try {
			List<Dataset> datasets = DatasetLoader.loadDatasets(getServletContext().getRealPath("app/datasets.xml"));
			
			endpoint2DatasetMap = new HashMap<Endpoint, Dataset>();
			
			for(Dataset dataset : datasets){
				logger.debug("Loaded dataset: " + dataset);
				endpoint2DatasetMap.put(new Endpoint(dataset.getEndpoint().getLabel()), dataset);
			}
		}catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
		}
	}
	
	
	private String getRootPath(){
		String path = System.getProperty("catalina.home");
		if(path == null){
			return "";
		} else {
			return path + (path.endsWith("/") ? "" : "/");
		}
	}

	@Override
	public PagingLoadResult<Example> getSearchResult(String searchTerm, PagingLoadConfig config) throws AutoSPARQLException{
		logger.info(getUserString() + ":Searching for \"" + searchTerm + "\"");
		return getAutoSPARQLSession().getSearchResult(searchTerm, config);
	}
	
	@Override
	public PagingLoadResult<Example> getQueryResult(String query,
			PagingLoadConfig config) throws AutoSPARQLException {
		logger.info(getUserString() + ":Searching for \"" + query + "\"");
		getAutoSPARQLSession().setQuestion(query);
		return getAutoSPARQLSession().getQueryResult(query, config);
	}

	@Override
	public Example getNextQueryResult(String query)
			throws AutoSPARQLException {
		logger.info("Searching for " + query + "(" + getSession().getId() + ")");
		return getAutoSPARQLSession().getNextQueryResult(query);
	}
	
	@Override
	public Example getSimilarExample(List<String> posExamples,
			List<String> negExamples) throws AutoSPARQLException{
		logger.info(getUserString() + ":Searching similiar example");
		Example example = getAutoSPARQLSession().getSimilarExample(posExamples, negExamples);
		logger.info("Suggestion: " + example.getLabel());
		return example;
	}

	@Override
	public PagingLoadResult<Example> getCurrentQueryResult(
			PagingLoadConfig config) throws SPARQLQueryException {
		return getAutoSPARQLSession().getCurrentQueryResult(config);
	}
	
	public void setExamples(List<String> posExamples,
			List<String> negExamples){
		logger.info(getUserString() + ":Setting positive examples = " + posExamples);
		logger.info(getUserString() + ":Setting negative examples = " + negExamples);
		try{
			getAutoSPARQLSession().setExamples(posExamples, negExamples);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public void setEndpoint(Endpoint endpoint) throws AutoSPARQLException{
		try {
			createNewAutoSPARQLSession(endpoint2DatasetMap.get(endpoint));
			logger.info(getUserString() + ":Set endpoint " + endpoint.getLabel());
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
			throw new AutoSPARQLException(e);
		}
	}
	
	@Override
	public void setQuestion(String question) throws AutoSPARQLException{
		logger.info(getUserString() + ":Set question \"" + question + "\"");
		this.question = question;
	}

	@Override
	public List<Endpoint> getEndpoints() throws AutoSPARQLException{
		try {
			return new ArrayList<Endpoint>(endpoint2DatasetMap.keySet());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
			throw new AutoSPARQLException(e);
		}
	}

	@Override
	public String getCurrentSPARQLQuery() throws AutoSPARQLException {
		return getAutoSPARQLSession().getCurrentQuery();
	}
	
	private void createNewAutoSPARQLSession(Dataset dataset){
		logger.info(getUserString() + ": Start new AutoSPARQL session");
		AutoSPARQLSession session = new AutoSPARQLSession(dataset, cacheDir,
				getServletContext().getRealPath(""), questionProcessor);
		getSession().setAttribute(AUTOSPARQL_SESSION, session);
	}
	
	private AutoSPARQLSession getAutoSPARQLSession(){
		return (AutoSPARQLSession) getSession().getAttribute(AUTOSPARQL_SESSION);
	}
	
	private HttpSession getSession(){
		return getThreadLocalRequest().getSession();
	}

	@Override
	public String getMessage() {
		return "";
	}

	@Override
	public void saveSPARQLQuery() throws AutoSPARQLException{
		logger.info(getUserString() + ":Saving SPARQL query");
		getAutoSPARQLSession().saveSPARQLQuery(store);
	}

	@Override
	public List<StoredSPARQLQuery> getSavedSPARQLQueries() throws AutoSPARQLException{
		try {
			return store.getStoredSPARQLQueries();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error while getting stored SPARQL queries from server.", e);
			throw new AutoSPARQLException(e);
		}
	}

	@Override
	public void loadSPARQLQuery(StoredSPARQLQuery storedQuery) {
		createNewAutoSPARQLSession(endpoint2DatasetMap.get(new Endpoint(storedQuery.getEndpoint())));
		logger.info(getUserString() + ":Loading stored query \"" + storedQuery.getQuestion() + "\"");
		store.incrementHitCount(storedQuery);
		
	}
	
	private void loadSPARQLQueriesFromFile(){
		logger.debug("Loading stored SPARQL queries");
		try {
			store = new SimpleFileStore(storeDir);
			storedSPARQLQueries = store.getStoredSPARQLQueries();
		} catch (Exception e) {
			logger.error("Error while loading stored SPARQL queries.", e);
		}
	}

	@Override
	public PagingLoadResult<Example> getSPARQLQueryResult(String query,
			PagingLoadConfig config) throws AutoSPARQLException {
		logger.debug("Retrieving results for SPARQL query(" + getSession().getId() + ")");
		return getAutoSPARQLSession().getSPARQLQueryResult(query, config);
	}
	
	@Override
	public PagingLoadResult<Example> getSPARQLQueryResultWithProperties(String query, List<String> properties,
			PagingLoadConfig config) throws AutoSPARQLException {
		logger.debug("Retrieving results for SPARQL query with properties(" + getSession().getId() + ")");
		return getAutoSPARQLSession().getSPARQLQueryResultWithProperties(query, properties, config);
	}

	@Override
	public Map<String, String> getProperties(String query) throws AutoSPARQLException {
		logger.debug("Loading properties (" + getSession().getId() + ")");
		return getAutoSPARQLSession().getProperties(query);
	}
	
	private String getUserString(){
		return "USER " + getSession().getId();
	}


}
