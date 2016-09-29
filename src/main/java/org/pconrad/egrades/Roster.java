package org.pconrad.egrades;


import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;


import com.jayway.jsonpath.JsonPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Roster {

    public String json = null;

    public ArrayList<String> emails = new ArrayList<String>();
    
    @Override
    public String toString() {
	return emails.toString();
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


	net.minidev.json.JSONArray emailsArray = JsonPath.parse(json).read("$.roster.*.email");

	java.util.Iterator emailIterator = emailsArray.iterator();
	while (emailIterator.hasNext()) {
	    String email = emailIterator.next().toString();
	    emails.add(email);
	}

    }

    
}
