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

public class EGradesReader {

	public static void main(String [] args) {

		String usage = 
			"Usage: java -cp target/github-org-utility-webapp-1.0.jar org.pconrad.egrades.EGradesReader file.csv CS56 F16";
		
		if (args.length != 3) {
			System.err.println(usage);
			System.exit(1);
		}
		
		String filename = args[0];
		String course = args[1];
		String quarter = args[2];
		System.err.println("Opening " + filename + " for course " + course + " quarter: " + quarter); 

		ArrayList<Document> roster = new ArrayList<Document>();

		try {
			Reader in = new FileReader(filename);			
			CSVFormat format = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord();
			CSVParser records = format.parse(in);

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
			new Document("course",course)
			.append("quarter",quarter)
			.append("roster",roster);

		MongoDatabase database = mc.getDatabase(mcuri.getDatabase());			
		database.getCollection("authorized-emails").insertOne(d);
	}


}