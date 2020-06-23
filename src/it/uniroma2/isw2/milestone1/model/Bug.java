package it.uniroma2.isw2.milestone1.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Bug {

	private String id;
	private LocalDate discoveryDate;
	private List<Integer> avs;
	private int fv = 0;
	private int ov = 0;
	private List<String> filenames = new ArrayList<>();
	
	
	public String getID() {
		return id;
	}
	public void setID(String id) {
		this.id = id;
	}
	public LocalDate getDiscoveryDate() {
		return discoveryDate;
	}
	public void setDiscoveryDate(LocalDate discoveryDate) {
		this.discoveryDate = discoveryDate;
	}
	public List<Integer> getAVs() {
		return avs;
	}
	public void setAVs(List<Integer> avs) {
		this.avs = avs;
	}
	public int getFV() {
		return fv;
	}
	public void setFV(int fv) {
		this.fv = fv;
	}
	public int getOV() {
		return ov;
	}
	public void setOV(int ov) {
		this.ov = ov;
	}
	public List<String> getFilenames() {
		return filenames;
	}
	public void setFilenames(List<String> filenames) {
		this.filenames = filenames;
	}
	public void print() {
		//Useful in debugging 
		Logger logger = LogManager.getLogger(Bug.class);
		logger.info("Bug " + this.getID() + " discovered at " + this.getDiscoveryDate());
		logger.info("Affected Versions: ");
		for (int i=0; i<this.avs.size(); i++) {
			logger.info(avs.get(i) + " - ");
		}
		logger.info("Opening Version: " + this.ov);
		logger.info("Fixed Version: " + this.fv);

		int c = 0;
		for (int i=0; i<this.filenames.size(); i++) {
			c++;
		}
		logger.info("Files affected: " + c);
	}
	
}
