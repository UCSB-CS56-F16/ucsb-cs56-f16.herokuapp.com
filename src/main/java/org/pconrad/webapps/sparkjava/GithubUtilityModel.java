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
public class GithubUtilityModel extends HashMap<String,Object> {
    
    public final HashMap<String,String> envVars;
	public final Request request;
	public final Response response;

	public GithubUtilityModel(HashMap<String,String> envVars, Request request, Response response) {
		this.envVars = envVars;
		this.request = request;
		this.response = response;
	}
    

    /** 
		add github information to the session
		
    */
	
    private GithubUtilityModel addGithub() {
		GitHubProfile ghp = ((GitHubProfile)(this.get("ghp")));
		if (ghp == null) {
			// System.out.println("No github profile");
			return this;
		}
		try {
			String accessToken = ghp.getAccessToken();
			GitHub gh = null;
			gh =  GitHub.connect( this.get("userid").toString(), accessToken);				
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

       
    public GithubUtilityModel build() {
		
		Logger logger = LoggerFactory.getLogger(GithubUtilityModel.class);
		
		this.put("org_name", GithubOrgUtilityWebapp.GITHUB_ORG);
		
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
				
				this.put("ghp", ghp);
				this.put("userid",githubLogin);
				request.session().attribute("login",githubLogin);
				
				this.put("name",ghp.getDisplayName());
				this.put("avatar_url",ghp.getPictureUrl());
				this.put("email",ghp.getEmail());
				
				
				for ( String id : GithubOrgUtilityWebapp.adminGithubIds) {
					if (githubLogin.equals(id)) {
						logger.info("Admin user: {}",id);
						this.put("admin","admin"); // otherwise ABSENT
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
			
			
			Roster roster = null;
			try {
			    roster = new Roster(envVars.get("MONGO_CLIENT_URI"));
			    this.put("roster",roster);
			} catch (Exception e) {
			    e.printStackTrace();
			}
			
		}

		// LAST put all session values into the model under the key "session" 
		// Must be last if what you see in the model is to be an accurate reflection
		// of all changes to session made in code above.
		
		Map<String, Object> map = new HashMap<String, Object>();
		for (String k: request.session().attributes()) {
			Object v = request.session().attribute(k);
			map.put(k,v);
		}
		this.put("session", map.entrySet());
		
		return this;	
    } // build()
	
    private static java.util.List<CommonProfile> getProfiles(final Request request,
															 final Response response) {
		final SparkWebContext context = new SparkWebContext(request, response);
		final ProfileManager manager = new ProfileManager(context);
		return manager.getAll(true);
    }    

	
}
