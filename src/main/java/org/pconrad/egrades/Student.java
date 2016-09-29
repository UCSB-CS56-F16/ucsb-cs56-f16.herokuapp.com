package org.pconrad.egrades;

public class Student {
	
	public String perm;
	public String lname;
	public String fname;
	public String email;
	
	public Student (String perm, String lname, String fname, String email) {
		this.perm = perm;
		this.lname = lname;
		this.fname = fname;
		this.email = email;
	}
	
	public String toString() {
		return "{ " +
		    "\"perm\" : \"" +  perm + "\"," +
		    "\"lname\" : \"" + lname + "\"," +
		    "\"fname\" : \"" + fname + "\"," +
		    "\"email\" : \"" + email + "\"," +
		    "}";
	}
	
	public org.bson.Document toDocument() {
		return new org.bson.Document("perm",perm)
			.append("lname",lname)
			.append("fname",fname)
			.append("email",email);
	}
}

