package it.uniroma2.isw2.milestone3;

import java.io.File;
import java.io.FileWriter;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.bayes.NaiveBayes;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.filters.unsupervised.attribute.StringToNominal;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Main {
	private static final Logger logger = LogManager.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		
		String dataset = "SYNCOPE";
		logger.info("Project Name: " + dataset);
		
		//Preparing the file to export
		String data = "data/milestone3/";
		try (FileWriter file = new FileWriter(data + dataset + ".csv")) {
			file.append("dataset,#TrainingRelease,%Training,%Defective in Training,%Defective in Testing,"
					+ "Classifier,FeatureSelection,Balancing,TP,FP,TN,FN,Precision,Recall,AUC,Kappa\n");
			
			File directory = new File(data + dataset + "/training/");
			int n = directory.listFiles().length/2;
			
			for (int i=1; i<=n; i++) {
				logger.info("Training on " + i + " and Testing on " + (i+1) );
				//Loading the datasets		
				DataSource source1 = new DataSource(data + dataset + "/training/" + i + ".arff");
				Instances train = source1.getDataSet();
				DataSource source2 = new DataSource(data + dataset + "/testing/" + (i+1) + ".arff"); 
				Instances test = source2.getDataSet();
				
				//Converting String attributes
		        StringToNominal stringtoNominal = new StringToNominal();
		        String[] options = new String[2];
		        options[0] = "-R";
				options[1] = "1-2";
				stringtoNominal.setOptions(options);
		        stringtoNominal.setInputFormat(test);
		        Instances training = Filter.useFilter(train, stringtoNominal); 
		        Instances testing = Filter.useFilter(test, stringtoNominal);
				
		        int numAttr = training.numAttributes();
				training.setClassIndex(numAttr-1);
				testing.setClassIndex(numAttr-1);
				
				int pctTraining = 100 * training.size() / (training.size() + testing.size());
				int pctDefectiveTrain =  100*(training.attributeStats(numAttr-1).nominalCounts[0])/training.size();
				int pctDefectiveTest =  100*(testing.attributeStats(numAttr-1).nominalCounts[0])/testing.size();
				int majorityClass = Math.max(training.attributeStats(numAttr-1).nominalCounts[0], training.attributeStats(numAttr-1).nominalCounts[1]);
				int majorityClasspct = 100*majorityClass/training.size();
				//1. NaiveBayes
				NaiveBayes classifier = new NaiveBayes();
				classifier.buildClassifier(training);
				Evaluation eval = new Evaluation(testing);	
				eval.evaluateModel(classifier, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",NaiveBayes,No FS,No Sampling," + 
					eval.numTruePositives(0) + "," + eval.numFalsePositives(0) + "," + eval.numTrueNegatives(0) 
					+ "," + eval.numFalseNegatives(0) + "," + eval.areaUnderROC(0) 
					+ "," + eval.kappa() + "," + eval.precision(0) + "," + eval.recall(0) + "\n");

				
				//2. RandomForest
				RandomForest classifier2 = new RandomForest();
				classifier2.buildClassifier(training);
				Evaluation eval2 = new Evaluation(testing);
				eval2.evaluateModel(classifier2, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
				    pctDefectiveTest + ",RandomForest,No FS,No Sampling," + 
					eval2.numTruePositives(0) + "," + eval2.numFalsePositives(0) + "," + eval2.numTrueNegatives(0) 
					+ "," + eval2.numFalseNegatives(0) + "," + eval2.areaUnderROC(0) 
				    + "," + eval2.kappa() + "," + eval2.precision(0) + "," 
					+ eval2.recall(0) + "\n");

				//3. IBK
				IBk classifier3 = new IBk();
				classifier3.buildClassifier(training);
				Evaluation eval3 = new Evaluation(testing);
				eval3.evaluateModel(classifier3, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",IBK,No FS,No Sampling," + 
					eval3.numTruePositives(0) + "," + eval3.numFalsePositives(0) + "," + eval3.numTrueNegatives(0) 
					+ "," + eval3.numFalseNegatives(0) + "," + eval3.areaUnderROC(0) 
			        + "," + eval3.kappa() + "," + eval3.precision(0) + "," 
				    + eval3.recall(0) + "\n");
				
				//UnderSampling
				FilteredClassifier fc1 = new FilteredClassifier();
				FilteredClassifier fc2 = new FilteredClassifier();
				FilteredClassifier fc3 = new FilteredClassifier();
				
				fc1.setClassifier(new NaiveBayes());
				fc2.setClassifier(new RandomForest());
				fc3.setClassifier(new IBk());
				
				SpreadSubsample  spreadSubsample = new SpreadSubsample();
				String[] opts = new String[]{ "-M", "1.0"};
				spreadSubsample.setOptions(opts);
				fc1.setFilter(spreadSubsample);
				fc2.setFilter(spreadSubsample);
				fc3.setFilter(spreadSubsample);
				
				fc1.buildClassifier(training);
				Evaluation evalUndersampled = new Evaluation(testing);	
				evalUndersampled.evaluateModel(fc1, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",NaiveBayes,No FS,UnderSampling," + 
					evalUndersampled.numTruePositives(0) + "," + evalUndersampled.numFalsePositives(0) + ","
					+ evalUndersampled.numTrueNegatives(0) + "," + evalUndersampled.numFalseNegatives(0) + "," 
					+ evalUndersampled.areaUnderROC(0) 
					+ "," + evalUndersampled.kappa() + "," + evalUndersampled.precision(0) + "," 
					+ evalUndersampled.recall(0) + "\n");
				
				fc2.buildClassifier(training);
				Evaluation evalUndersampled2 = new Evaluation(testing);	
				evalUndersampled2.evaluateModel(fc2, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",RandomForest,No FS,UnderSampling," + 
					evalUndersampled2.numTruePositives(0) + "," + evalUndersampled2.numFalsePositives(0) + ","
					+ evalUndersampled2.numTrueNegatives(0) + "," + evalUndersampled2.numFalseNegatives(0) + ","
					+ evalUndersampled2.areaUnderROC(0) 
					+ "," + evalUndersampled2.kappa() + "," + evalUndersampled2.precision(0) + "," 
					+ evalUndersampled2.recall(0) + "\n");
				
				fc3.buildClassifier(training);
				Evaluation evalUndersampled3 = new Evaluation(testing);	
				evalUndersampled3.evaluateModel(fc3, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",IBK,No FS,UnderSampling," + 
					evalUndersampled3.numTruePositives(0) + "," + evalUndersampled3.numFalsePositives(0) + ","
					+ evalUndersampled3.numTrueNegatives(0) + "," + evalUndersampled3.numFalseNegatives(0) + ","
					+ evalUndersampled3.areaUnderROC(0) 
					+ "," + evalUndersampled3.kappa() + "," + evalUndersampled3.precision(0) + "," 
					+ evalUndersampled3.recall(0) + "\n");
				
				//OverSampling 
				Resample resample = new Resample();
				String[] opts2 = new String[]{ "-B", "1.0", "-Z", 2*majorityClasspct + ".0"};
				resample.setOptions(opts2);
				resample.setInputFormat(training);
				FilteredClassifier fc1o = new FilteredClassifier();
				FilteredClassifier fc2o = new FilteredClassifier();
				FilteredClassifier fc3o = new FilteredClassifier();
				
				fc1o.setClassifier(new NaiveBayes());
				fc2o.setClassifier(new RandomForest());
				fc3o.setClassifier(new IBk());
				
				fc1o.setFilter(resample);
				fc2o.setFilter(resample);
				fc3o.setFilter(resample);
				
				fc1o.buildClassifier(training);
				Evaluation evalOversampled = new Evaluation(testing);	
				evalOversampled.evaluateModel(fc1o, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",NaiveBayes,No FS,OverSampling," + 
					evalOversampled.numTruePositives(0) + "," + evalOversampled.numFalsePositives(0) + ","
					+ evalOversampled.numTrueNegatives(0) + "," + evalOversampled.numFalseNegatives(0) + ","
					+ evalOversampled.areaUnderROC(0) 
					+ "," + evalOversampled.kappa() + "," + evalOversampled.precision(0) + "," 
					+ evalOversampled.recall(0) + "\n");
				
				fc2o.buildClassifier(training);
				Evaluation evalOversampled2 = new Evaluation(testing);	
				evalOversampled2.evaluateModel(fc2o, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",RandomForest,No FS,OverSampling," + 
					evalOversampled2.numTruePositives(0) + "," + evalOversampled2.numFalsePositives(0) + ","
					+ evalOversampled2.numTrueNegatives(0) + "," + evalOversampled2.numFalseNegatives(0) + ","
					+ evalOversampled2.areaUnderROC(0) 
					+ "," + evalOversampled2.kappa() + "," + evalOversampled2.precision(0) + "," 
					+ evalOversampled2.recall(0) + "\n");

				fc3o.buildClassifier(training);
				Evaluation evalOversampled3 = new Evaluation(testing);	
				evalOversampled3.evaluateModel(fc3o, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",IBK,No FS,OverSampling," + 
					evalOversampled3.numTruePositives(0) + "," + evalOversampled3.numFalsePositives(0) + ","
					+ evalOversampled3.numTrueNegatives(0) + "," + evalOversampled3.numFalseNegatives(0) + ","
					+ evalOversampled3.areaUnderROC(0) 
			        + "," + evalOversampled3.kappa() + "," + evalOversampled3.precision(0) + "," 
			        + evalOversampled3.recall(0) + "\n");
				
				//SMOTE
				SMOTE smote = new SMOTE();
				smote.setInputFormat(training);
				
				FilteredClassifier fc1s = new FilteredClassifier();
				FilteredClassifier fc2s = new FilteredClassifier();
				FilteredClassifier fc3s = new FilteredClassifier();
				
				fc1s.setClassifier(new NaiveBayes());
				fc2s.setClassifier(new RandomForest());
				fc3s.setClassifier(new IBk());
				
				fc1s.setFilter(smote);
				fc2s.setFilter(smote);
				fc3s.setFilter(smote);
								
				fc1s.buildClassifier(training);
				Evaluation evalSmote = new Evaluation(testing);	
				evalSmote.evaluateModel(fc1s, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",NaiveBayes,No FS,Smote," + 
					evalSmote.numTruePositives(0) + "," + evalSmote.numFalsePositives(0) + ","
					+ evalSmote.numTrueNegatives(0) + "," + evalSmote.numFalseNegatives(0) + "," 
					+ evalSmote.areaUnderROC(0) 
					+ "," + evalSmote.kappa() + "," + evalSmote.precision(0) + "," 
					+ evalSmote.recall(0) + "\n");
				
				fc2s.buildClassifier(training);
				Evaluation evalSmote2 = new Evaluation(testing);	
				evalSmote2.evaluateModel(fc2s, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",RandomForest,No FS,Smote," + 
					evalSmote2.numTruePositives(0) + "," + evalSmote2.numFalsePositives(0) + ","
					+ evalSmote2.numTrueNegatives(0) + "," + evalSmote2.numFalseNegatives(0) + "," 
					+ evalSmote2.areaUnderROC(0) 
					+ "," + evalSmote2.kappa() + "," + evalSmote2.precision(0) + "," 
					+ evalSmote2.recall(0) + "\n");
				
				fc3s.buildClassifier(training);
				Evaluation evalSmote3 = new Evaluation(testing);	
				evalSmote3.evaluateModel(fc3s, testing); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",IBK,No FS,Smote," + 
					evalSmote3.numTruePositives(0) + "," + evalSmote3.numFalsePositives(0) + ","
					+ evalSmote3.numTrueNegatives(0) + "," + evalSmote3.numFalseNegatives(0) + "," 
					+ evalSmote3.areaUnderROC(0) 
					+ "," + evalSmote3.kappa() + "," + evalSmote3.precision(0) + "," 
					+ evalSmote3.recall(0) + "\n");
				
				//Feature Selection
				AttributeSelection filter = new AttributeSelection();
				CfsSubsetEval evalFs = new CfsSubsetEval();
				BestFirst search = new BestFirst();
				filter.setEvaluator(evalFs);
				filter.setSearch(search);
				filter.setInputFormat(training);
				Instances filteredTraining = Filter.useFilter(training, filter);
				
				int numAttrFiltered = filteredTraining.numAttributes();
				
				filteredTraining.setClassIndex(numAttrFiltered - 1);
				Instances filteredTesting = Filter.useFilter(testing, filter);
				filteredTesting.setClassIndex(numAttrFiltered - 1);
				
				//NaiveBayes
				classifier.buildClassifier(filteredTraining);
			    eval.evaluateModel(classifier, filteredTesting);
			    file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",NaiveBayes,BF,No Sampling," + 
					eval.numTruePositives(0) + "," + eval.numFalsePositives(0) + ","
					+ eval.numTrueNegatives(0) + "," + eval.numFalseNegatives(0) + "," 
					+ eval.areaUnderROC(0) 
					+ "," + eval.kappa() + "," + eval.precision(0) + "," 
					+ eval.recall(0) + "\n");
				//RandomForest
				classifier2.buildClassifier(filteredTraining);
			    eval2.evaluateModel(classifier, filteredTesting);
			    file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",RandomForest,BF,No Sampling," + 
					eval2.numTruePositives(0) + "," + eval2.numFalsePositives(0) + ","
					+ eval2.numTrueNegatives(0) + "," + eval2.numFalseNegatives(0) + "," 
					+ eval2.areaUnderROC(0) 
					+ "," + eval2.kappa() + "," + eval2.precision(0) + "," 
					+ eval2.recall(0) + "\n");
				//IBK
				classifier3.buildClassifier(filteredTraining);
			    eval3.evaluateModel(classifier, filteredTesting);
			    file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",IBK,BF,No Sampling," + 
					eval3.numTruePositives(0) + "," + eval3.numFalsePositives(0) + ","
					+ eval3.numTrueNegatives(0) + "," + eval3.numFalseNegatives(0) + "," 
					+ eval3.areaUnderROC(0) 
					+ "," + eval3.kappa() + "," + eval3.precision(0) + "," 
					+ eval3.recall(0) + "\n");
			    
				//UnderSampling
				fc1 = new FilteredClassifier();
				fc2 = new FilteredClassifier();
				fc3 = new FilteredClassifier();
				
				fc1.setClassifier(new NaiveBayes());
				fc2.setClassifier(new RandomForest());
				fc3.setClassifier(new IBk());
				
				spreadSubsample = new SpreadSubsample();
				opts = new String[]{ "-M", "1.0"};
				spreadSubsample.setOptions(opts);
				fc1.setFilter(spreadSubsample);
				fc2.setFilter(spreadSubsample);
				fc3.setFilter(spreadSubsample);
				
				fc1.buildClassifier(filteredTraining);
				evalUndersampled = new Evaluation(filteredTesting);	
				evalUndersampled.evaluateModel(fc1, filteredTesting); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",NaiveBayes,BF,UnderSampling," + 
					evalUndersampled.numTruePositives(0) + "," + evalUndersampled.numFalsePositives(0) + ","
					+ evalUndersampled.numTrueNegatives(0) + "," + evalUndersampled.numFalseNegatives(0) 
					+ "," + evalUndersampled.areaUnderROC(0) 
					+ "," + evalUndersampled.kappa() + "," + evalUndersampled.precision(0) + "," 
					+ evalUndersampled.recall(0) + "\n");
				
				fc2.buildClassifier(filteredTraining);
				evalUndersampled2 = new Evaluation(filteredTesting);	
				evalUndersampled2.evaluateModel(fc2, filteredTesting); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",RandomForest,BF,UnderSampling," + 
					evalUndersampled2.numTruePositives(0) + "," + evalUndersampled2.numFalsePositives(0) + ","
					+ evalUndersampled2.numTrueNegatives(0) + "," + evalUndersampled2.numFalseNegatives(0)
					+ "," + evalUndersampled2.areaUnderROC(0) 
					+ "," + evalUndersampled2.kappa() + "," + evalUndersampled2.precision(0) + "," 
					+ evalUndersampled2.recall(0) + "\n");
				
				fc3.buildClassifier(filteredTraining);
				evalUndersampled3 = new Evaluation(filteredTesting);	
				evalUndersampled3.evaluateModel(fc3, filteredTesting); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",IBK,BF,UnderSampling," + 
					evalUndersampled3.numTruePositives(0) + "," + evalUndersampled3.numFalsePositives(0) + ","
					+ evalUndersampled3.numTrueNegatives(0) + "," + evalUndersampled3.numFalseNegatives(0) 
					+ "," + evalUndersampled3.areaUnderROC(0) 
					+ "," + evalUndersampled3.kappa() + "," + evalUndersampled3.precision(0) + "," 
					+ evalUndersampled3.recall(0) + "\n");
				
				//OverSampling
				resample = new Resample();
				resample.setOptions(opts2);
				resample.setInputFormat(filteredTraining);
				fc1o = new FilteredClassifier();
				fc2o = new FilteredClassifier();
				fc3o = new FilteredClassifier();
				
				fc1o.setClassifier(new NaiveBayes());
				fc2o.setClassifier(new RandomForest());
				fc3o.setClassifier(new IBk());
				
				fc1o.setFilter(resample);
				fc2o.setFilter(resample);
				fc3o.setFilter(resample);
				
				fc1o.buildClassifier(filteredTraining);
				evalOversampled = new Evaluation(filteredTesting);	
				evalOversampled.evaluateModel(fc1o, filteredTesting); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",NaiveBayes,BF,OverSampling," + 
					evalOversampled.numTruePositives(0) + "," + evalOversampled.numFalsePositives(0) + ","
					+ evalOversampled.numTrueNegatives(0) + "," + evalOversampled.numFalseNegatives(0) + ","
					+ evalOversampled.areaUnderROC(0) 
					+ "," + evalOversampled.kappa() + "," + evalOversampled.precision(0) + "," 
					+ evalOversampled.recall(0) + "\n");
				
				fc2o.buildClassifier(filteredTraining);
				evalOversampled2 = new Evaluation(filteredTesting);	
				evalOversampled2.evaluateModel(fc2o, filteredTesting); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",RandomForest,BF,OverSampling," + 
					evalOversampled2.numTruePositives(0) + "," + evalOversampled2.numFalsePositives(0) + ","
					+ evalOversampled2.numTrueNegatives(0) + "," + evalOversampled2.numFalseNegatives(0) 
					+ "," + evalOversampled2.areaUnderROC(0) 
					+ "," + evalOversampled2.kappa() + "," + evalOversampled2.precision(0) + "," 
					+ evalOversampled2.recall(0) + "\n");

				fc3o.buildClassifier(filteredTraining);
				evalOversampled3 = new Evaluation(filteredTesting);	
				evalOversampled3.evaluateModel(fc3o, filteredTesting); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",IBK,BF,OverSampling," + 
					evalOversampled3.numTruePositives(0) + "," + evalOversampled3.numFalsePositives(0) + ","
					+ evalOversampled3.numTrueNegatives(0) + "," + evalOversampled3.numFalseNegatives(0) 
					+ "," + evalOversampled3.areaUnderROC(0) 
					+ "," + evalOversampled3.kappa() + "," + evalOversampled3.precision(0) + "," 
					+ evalOversampled3.recall(0) + "\n");
				
				//SMOTE
				smote = new SMOTE();
				smote.setInputFormat(filteredTraining);
				
				fc1s = new FilteredClassifier();
				fc2s = new FilteredClassifier();
				fc3s = new FilteredClassifier();
				
				fc1s.setClassifier(new NaiveBayes());
				fc2s.setClassifier(new RandomForest());
				fc3s.setClassifier(new IBk());
				
				fc1s.setFilter(smote);
				fc2s.setFilter(smote);
				fc3s.setFilter(smote);
								
				fc1s.buildClassifier(filteredTraining);
				evalSmote = new Evaluation(filteredTesting);	
				evalSmote.evaluateModel(fc1s, filteredTesting); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",NaiveBayes,BF,Smote," + 
					evalSmote.numTruePositives(0) + "," + evalSmote.numFalsePositives(0) + ","
					+ evalSmote.numTrueNegatives(0) + "," + evalSmote.numFalseNegatives(0) + "," 
					+ evalSmote.areaUnderROC(0) 
					+ "," + evalSmote.kappa() + "," + evalSmote.precision(0) + "," 
					+ evalSmote.recall(0) + "\n");
				
				fc2s.buildClassifier(filteredTraining);
				evalSmote2 = new Evaluation(filteredTesting);	
				evalSmote2.evaluateModel(fc2s, filteredTesting); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",RandomForest,BF,Smote," + 
					evalSmote2.numTruePositives(0) + "," + evalSmote2.numFalsePositives(0) + ","
					+ evalSmote2.numTrueNegatives(0) + "," + evalSmote2.numFalseNegatives(0) + ","
					+ evalSmote2.areaUnderROC(0) 
					+ "," + evalSmote2.kappa() + "," + evalSmote2.precision(0) + "," 
					+ evalSmote2.recall(0) + "\n");
				
				fc3s.buildClassifier(filteredTraining);
				evalSmote3 = new Evaluation(filteredTesting);	
				evalSmote3.evaluateModel(fc3s, filteredTesting); 
				file.append(dataset + "," + i + "," + pctTraining + "," + pctDefectiveTrain + "," + 
					pctDefectiveTest + ",IBK,BF,Smote," + 
					evalSmote3.numTruePositives(0) + "," + evalSmote3.numFalsePositives(0) + ","
					+ evalSmote3.numTrueNegatives(0) + "," + evalSmote3.numFalseNegatives(0) + "," 
					+ evalSmote3.areaUnderROC(0) 
					+ "," + evalSmote3.kappa() + "," + evalSmote3.precision(0) + "," 
					+ evalSmote3.recall(0) + "\n");
			    
			}
		} finally {
			logger.info("Done! Your file is ready!");
		}
	}

}
