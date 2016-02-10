package edu.psu.ist.acs.micro.mid.scratch;

import java.io.File;
import java.io.IOException;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
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

public class TrainMIDRelevanceGST {
	private static final double POSITIVE_RATE = .5;
	
	private static String experimentName;
	private static int randomSeed;
	
	private static Context<DocumentNLPDatum<Boolean>, Boolean> context;
	private static DocumentNLPDatum.Tools<Boolean> datumTools;
	private static MIDProperties properties;
	private static MIDDataTools dataTools;
	private static int datumId;
	private static Storage<?, Document> storage;
	
	public static void main(String[] args) {
		if (!parseArgs(args))
			return;
		
		List<DataSet<DocumentNLPDatum<Boolean>, Boolean>> data = constructData();
		
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
		
		DocumentSet<DocumentNLP, DocumentNLPMutable> goldDocuments = new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>((StoredCollection<DocumentNLPMutable, ?>)storage.getCollection(properties.getMIDNewsGoldRelevanceLabeledDocumentCollectionName() + "_tokens"));
		
		DataSet<DocumentNLPDatum<Boolean>, Boolean> data = new DataSet<DocumentNLPDatum<Boolean>, Boolean>(datumTools, null);
		for (DocumentNLP document : goldDocuments) {
			TernaryRelevanceClass relevanceClass = document.getDocumentAnnotation(AnnotationTypeNLPMID.MID_GOLD_TERNARY_RELEVANCE_CLASS);
			if (relevanceClass == TernaryRelevanceClass.CIGAR || relevanceClass == TernaryRelevanceClass.TRUE) {
				data.add(new DocumentNLPDatum<Boolean>(datumId, document, true));
				context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Loaded positive document " + document.getName() + " (" + datumId + ")... ");
				datumId++;
			} else {
				context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Skipped negative document " + document.getName() + "... ");
			}
		}
		
		int positiveCount = datumId;
		
		DocumentSet<DocumentNLP, DocumentNLPMutable> unlabeledDocuments = new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(
				(StoredCollection<DocumentNLPMutable, ?>)storage.getCollection(properties.getMIDNewsSvmUnlabeledDocumentCollectionName() + "_tokens"), 
				(int)Math.floor(positiveCount / POSITIVE_RATE - positiveCount));
		
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
			
		}, context.getIntValue("maxThreads"));

		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Finished loading documents");
		
		return data.makePartition(new double[] { .8,  .1, .1 }, dataTools.getGlobalRandom());
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
		
		context = Context.deserialize(datumTools, FileUtil.getFileReader(experimentInputPath));
		
		if (context == null) {
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to deserialize context.");
			return false;
		}
	
		datumId = 0;
		storage = properties.getStorage(dataTools, null);
		
		return true;
	}
}
