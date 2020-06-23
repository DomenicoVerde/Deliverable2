package it.uniroma2.isw2.milestone1.model;

import java.time.LocalDate;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Ref;

public class Release {
	private int index;
	private String name;
	private LocalDate releaseDate;
	private Ref tag;
	
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public LocalDate getReleaseDate() {
		return releaseDate;
	}
	public void setReleaseDate(LocalDate date) {
		this.releaseDate = date;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Ref getTag() {
		return tag;
	}
	public void setTag(Ref tag) {
		this.tag = tag;
	}
	
	public void print() {
		Logger logger = LogManager.getLogger(Release.class);
		logger.info("Release " + this.getIndex() + ": " + this.getName());
		logger.info("release date: " + this.getReleaseDate());
		logger.info("tag: " + this.getTag());
	}
}
