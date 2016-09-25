package org.pconrad.webapps.sparkjava;

/**
	class to represent only the fields WE need to know about
	for a UCSB-CS56-Projects repo
	
*/
public class CS56ProjectRepo {
	
	public String  name;
	public String  apiUrl;
	public String  htmlUrl;
	public String  description;
	public int numOpenIssues;
	
	public CS56ProjectRepo (String name, String apiUrl,	String htmlUrl,	String description, int numOpenIssues) {
		this.name = name;
		this.apiUrl = apiUrl;
		this.htmlUrl = htmlUrl;
		this.description = description;
		this.numOpenIssues = numOpenIssues;
	}

}