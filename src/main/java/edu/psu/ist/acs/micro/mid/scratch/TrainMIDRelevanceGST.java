package edu.psu.ist.acs.micro.mid.scratch;

import java.io.File;
import java.io.IOException;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.store.Storage;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.model.evaluation.ValidationGST;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID.TernaryRelevanceClass;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

/**
 * @deprecated
 * 
 * TrainMIDRelevanceGST was previously used for training 
 * MID relevance classification models. From now on, these models
 * should be trained using 
 * edu.psu.ist.acs.micro.mid.scratch.RunMIDContext with 
 * ctx scripts from 
 * src/main/resources/contexts/relevance/experiment. 
 *
 * @author Bill McDowell
 *
 */
public class TrainMIDRelevanceGST {
	private static String experimentName;
	private static int randomSeed;
	
	private static DatumContext<DocumentNLPDatum<Boolean>, Boolean> context;
	private static DocumentNLPDatum.Tools<Boolean> datumTools;
	private static MIDProperties properties;
	private static MIDDataTools dataTools;
	private static int datumId;
	private static Storage<?, Document> storage;
	
	public static void main(String[] args) {
		if (!parseArgs(args))
			return;
		
		List<DataSet<DocumentNLPDatum<Boolean>, Boolean>> data = (context.getBooleanValue("biasedTrainingSample")) ? constructDataBiased() : constructData();
		
		DataSet<DocumentNLPDatum<Boolean>, Boolean> trainData = data.get(0);
		DataSet<DocumentNLPDatum<Boolean>, Boolean> devData = data.get(1);
		DataSet<DocumentNLPDatum<Boolean>, Boolean> testData = data.get(2);
		
		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Setting up GST validation...");
		
		ValidationGST<DocumentNLPDatum<Boolean>, Boolean> validation = 
				new ValidationGST<DocumentNLPDatum<Boolean>, Boolean>(
						experimentName, 
						context,
						trainData,
						devData, 
						testData);
		
		if (!validation.runAndOutput())
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to run validation.");
	}
	
	@SuppressWarnings("unchecked")
	private static List<DataSet<DocumentNLPDatum<Boolean>, Boolean>> constructData() {
		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Constructing data...");
		
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> goldDocuments = new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>((StoredCollection<DocumentNLPMutable, ?>)storage.getCollection(properties.getMIDNewsGoldRelevanceLabeledDocumentCollectionName() + "_tokens"));
		
		DataSet<DocumentNLPDatum<Boolean>, Boolean> data = new DataSet<DocumentNLPDatum<Boolean>, Boolean>(datumTools, null);
		
		goldDocuments.map(new Fn<DocumentNLP, Boolean>() {
			@Override
			public Boolean apply(DocumentNLP document) {
				synchronized (this) {
					TernaryRelevanceClass relevanceClass = document.getDocumentAnnotation(AnnotationTypeNLPMID.MID_GOLD_TERNARY_RELEVANCE_CLASS);
					if (relevanceClass == TernaryRelevanceClass.CIGAR || relevanceClass == TernaryRelevanceClass.TRUE) {
						data.add(new DocumentNLPDatum<Boolean>(datumId, document, true));
						context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Loaded positive document " + document.getName() + " (" + datumId + ")... ");
						datumId++;
					} else {
						context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Skipped negative document " + document.getName() + "... ");
					}
				}
				
				return true;
			}
		}, context.getIntValue("maxThreads"), dataTools.getGlobalRandom());
		
		int positiveCount = datumId;
		
		double positiveRate = context.getDoubleValue("positiveRate");
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> unlabeledDocuments = new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(
				(StoredCollection<DocumentNLPMutable, ?>)storage.getCollection(properties.getMIDNewsSvmUnlabeledDocumentCollectionName() + "_tokens"), 
				(int)Math.floor(positiveCount / positiveRate - positiveCount));
		
		unlabeledDocuments.map(new Fn<DocumentNLP, Boolean>() {
			@Override
			public Boolean apply(DocumentNLP document) {
				// Note that this is useful because "map" function deserializes documents in parallel internally
				synchronized (this) {
					data.add(new DocumentNLPDatum<Boolean>(datumId, document, false));
					context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Loaded negative document " + document.getName() + " (" + datumId + ")... ");
					datumId++;
				}
				
				return true;
			}
			
		}, context.getIntValue("maxThreads"), dataTools.getGlobalRandom());

		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Finished loading documents");
		
		return data.makePartition(new double[] { .8,  .1, .1 }, dataTools.getGlobalRandom());
	}
	
	@SuppressWarnings("unchecked")
	private static List<DataSet<DocumentNLPDatum<Boolean>, Boolean>> constructDataBiased() {
		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Constructing data...");
		
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> goldDocuments = new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>((StoredCollection<DocumentNLPMutable, ?>)storage.getCollection(properties.getMIDNewsGoldRelevanceLabeledDocumentCollectionName() + "_tokens"));
		DataSet<DocumentNLPDatum<Boolean>, Boolean> data = new DataSet<DocumentNLPDatum<Boolean>, Boolean>(datumTools, null);
		
		goldDocuments.map(new Fn<DocumentNLP, Boolean>() {
			@Override
			public Boolean apply(DocumentNLP document) {
				
				synchronized (this) {
					TernaryRelevanceClass relevanceClass = document.getDocumentAnnotation(AnnotationTypeNLPMID.MID_GOLD_TERNARY_RELEVANCE_CLASS);
					if (relevanceClass == TernaryRelevanceClass.CIGAR || relevanceClass == TernaryRelevanceClass.TRUE) {
						data.add(new DocumentNLPDatum<Boolean>(datumId, document, true));
						context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Loaded positive document " + document.getName() + " (" + datumId + ")... ");
						datumId++;
					} else {
						context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Skipped negative document " + document.getName() + "... ");
					}
				}
				
				return true;
			}
		}, context.getIntValue("maxThreads"), dataTools.getGlobalRandom());
		
		
		List<DataSet<DocumentNLPDatum<Boolean>, Boolean>> positiveDataParts = data.makePartition(new double[] { .8,  .1, .1 }, dataTools.getGlobalRandom());
		int trainingPositiveCount = positiveDataParts.get(0).size();
		int devPositiveCount = positiveDataParts.get(1).size();
		int testPositiveCount = positiveDataParts.get(2).size();
		
		double nonTrainingPositiveRate = context.getDoubleValue("positiveRate");
		
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> unlabeledDocuments = new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(
				(StoredCollection<DocumentNLPMutable, ?>)storage.getCollection(properties.getMIDNewsSvmUnlabeledDocumentCollectionName() + "_tokens"), 
				(int)Math.floor(trainingPositiveCount + devPositiveCount / nonTrainingPositiveRate - devPositiveCount + testPositiveCount / nonTrainingPositiveRate - testPositiveCount), dataTools.getGlobalRandom(), false);		
		DataSet<DocumentNLPDatum<Boolean>, Boolean> unlabeledData = new DataSet<DocumentNLPDatum<Boolean>, Boolean>(datumTools, null);
		
		unlabeledDocuments.map(new Fn<DocumentNLP, Boolean>() {
			@Override
			public Boolean apply(DocumentNLP document) {
				// Note that this is useful because "map" function deserializes documents in parallel internally
				synchronized (this) {
					unlabeledData.add(new DocumentNLPDatum<Boolean>(datumId, document, false));
					context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Loaded negative document " + document.getName() + " (" + datumId + ")... ");
					datumId++;
				}
				
				return true;
			}
			
		}, context.getIntValue("maxThreads"), dataTools.getGlobalRandom());

		double trainProportion = trainingPositiveCount / unlabeledData.size();
		double devProportion = (devPositiveCount / nonTrainingPositiveRate - devPositiveCount)/ unlabeledData.size();
		double testProportion = 1.0 - devProportion - trainProportion;
		
		List<DataSet<DocumentNLPDatum<Boolean>, Boolean>> negativeDataParts = unlabeledData.makePartition(new double[] { trainProportion, devProportion, testProportion }, dataTools.getGlobalRandom());
		positiveDataParts.get(0).addAll(negativeDataParts.get(0));
		positiveDataParts.get(1).addAll(negativeDataParts.get(1));
		positiveDataParts.get(2).addAll(negativeDataParts.get(2));
		
		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Finished loading documents");
		
		return positiveDataParts; 
	}
	
	private static boolean parseArgs(String[] args) {
		OptionParser parser = new OptionParser();
		
		parser.accepts("experimentName").withRequiredArg()
			.describedAs("Name of the training experiment")
			.ofType(String.class);
		
		parser.accepts("randomSeed").withRequiredArg()
			.describedAs("Seed for random numbers")
			.ofType(Integer.class)
			.defaultsTo(1);
		
		parser.accepts("help").forHelp();
		
		OptionSet options = parser.parse(args);
		
		if (options.has("help")) {
			try {
				parser.printHelpOn(System.out);
			} catch (IOException e) {
				return false;
			}
			return false;
		}
		
		experimentName = options.valueOf("experimentName").toString();
		randomSeed = (Integer)options.valueOf("randomSeed");
		
		properties = new MIDProperties();
		String experimentInputPath = new File(properties.getContextInputDirPath(), "/MIDRelevanceGST/" + experimentName + ".ctx").getAbsolutePath();
		String experimentOutputPath = new File(properties.getExperimentOutputDirPath(), "/MIDRelevanceGST/" + experimentName).getAbsolutePath(); 
		
		dataTools = new MIDDataTools(new OutputWriter(
				new File(experimentOutputPath + ".debug.out"),
				new File(experimentOutputPath + ".results.out"),
				new File(experimentOutputPath + ".data.out"),
				new File(experimentOutputPath + ".model.out")
				), properties);
		
		dataTools.setRandomSeed(randomSeed);
		datumTools = DocumentNLPDatum.getBooleanTools(dataTools);
		
		context = DatumContext.run(datumTools, FileUtil.getFileReader(experimentInputPath));
		
		if (context == null) {
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to deserialize context.");
			return false;
		}
	
		datumId = 0;
		storage = properties.getStorage(dataTools, null);
		
		return true;
	}
}
