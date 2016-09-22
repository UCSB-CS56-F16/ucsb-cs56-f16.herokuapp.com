package org.pconrad.webapps.sparkjava;

import java.io.Reader;
import java.io.FileReader;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import static org.pconrad.webapps.sparkjava.GithubOrgUtilityWebapp.getNeededEnvVars;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;

import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

public class WriteAdmin {

	public static void main(String [] args) {

		String usage = 
			"Usage: java -cp target/github-org-utility-webapp-1.0.jar org.pconrad.webapps.sparkjava.WriteAdmin admin-github-id";
		
		if (args.length != 1) {
			System.err.println(usage);
			System.exit(1);
		}
		
		String admin_github_id = args[0];
		System.err.println("Admin github id:" + admin_github_id); 

		java.util.HashMap<String,String> envVars =
			getNeededEnvVars(new String []{ "MONGO_CLIENT_URI"});
		
		MongoClientURI mcuri = new MongoClientURI( envVars.get("MONGO_CLIENT_URI"));
		MongoClient mc = new MongoClient(mcuri);
		
		if (mc==null) {
			System.err.println("Mongo DB Authentication failed.  Check values of MONGO_CLIENT_URI env vars");
			System.exit(3);
		}

		Document d = new Document("admin_github_id",admin_github_id);
		MongoDatabase database = mc.getDatabase(mcuri.getDatabase());			
		database.getCollection("admin").insertOne(d);
	}


}