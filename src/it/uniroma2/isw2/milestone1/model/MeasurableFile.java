package it.uniroma2.isw2.milestone1.model;

import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class MeasurableFile {
	
	private String name;
	private String version;
	
	//Metrics 
	private int loc;
	private double age;
	private int nr;
	private int changeset;
	private double averageChangeSet;
	private int maxChangeSet;
	private int nAuth;
	private int locAdded;
	private double avgLocAdded;
	
	private boolean buggyness;
	
	
	public MeasurableFile() {
		this.loc = 0;
		this.age = 0;
		this.nr = 0;
		this.changeset = 0;
		this.averageChangeSet = 0;
		this.maxChangeSet = 0;
		this.nAuth = 0;
		this.locAdded = 0;
		this.avgLocAdded = 0;
		this.buggyness = false;
	}

	public void print() {
		Logger logger = LogManager.getLogger(MeasurableFile.class);
		logger.info("Version: " + this.version);
		logger.info("Filename: " + this.name);
		logger.info("LOC: " + this.loc);
		logger.info("Age: " + this.age);
		logger.info("Number of Revisions: " + this.nr);
		logger.info("Change Set: " + this.changeset);
		logger.info("Average Change Set: " + this.averageChangeSet);
		logger.info("Max Change Set: " + this.maxChangeSet);
		logger.info("Number of Authors: " + this.nAuth);
		logger.info("LOC Added Sum: " + this.locAdded);
		logger.info("Average LOC Added: " + this.avgLocAdded);
		logger.info("Buggy?: " + this.buggyness);
	}
	
	public void printonFile(FileWriter f) throws IOException {
		f.append(this.version + ",");
		f.append(this.name + ",");
		f.append(this.loc + ",");
		f.append(this.age + ",");
		f.append(this.nr + ",");
		f.append(this.changeset + ",");
		f.append(this.averageChangeSet + ",");
		f.append(this.maxChangeSet + ",");
		f.append(this.nAuth + ",");
		f.append(this.locAdded + ",");
		f.append(this.avgLocAdded + ",");
		f.append(this.buggyness + "\n");
	}
	
	public int getLoc() {
		return loc;
	}

	public void setLoc(int loc) {
		this.loc = loc;
	}

	public double getAge() {
		return age;
	}

	public void setAge(double age) {
		this.age = age;
	}

	public int getNr() {
		return nr;
	}

	public void setNr(int nr) {
		this.nr = nr;
	}

	public int getChangeset() {
		return changeset;
	}

	public void setChangeset(int changeset) {
		this.changeset = changeset;
	}

	public double getAverageChangeSet() {
		return averageChangeSet;
	}

	public void setAverageChangeSet(double averageChangeSet) {
		this.averageChangeSet = averageChangeSet;
	}

	public int getMaxChangeSet() {
		return maxChangeSet;
	}

	public void setMaxChangeSet(int maxChangeSet) {
		this.maxChangeSet = maxChangeSet;
	}

	public int getnAuth() {
		return nAuth;
	}

	public void setnAuth(int nAuth) {
		this.nAuth = nAuth;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int getLocAdded() {
		return locAdded;
	}

	public void setLocAdded(int locAdded) {
		this.locAdded = locAdded;
	}

	public double getAvgLocAdded() {
		return avgLocAdded;
	}

	public void setAvgLocAdded(double avgLocAdded) {
		this.avgLocAdded = avgLocAdded;
	}

	public boolean isBuggy() {
		return buggyness;
	}

	public void setBuggyness(boolean buggyness) {
		this.buggyness = buggyness;
	}
}
