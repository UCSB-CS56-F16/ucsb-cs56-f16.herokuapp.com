package org.pconrad.webapps.sparkjava;

import org.pconrad.egrades.Roster;

import javax.servlet.MultipartConfigElement; // for uploading file
import java.util.Scanner;

import java.util.HashMap;
import java.util.Map;
import static java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.ModelAndView;

import spark.Spark;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.before;
import static spark.Spark.halt;

import spark.Request;
import spark.Response;

import org.pac4j.core.config.Config;
import org.pac4j.sparkjava.SecurityFilter;
import org.pac4j.sparkjava.ApplicationLogoutRoute;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.sparkjava.SparkWebContext;

import org.pac4j.core.engine.DefaultApplicationLogoutLogic;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepository.Contributor;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHOrganization;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;


import java.io.InputStream;


import org.pac4j.oauth.profile.github.GitHubProfile;

import java.util.Collection;


import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;


import com.jayway.jsonpath.JsonPath;

/**
   Demo of Spark Pac4j with Github OAuth

   @author pconrad
 */
public class GithubOrgUtilityWebapp {

	private static String GITHUB_ORG = null;

	private static String [] adminGithubIds = null;
    
    private static java.util.List<CommonProfile> getProfiles(final Request request,
						   final Response response) {
	final SparkWebContext context = new SparkWebContext(request, response);
	final ProfileManager manager = new ProfileManager(context);
	return manager.getAll(true);
    }    
    
    private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

    /** 
	add github information to the session

    */
    private static Map addGithub(Map model, Request request, Response response) {
	GitHubProfile ghp = ((GitHubProfile)(model.get("ghp")));
	if (ghp == null) {
	    // System.out.println("No github profile");
	    return model;
	}
	try {
	    String accessToken = ghp.getAccessToken();
	    GitHub gh = null;
	    String org_name = model.get("org_name").toString();
	    gh =  GitHub.connect( model.get("userid").toString(), accessToken);
	    java.util.Map<java.lang.String,GHRepository> repos = null;
	    GHOrganization org = gh.getOrganization(org_name);
	    if (org != null) {
		repos = org.getRepositories();
	    }

	    java.util.HashMap<String, CS56ProjectRepo> cs56repos = 
	    	new java.util.HashMap<String, CS56ProjectRepo>();

	 	for (Map.Entry<String, GHRepository> entry : repos.entrySet()) {
		    String repoName = entry.getKey();
    		GHRepository repo = entry.getValue();

			java.util.List<GHIssue> issues = repo.getIssues(GHIssueState.OPEN);

    		// javadoc for GHRepository: http://github-api.kohsuke.org/apidocs/index.html

    		CS56ProjectRepo pr = new CS56ProjectRepo(
    				repoName,
    				repo.getUrl().toString(),
    				repo.getHtmlUrl().toString(),
    				repo.getDescription(),
    				issues.size()
    			);

    		cs56repos.put(repoName,pr);

		}		



	    if (org != null && cs56repos != null) {
		model.put("repos",cs56repos.entrySet());
	    } else {
		model.remove("repos");
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return model;
    }


    
    private static Map buildModel(Request request, Response response) {
		
		final Map model = new HashMap<String,Object>();
		Logger logger = LoggerFactory.getLogger(GithubOrgUtilityWebapp.class);
		
		model.put("org_name", GithubOrgUtilityWebapp.GITHUB_ORG);

		// THIS BLOCK HANDLES AUTHENTICATION FROM PROFILE

		// First, we assume that we are not authenticated and have no profile

		request.session().attribute("admin","");		
		request.session().attribute("login","");		
		java.util.List<CommonProfile> userProfiles = getProfiles(request,response);
		
		try {
			if (userProfiles.size()>0) {
				CommonProfile firstProfile = userProfiles.get(0);
				
				GitHubProfile ghp = (GitHubProfile) firstProfile;
				
				String githubLogin = ghp.getUsername();
				
				model.put("ghp", ghp);
				model.put("userid",githubLogin);
				request.session().attribute("login",githubLogin);

				model.put("name",ghp.getDisplayName());
				model.put("avatar_url",ghp.getPictureUrl());
				model.put("email",ghp.getEmail());
				

				for ( String id : GithubOrgUtilityWebapp.adminGithubIds) {
					if (githubLogin.equals(id)) {
						logger.info("Admin user: {}",id);
						model.put("admin","admin"); // otherwise ABSENT
						request.session().attribute("admin","admin");
						break;
					}
		}

	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	

		// IF THIS IS THE ROSTER ROUTE... 
		//  TODO: Fix this horrible spaghetti code.  Yuck.

		logger.info("request.contextPath()={}",request.contextPath());
		logger.info("request.raw().getRequestURI()={}", request.raw().getRequestURI());

		// SEE: https://bz.apache.org/bugzilla/show_bug.cgi?id=28323
		// SEE: http://stackoverflow.com/questions/4278083/how-to-get-request-uri-without-context-path

		String uri = request.raw().getRequestURI();
		if (uri.equals("/roster")) {
			logger.info("/roster path... inside buildModel()...");

			Roster roster = new Roster();
			model.put("roster",roster);

		}


		// LAST put all session values into the model under the key "session" 
		// Must be last if what you see in the model is to be an accurate reflection
		// of all changes to session made in code above.
		
		Map<String, Object> map = new HashMap<String, Object>();
		for (String k: request.session().attributes()) {
			Object v = request.session().attribute(k);
			map.put(k,v);
		}
		model.put("session", map.entrySet());
		
		return model;	
    }
	
    /**

       return a HashMap with values of all the environment variables
       listed; print error message for each missing one, and exit if any
       of them is not defined.
    */
    
    public static HashMap<String,String> getNeededEnvVars(String [] neededEnvVars) {
	HashMap<String,String> envVars = new HashMap<String,String>();
	
	
	for (String k:neededEnvVars) {
	    String v = System.getenv(k);
	    envVars.put(k,v);
	}
	
	boolean error=false;
	for (String k:neededEnvVars) {
	    if (envVars.get(k)==null) {
		error = true;
		System.err.println("Error: Must define env variable " + k);
	    }
	}
	if (error) { System.exit(1); }
	
	return envVars;
    }
    
    public static void main(String[] args) {

    Logger logger = LoggerFactory.getLogger(GithubOrgUtilityWebapp.class);
    logger.info("GithubOrgUtilityWebapp starting up");

	
	HashMap<String,String> envVars =
	    getNeededEnvVars(new String []{ "GITHUB_CLIENT_ID",
										"GITHUB_CLIENT_SECRET",
										"GITHUB_CALLBACK_URL",
										"APPLICATION_SALT",
										"MONGO_CLIENT_URI",
										"ADMIN_GITHUB_IDS",
			                            "GITHUB_ORG"});
	


	MongoClientURI mcuri = new MongoClientURI( envVars.get("MONGO_CLIENT_URI"));
	MongoClient mc = new MongoClient(mcuri);
	MongoDatabase database = mc.getDatabase(mcuri.getDatabase());			
	
	if (mc==null || database==null ) {
		logger.error("Mongo DB Authentication failed.  Check value of MONGO_CLIENT_URI env var");
		System.exit(3);
	}
	
	String ADMIN_GITHUB_IDS=envVars.get("ADMIN_GITHUB_IDS");
	GithubOrgUtilityWebapp.GITHUB_ORG=envVars.get("GITHUB_ORG");

	GithubOrgUtilityWebapp.adminGithubIds=ADMIN_GITHUB_IDS.split(",");

	Spark.staticFileLocation("/static");
	
	try {
	    // needed for Heroku
	    Spark.port(Integer.valueOf(System.getenv("PORT"))); 
	} catch (Exception e) {
	    System.err.println("NOTICE: using default port." +
			       " Define PORT env variable to override");
	}

	Config config = new
	    GithubOAuthConfigFactory(envVars.get("GITHUB_CLIENT_ID"),
				     envVars.get("GITHUB_CLIENT_SECRET"),
				     envVars.get("GITHUB_CALLBACK_URL"),
				     envVars.get("APPLICATION_SALT"),
				     templateEngine,
				     "repo").build();

	final SecurityFilter
	    githubFilter = new SecurityFilter(config, "GithubClient", "", "");

	get("/",
	    (request, response) -> new ModelAndView(buildModel(request,response),"home.mustache"),
	    templateEngine);

	before("/login", githubFilter);

	get("/login",
	    (request, response) -> new ModelAndView(buildModel(request,response),"home.mustache"),
	    templateEngine);

	get("/logout", new ApplicationLogoutRoute(config, "/"));
	
	before("/roster",
		   (request, response) -> { 
			   logger.info("/roster before filter: entering");
			   String login = request.session().attribute("login");
			   String session_admin = request.session().attribute("admin");
			   if ( login==null || login.equals("") ||
					session_admin == null || !session_admin.equals("admin")) {
						logger.info("/roster before filter: login: {},  session_admin: {}",
									login,session_admin);
				   halt(401,"/roster route requires admin login");
			   }
		   });

	before("/rosterUpload",
		   (request, response) -> { 
			   logger.info("/roster before filter: entering");
			   String login = request.session().attribute("login");
			   String session_admin = request.session().attribute("admin");
			   if ( login==null || login.equals("") ||
					session_admin == null || !session_admin.equals("admin")) {
						logger.info("/roster before filter: login: {},  session_admin: {}",
									login,session_admin);
				   halt(401,"/roster route requires admin login");
			   }
		   });



	get("/roster",
	    (request, response) -> new ModelAndView(buildModel(request,response),
						    "roster.mustache"),
	    templateEngine);


	post("/rosterUpload", (request, response) -> {
			request.attribute("org.eclipse.jetty.multipartConfig", 
							  new MultipartConfigElement("/temp"));
			try (InputStream is = request.raw().getPart("uploaded_file").getInputStream()) {
					Scanner s = new Scanner(is).useDelimiter("\\A");
					String result = s.hasNext() ? s.next() : "";
					logger.info("uploaded file: {}",result);
				}
			return "File uploaded";
		});

	final org.pac4j.sparkjava.CallbackRoute callback =
	    new org.pac4j.sparkjava.CallbackRoute(config);

	get("/callback", callback);
	post("/callback", callback);

    }
}
