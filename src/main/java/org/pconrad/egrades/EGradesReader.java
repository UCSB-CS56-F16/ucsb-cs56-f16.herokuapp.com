package org.pconrad.egrades;

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

import java.io.InputStream;

public class EGradesReader {

	private Reader reader = null;
	
	public EGradesReader(Reader reader) {
		this.reader = reader;
	}
	
	public void perform() {
		
		ArrayList<Document> roster = new ArrayList<Document>();
		
		try {

			CSVFormat format = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord();
			CSVParser records = format.parse(this.reader);
			
			for (CSVRecord record : records) {
				String perm = record.get("Perm #").trim();
				String lname = WordUtils.capitalizeFully(record.get("Student Last").trim());
				String fname = WordUtils.capitalizeFully(record.get("Student First Middle").trim());
				String email = record.get("Email").trim();
				
				Student s = new Student(perm,lname,fname,email);
				System.out.println(s);
				roster.add(s.toDocument());

			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}
			
		java.util.HashMap<String,String> envVars =
			getNeededEnvVars(new String []{ "MONGO_CLIENT_URI"});
				
		MongoClientURI mcuri = new MongoClientURI( envVars.get("MONGO_CLIENT_URI"));
		MongoClient mc = new MongoClient(mcuri);
		
		if (mc==null) {
			System.err.println("Mongo DB Authentication failed.  Check values of MONGO_CLIENT_URI env vars");
			System.exit(3);
		}

		Document d = 
			new Document("roster",roster);

		MongoDatabase database = mc.getDatabase(mcuri.getDatabase());			
		database.getCollection("authorized-emails").insertOne(d);
	} // perform

	public static void main(String [] args) {
		
		String usage = 
			"Usage: java -cp target/github-org-utility-webapp-1.0.jar org.pconrad.egrades.EGradesReader file.csv";
		
		if (args.length != 1) {
			System.err.println(usage);
			System.exit(1);
		}
		
		String filename = args[0];

		try {
			Reader r = new FileReader(args[0]);
			EGradesReader egr = new EGradesReader(r);
			egr.perform();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(3);
		}
	} // main

}// class