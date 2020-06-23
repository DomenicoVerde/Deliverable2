package it.uniroma2.isw2.milestone1;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.json.JSONException;
import org.json.JSONObject;

import it.uniroma2.isw2.milestone1.model.Bug;
import it.uniroma2.isw2.milestone1.model.MeasurableFile;
import it.uniroma2.isw2.milestone1.model.Release;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.json.JSONArray;

public class Main {
	
    private static Map<LocalDateTime, String> releaseNames = null;
	private static List<LocalDateTime> releases = null;
	private static int skipped = 0;
	private static final Logger logger = LogManager.getLogger(Main.class);
	
	private static double p = 1.000;
	
	public static void addRelease(String strDate, String name) {
		LocalDate date = LocalDate.parse(strDate);
		LocalDateTime dateTime = date.atStartOfDay();
		if (!releases.contains(dateTime))
			releases.add(dateTime);
		releaseNames.put(dateTime, name);
	}

	public static JSONObject readJsonFromUrl(String url) throws 
		IOException, JSONException {
	      InputStream is = new URL(url).openStream();
	      try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){        
		         String jsonText = readAll(rd);
		         return new JSONObject(jsonText);
	      } finally {
	    	  is.close();
	      }
	}
   
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
	
	private static void getJiraReleases(String projName) throws IOException,
	  JSONException {
		//Fills the Arraylist with releases dates and orders them
		//Ignores releases with missing dates
		releases = new ArrayList<>();
		Integer i;
		String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
		JSONObject json = readJsonFromUrl(url);
		JSONArray versions = json.getJSONArray("versions");
		releaseNames = new HashMap<>();
	    for (i = 0; i < versions.length(); i++ ) {
	    	String name = "";
	        if(versions.getJSONObject(i).has("releaseDate")) {
	        	if (versions.getJSONObject(i).has("name"))
	            	name = versions.getJSONObject(i).get("name").toString();
	            addRelease(versions.getJSONObject(i).get("releaseDate").toString(), name);
	        }
	    }
	    // order releases by date
	    Collections.sort(releases, (o1, o2) -> o1.compareTo(o2));
	}

	private static Git makeRepository(File dir, String projName) throws 
		InvalidRemoteException, TransportException, GitAPIException {
		//Clones or sets the repository of the projects
		Git git;
		if (dir.length() == 0) {
			git = Git.cloneRepository().setURI("https://gitbox.apache.org/repos/asf/" 
					+ projName.toLowerCase() + ".git")
				   .setDirectory(dir)
				   .setCloneAllBranches(true)
				   .call();
		} else {
			git = Git.init().setDirectory(dir).call();
		}
		return git;
	}
	
	private static String readFilefromCommit(RevCommit c, Git git, String path) throws
			MissingObjectException, IncorrectObjectTypeException, IOException {
        
        OutputStream buffer = new ByteArrayOutputStream();
        
        DiffFormatter df = new DiffFormatter(buffer);
        df.setRepository(git.getRepository());
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setPathFilter(PathFilter.create(path));
        
        List<DiffEntry> diffs = df.scan(null, c);
        df.format(diffs.get(0));
        String content = buffer.toString();
        df.close();
        return content;
    }
	
	private static int getLines(String content) {
    	int count = 0;
    	for (int i = 0; i < content.length(); i++) {
    	    if (content.charAt(i) == '\n')
    	        count++;
    	}
    	return count;
	}
	
	private static int getChangeSet(RevCommit oldCommit, RevCommit rev,Git git) throws 
			GitAPIException, IOException  {
		try ( DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE) ) {
        df.setRepository(git.getRepository());
        df.setDiffComparator(RawTextComparator.DEFAULT);
        return df.scan(oldCommit, rev).size();
        }
	}
	
	private static int countLocAdded(RevCommit previous, RevCommit commit, Git git, String filename) throws 
			CorruptObjectException, MissingObjectException, IOException, GitAPIException {
		//if commit is the first commit 
		if (previous == null) {
			return 0;
		}
        int addedLines = 0;
        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(git.getRepository());
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setPathFilter(PathFilter.create(filename));
        List<DiffEntry> diffs = df.scan(previous, commit);
        for (DiffEntry diff : diffs) {
            for (Edit edit : df.toFileHeader(diff).toEditList()) {
                addedLines += edit.getEndB() - edit.getBeginB();
            }
        }
        df.close();
        return addedLines;
	}
	
	public static JSONArray getJiraBugs(String projName) throws IOException,
	  JSONException {
		//Get all the bugs for this project from Jira
		Integer j = 0;
		Integer i = 0;
		Integer total = 1;
		JSONArray jiraResults = new JSONArray();
		do {
			//Only gets a max of 1000 at a time, so must do this multiple times if bugs > 1000
			j = i + 1000;
			String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
					+ projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
					+ "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed"
					+ "%22&fields=key,versions,fixVersions,created&startAt="
					+ i.toString() + "&maxResults=" + j.toString();
			JSONObject json = readJsonFromUrl(url);
			JSONArray issues = json.getJSONArray("issues");
			total = json.getInt("total");
			for (; i < total && i < j; i++) {
				//Storing each result in an array
				jiraResults.put(issues.getJSONObject(i%1000)); 
			}  
		} while (i < total);
		
		return jiraResults;
	}
	
	public static List<Bug> jsonBugConverter(JSONArray jsonBugs, List<Release> releases) throws JSONException {
		//Convert a JSON Object to a Bug Object
		List<Bug> bugs = new ArrayList<>();
		String fields = "fields";
		
		for (int i=0; i<jsonBugs.length(); i++) {
			//Setting BugID
			JSONObject jsonBug = jsonBugs.getJSONObject(i);
			Bug bug = new Bug();
			bug.setID(jsonBug.getString("key"));
			
			//Setting AVs
			adaptAVs(jsonBug, bug, releases);
			
			//Setting up bug discovery date
			String jsonDate = jsonBug.getJSONObject(fields).getString("created");
			LocalDate date = parseDate(jsonDate);
			bug.setDiscoveryDate(date);
			
			//Setting OV
			for (Release rel: releases) {
				LocalDate relDate = rel.getReleaseDate();
				if (bug.getDiscoveryDate().isBefore(relDate)) {
					bug.setOV(rel.getIndex());
					break;
				}
			}
			
			//Setting FV
			JSONArray jsonFVs = jsonBug.getJSONObject(fields).getJSONArray("fixVersions");
			if (jsonFVs.length() > 0) {
				String name = jsonFVs.getJSONObject(jsonFVs.length()-1).get("name").toString();
				for (Release rel: releases) {
					if (rel.getName().contentEquals(name)) {
						bug.setFV(rel.getIndex());
						break;
					}
				}
			}				
			bugs.add(bug);
		}
		
		return bugs;
	}
	
	private static void adaptAVs(JSONObject jsonBug, Bug bug,  List<Release> releases) throws JSONException {
		
		JSONArray jsonAVs = jsonBug.getJSONObject("fields").getJSONArray("versions");
		List<Integer> avs = new ArrayList<>();
		
		//if the json is empty don't update AV, it has to be estimated
		if (jsonAVs.length() == 1) {
			
			String name = jsonAVs.getJSONObject(0).get("name").toString();
			for (Release rel: releases) {
				if (rel.getName().contentEquals(name)) {
					avs.add(rel.getIndex());
					break;
				}
			}
		} else if (jsonAVs.length() > 1) {
			
			//Json versions are ordered: take the first and last e considering affected all version between these
			String first = jsonAVs.getJSONObject(0).get("name").toString();
			String last = jsonAVs.getJSONObject(jsonAVs.length()-1).get("name").toString();
			for (Release rel: releases) {
				if (rel.getName().contentEquals(first)) {
					avs.add(rel.getIndex());
				}
				if (rel.getName().contentEquals(last)) {
					avs.add(rel.getIndex());
				}
			}
		}
		bug.setAVs(avs);
	}
	
	public static LocalDate parseDate(String date) {
		String[] ymd = date.split("-");
		String[] dt = ymd[2].split("T");
		return LocalDate.of(Integer.parseInt(ymd[0]), 
				Integer.parseInt(ymd[1]), Integer.parseInt(dt[0]));
	}
	
	private static List<Release> makeReleases(Git git) throws GitAPIException {
		//Mapping Jira releases with Git Tags, creating a list of release for this project
		List<Ref> allTags = git.tagList().call();
	    List<Release> versions = new ArrayList<>();
	    for (int i=0; i<releaseNames.size(); i++) {
	    	String name = releaseNames.get(releases.get(i));
	    	LocalDate jiradate = releases.get(i).toLocalDate();
	    	for (Ref ref : allTags) {
	    		if (ref.getName().contains(name) && !ref.getName().contains("rc")) {
	    			//Skip Git Release Candidates
    				Release rel = new Release();
    				rel.setName(name);
    				rel.setTag(ref);
    				rel.setReleaseDate(jiradate);
    				rel.setIndex(i+1);
    				versions.add(rel);
    				break;
	    		}
	    	}
	    }
		return versions;
	}
	
	private static List<Bug> matchBugs (List<Bug> bugs, Git git) throws NoHeadException,
 GitAPIException, IOException {
		
	    List<Bug> filteredBugs = new ArrayList<>();
		DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(git.getRepository());
        df.setDiffComparator(RawTextComparator.DEFAULT);
	    for (int index = 0; index < bugs.size(); index++) {
	    	RevCommit fixCommit = null;
	    	RevCommit prevCommit = null;
	    	RevCommit commit = null;
	    	Iterable<RevCommit> commits = git.log().all().call();
	    	Iterator<RevCommit> itr = commits.iterator();
	    	Bug bug = bugs.get(index);

	    	while (itr.hasNext()) {
	    		commit = itr.next();
	    		//log commits are ordered -> the first is the last commit with that id
	    		if (commit.getFullMessage().contains(bug.getID())) {
	    			fixCommit = commit;
		    		if (itr.hasNext()) {
		    			prevCommit = itr.next();
		    			break;
		    		}
	    		}
	    	}
	    	
	    	if (fixCommit == null) {
	    		filteredBugs.add(bugs.get(index));
	    	} else {
	    		List<String> files = retrieveFilenames(df, prevCommit, fixCommit);
		    	bug.setFilenames(files);
	    	}
	    }
	    bugs.removeAll(filteredBugs);
	    df.close();
		return bugs;
	}
	
	private static List<String> retrieveFilenames(DiffFormatter df, RevCommit prevCommit, RevCommit fixCommit) throws IOException {
		List<String> files = new ArrayList<>();
		List<DiffEntry> x = df.scan(prevCommit, fixCommit);
    	for (DiffEntry diff : x) {
    		//report only java files
    		if (diff.getPath(null).contains(".java"))
    			files.add(diff.getPath(null));
    	}
    	return files;
	}

	private static List<Bug> filter(List<Bug> bugs) {
		List<Bug> filteredBugs = new ArrayList<>();
	    for (int b=0; b<bugs.size(); b++) {
	    	//if IV = FV, discard  it (bug not-post-release)
	    	if (bugs.get(b).getAVs().size() > 0 && bugs.get(b).getAVs().get(0).intValue() == bugs.get(b).getFV()) {
	    		filteredBugs.add(bugs.get(b));
	    	}
	    	//if no FV, discard it
	    	if (bugs.get(b).getFV() <= 0) {
	    		filteredBugs.add(bugs.get(b));    		
	    	}
	    }
	    return filteredBugs;
	}
	
	private static void simple(List<Bug> bugs) {
	    int n = 0;
	    double diffsum = 0.000;
	    for (int b=0; b<bugs.size(); b++) {
	    	Bug bug = bugs.get(b);
	    	if (bug.getAVs().size() > 0) {
	    		diffsum += bug.getFV() - bug.getAVs().get(0);
	    		n++;
	    		p = diffsum / n;
	    	} else {
	    		int predictedAV = (int) (bug.getFV() - p);
	    		List<Integer> avs = new ArrayList<>();
	    		avs.add(predictedAV);
	    		bug.setAVs(avs);
	    	}
	    }
	}
	
	private static void proportion(List<Bug> bugs) {
		//Proportion_increment
    	double pSum = 0;
    	int n = 0;
    	for (int b=0; b<bugs.size(); b++) {
    		Bug bug = bugs.get(b);
    		//if the bug has AVs -> update P
	    	if (bug.getAVs().size() > 0) {
	    		int fv = bug.getFV();
	    		int iv = bug.getAVs().get(0);
	    		int ov = bug.getOV();
	    		
	    		//if OV = FV no statistic possible!
	    		if (ov != fv) {
	    			pSum += 1.000*(fv-iv)/(fv-ov);
	    			n++;
	    			p = pSum / n;
	    		}
	    	} else {
	    		//otherwise -> predict IV
	    		int fv = bug.getFV();
	    		int ov = bug.getOV();
	    		int iv = (int) (fv - p*(fv-ov));
	    		List<Integer> avs = new ArrayList<>();
	    		avs.add(iv);
	    		if (ov != iv) {
	    			avs.add(ov);
	    		}
	    		bug.setAVs(avs);
	    	}
    	}
	}
	
	private static Iterable<RevCommit> getCommits(Release rel, Git git) throws
	  		IOException, NoHeadException, GitAPIException {
    	Ref tag = rel.getTag();
        LogCommand log = git.log();	        
        Ref peeledRef = git.getRepository().getRefDatabase().peel(tag);
        if(peeledRef.getPeeledObjectId() != null) {
            log.add(peeledRef.getPeeledObjectId());
        } else {
            log.add(tag.getObjectId());
        }
        return log.call();
	}

	private static List<String> getFilenames(Iterable<RevCommit> commits, Git git) throws
			MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		List<String> filenames = new ArrayList<>();
        if (commits.iterator().hasNext()) {
	        //Checkout the system at the current version (HEAD)
	        RevCommit commit = commits.iterator().next();
            ObjectId treeId = commit.getTree();
            try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
            	  treeWalk.reset(treeId);
            	  treeWalk.setRecursive(true);
            	  while (treeWalk.next()) {
            		  String path = treeWalk.getPathString();
            		  //select only java files
            		  if (path.contains(".java")) {
            	    	filenames.add(path);
            		  }
            	  }
            }
        }
        return filenames;
	}

	private static void setBuggynes (MeasurableFile jf, List<Bug> bugs, Release rel, String file ) {
    	for (Bug bug: bugs) {
    		int low = bug.getAVs().get(0);
    		if (bug.getAVs().size() > 1) {
    			int high = bug.getAVs().get(1);
    			if (rel.getIndex() >= low && rel.getIndex() <= high && bug.getFilenames().contains(file)) {
    				jf.setBuggyness(true);
    			}
    		} else {
    			if (rel.getIndex() == low && bug.getFilenames().contains(file)) {
    				jf.setBuggyness(true);
    			}
    		}
    	} 
	}
	
	private static int max(int maxChangeSet, int actualChangeSet) {
		if (maxChangeSet < actualChangeSet)
			maxChangeSet = actualChangeSet;
		return maxChangeSet;
	}
	
	public static void main(String[] args) throws Exception {
		//Change this strings to select project and algorithm to make predictions about AVs
		String projName ="BOOKKEEPER";
		String algorithm = "proportion";
		
		//Clone or Set Git repository of the Project
		File dir = new File("data/milestone1/" + projName + ".git");
		Git git = makeRepository(dir, projName);
		
		logger.info("Retrieving Releases...");
		getJiraReleases(projName);		
		logger.info("Found " + releases.size() + " releases in Jira!");
		
		//Map Jira releases with Git Tags (Selected only which have a release date)
		logger.info("Matching them in Git...");
	    List<Release> versions = makeReleases(git);
	    logger.info(versions.size() + " Releases matched! Retrieving Bugs...");
	    
	    JSONArray jsonBugs = getJiraBugs(projName);
	    logger.info(jsonBugs.length() +  " bugs found in Jira! Matching them in Git...");	    
		
	    List<Bug> bugs = jsonBugConverter(jsonBugs, versions);
	    
	    //Filtering 1: excluding those not matched in git (no filenames)
	    matchBugs(bugs, git);
	    logger.info(bugs.size() + " bugs matched in Gitbox! Filtering...");
	    
	    //Filtering Bugs 2: Discard not-post-release and without FV
	    bugs.removeAll(filter(bugs));
	    logger.info(bugs.size() + " bugs are useful!");
	    
	    //Prediction of AVs
	    if (algorithm.contentEquals("simple")) {
	    	simple(bugs);
	    } else {
	    	proportion(bugs);
	    }
	    
	    //Generate CSV dataset
		try (FileWriter fileWriter = new FileWriter("data/milestone1/" + projName + ".csv")){
			
			fileWriter.append("Version,Filename,LOC,Age,Num. Revisions," + 
					 "ChangeSet,Average Changeset,MAX ChangeSet,Number of Authors," + 
					 "LOC Added,Average LOC Added,Buggyness\n");
			fileWriter.flush();
			
			//Column 1: For each Version
		    for (Release rel: versions) {	        
		        // fetch all commits related to this version/tag
		    	Iterable<RevCommit> commits = getCommits(rel, git);
		        
		        //Column 2: Get all the Filenames of this Release
		        List<String> filenames = getFilenames(commits, git);
		        	        
		        //Measuring features of every java file
		        for (String file : filenames) {
		        	//do git log filename 
		        	Iterable<RevCommit> logs = git.log().addPath(file).call();
		        	Iterator<RevCommit> itr = logs.iterator();
	
	        		//first and last commit of a release
		        	RevCommit lastCommit = null; 
		        	RevCommit previousCommit = null;
		        	RevCommit commit;
		        	//Change Set features
		        	int maxChangeSet = 0;
		        	int changeSet = 0; 
		        	int actualChangeSet = 0;
		        	//Number of Revisions and Authors
		        	int nR = 0; 
		        	List<String> authors = new ArrayList<>();
		        	//Loc Added
		        	int locAddedSum = 0;
		        	while (itr.hasNext()) {
		        		commit = itr.next();
		        		//Updating Number of revisions
		        		nR++;
		        		//Updating Authors
		        		authors.add(commit.getAuthorIdent().getName().toString());
		        		//Updating ChangeSet Features
		        		actualChangeSet = getChangeSet(previousCommit, commit, git);
		        		changeSet += actualChangeSet ;
		        		maxChangeSet = max(maxChangeSet, actualChangeSet);
		        		//Counting LOC Added
		        		locAddedSum += countLocAdded(previousCommit, commit, git, file);
		        		
		        		//at the end this will be the first commit of the release
		        		previousCommit = commit;
		        	}
		        	
		        	//the last commit of a release is the first result (HEAD)
		        	logs = git.log().addPath(file).call();
		        	Iterator<RevCommit> itr2 = logs.iterator();
		        	lastCommit = itr2.next();
		        	if (nR != 0 && previousCommit != null) {
			        	//Reporting Results - Column 1,2
			        	MeasurableFile jf = new MeasurableFile();
			        	jf.setVersion(rel.getName());
			        	jf.setName(file);
			        	
			        	//Column 3: Lines of Code (LOC)
				        String content = readFilefromCommit(previousCommit, git, file);
				        int loc = getLines(content);
				        jf.setLoc(loc);
			        	
				        //Column 4: Age = ReleaseDate - CommitDate
			        	long days = Duration.between(previousCommit.getCommitterIdent().getWhen()
			        			.toInstant(), lastCommit.getCommitterIdent().getWhen()
			        			.toInstant()).toDays();
			        	double weeks = (days/7.000);
			        	jf.setAge(weeks);
	
			        	//Column 5: Number of Revisions
			        	jf.setNr(nR);
			        	
			        	//Columns 6,7,8: Total, Average and Max ChangeSet
			        	jf.setChangeset(changeSet);
			        	
			        	double averageChangeSet = ((double) changeSet)/nR*1.000;
			        	jf.setAverageChangeSet(averageChangeSet);
			        	jf.setMaxChangeSet(maxChangeSet);
			        	
			        	//Column 9: 
			        	Set<String> authorsFiltered = new LinkedHashSet<>(authors);
			        	jf.setnAuth(authorsFiltered.size());
			        	
			        	//Column 10, 11: LOC Added and Average
			        	jf.setLocAdded(locAddedSum);
			        	double avgLocAdded = (locAddedSum/(nR*1.000));
			        	jf.setAvgLocAdded(avgLocAdded);
			        	
			        	//Column 12: Buggyness
			        	setBuggynes(jf, bugs, rel, file);
	
			        	jf.print();	        	
			        	jf.printonFile(fileWriter);
			        	fileWriter.flush();
		        	} else {
		        		//Some files were skipped because there are part of a branch 
		        		logger.info("Skipped branch file: " + file);
		        		skipped++;
		        	}
		        }
		    }
		} finally {
	    logger.info("Skipped: " + skipped + " branch files!");
		}
	}
}