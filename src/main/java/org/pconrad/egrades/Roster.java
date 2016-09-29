package org.pconrad.egrades;


import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;


import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import org.pconrad.egrades.Student;

public class Roster {

    private String json = null;

    private HashMap<String,Student> rosterByEmail = new HashMap<String,Student>();

    public boolean isAuthorizedEmail(String emailToCheck) {
	return rosterByEmail.containsKey(emailToCheck);
    }

    /** 
	null if email not found 
     */
    public Student getStudentByEmail(String email) {
	return rosterByEmail.get(email);
    }
    
    @Override
    public String toString() {
	return rosterByEmail.toString();
    }

    public Roster(String mongoClientURIString) throws Exception {

	Logger logger = LoggerFactory.getLogger(Roster.class);	

	MongoClientURI mcuri = new MongoClientURI(mongoClientURIString);
	MongoClient mc = new MongoClient(mcuri);
	MongoDatabase database = mc.getDatabase(mcuri.getDatabase());
		
	if (mc==null || database==null ) {
	    logger.error("Mongo DB Authentication failed.  Check value of MONGO_CLIENT_URI env var");
	    throw new Exception("Can't connect to MongoDB");
	}

	String collectionName = "authorized-emails";
	MongoCollection<Document> collection =
	    database.getCollection(collectionName);

	if(collection==null){
	    logger.error("Please add {} collection to mongo database",
			 collectionName);
		throw new Exception("Collection " +
				    collectionName +
				    " not found");
	}
	Document myDoc = collection.find().first();
	if(myDoc==null){
	    logger.error("Please add at least one document to collection {}",collectionName);
	    throw new Exception("No documents in collection " + collectionName);
	}
	
	this.json = myDoc.toJson();
	System.out.println("json="+this.json);


        JSONArray permArray = JsonPath.parse(json).read("$.roster.*.perm");

	// THIS IS TERRIBLY INEFFICIENT BAD CODE.  It works, but it is NOT good.
	// IT is probably O(n^3) or worse in terms of the number of students, and runs
	// on every hit to the website.   Should be made more efficient, and moved so it runs only
	// once with the website starts up.
	
	java.util.Iterator permIterator = permArray.iterator();
	while (permIterator.hasNext()) {
	    String perm = permIterator.next().toString();
	    JSONArray emailJA = JsonPath.parse(json).read("$.roster[?(@.perm==\"" + perm + "\")].email");
	    String email = emailJA.get(0).toString();
	    String fname = extractFieldFromJsonByPerm(json, perm, "fname");
	    String lname = extractFieldFromJsonByPerm(json, perm, "lname");
	    Student s = new Student(perm, fname, lname, email);
	    this.rosterByEmail.put(email,s);
	}

    }

    public static String extractFieldFromJsonByPerm(String json, String perm, String field) {
	JSONArray ja = JsonPath.parse(json).read("$.roster[?(@.perm==\"" + perm + "\")]." + field );
	String value = ja.get(0).toString();
	return value;
    }
    
    
}
