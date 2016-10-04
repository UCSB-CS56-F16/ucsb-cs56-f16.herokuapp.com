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
    
    public static HashMap<String,String> envVars =
	getNeededEnvVars(new String []{ "GITHUB_CLIENT_ID",
					"GITHUB_CLIENT_SECRET",
					"GITHUB_CALLBACK_URL",
					"APPLICATION_SALT",
					"MONGO_CLIENT_URI",
					"ADMIN_GITHUB_IDS",
					"GITHUB_ORG"});
    
    public static String GITHUB_ORG = null;
    
    public static String [] adminGithubIds = null;
    
    private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

	
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
				     "user:email,repo").build();

	final SecurityFilter
	    githubFilter = new SecurityFilter(config, "GithubClient", "", "");

	get("/",
	    (request, response) -> 
		new ModelAndView(new GithubUtilityModel(envVars,request,response).build(),
						 "home.mustache"),
	    templateEngine);

	before("/login", githubFilter);

	get("/login",
	    (request, response) -> 
		new ModelAndView(new GithubUtilityModel(envVars,request,response).build(),
						 "home.mustache"),
	    templateEngine);

	get("/logout", new ApplicationLogoutRoute(config, "/"));


	before("/profile",
		   (request, response) -> { 
			   logger.info("/profile before filter: entering");
			   String login = request.session().attribute("login");
			   if ( login==null || login.equals("") ) {
				   halt(401,"/profile route requires user to be logged in");
			   }
		   });
	

	get("/profile",
	    (request, response) -> 
		new ModelAndView(new GithubUtilityModel(envVars,request,response).build().addGithubEmails(),
						 "profile.mustache"),
	    templateEngine);
		  
	
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
	    (request, response) -> new ModelAndView(new GithubUtilityModel(envVars,request,response).build(),
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
