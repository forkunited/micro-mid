package edu.psu.ist.acs.micro.mid.scratch;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.store.Storage;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.model.evaluation.ValidationGSTBinary;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.WeightedStringList;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDDispute;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

public class TrainMIDGSTBinary {
	public enum PredictionType {
		ACTION,
		HOSTILITY_LEVEL
	}
	
	private static String experimentName;
	private static int randomSeed;
	private static PredictionType predictionType;
	
	private static DatumContext<DocumentNLPDatum<WeightedStringList>, WeightedStringList> context;
	private static DocumentNLPDatum.Tools<WeightedStringList> datumTools;
	private static MIDProperties properties;
	private static MIDDataTools dataTools;
	private static int datumId;
	private static Storage<?, Document> storage;
	
	public static void main(String[] args) {
		if (!parseArgs(args))
			return;
		
		List<DataSet<DocumentNLPDatum<WeightedStringList>, WeightedStringList>> data = constructData();
		
		DataSet<DocumentNLPDatum<WeightedStringList>, WeightedStringList> trainData = data.get(0);
		DataSet<DocumentNLPDatum<WeightedStringList>, WeightedStringList> devData = data.get(1);
		DataSet<DocumentNLPDatum<WeightedStringList>, WeightedStringList> testData = data.get(2);
		
		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Setting up binary GST validation...");
		
		WeightedStringList labels = new WeightedStringList(context.getStringArray("validLabels").toArray(new String[0]), null, 0);
		for (String label : labels.getStrings()) {
			LabelIndicator<WeightedStringList> labelIndicator = new LabelIndicator<WeightedStringList>() {
				public String toString() {
					return label;
				}
				
				@Override
				public boolean indicator(WeightedStringList labelList) {
					return labelList.contains(label);
				}
				
				@Override
				public double weight(WeightedStringList labelList) {
					return labelList.getStringWeight(label);
				}
			};
			
			datumTools.addLabelIndicator(labelIndicator);
		}
		
		ValidationGSTBinary<DocumentNLPDatum<Boolean>,DocumentNLPDatum<WeightedStringList>,WeightedStringList> validation = 
				new ValidationGSTBinary<DocumentNLPDatum<Boolean>, DocumentNLPDatum<WeightedStringList>, WeightedStringList>(
						experimentName, 
						context,
						trainData,
						devData, 
						testData,
						datumTools.getInverseLabelIndicator("Weighted"));
		
		if (!validation.runAndOutput())
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to run validation.");
	}
	
	@SuppressWarnings("unchecked")
	private static List<DataSet<DocumentNLPDatum<WeightedStringList>, WeightedStringList>> constructData() {
		context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Constructing data...");
		
		DocumentSet<DocumentNLP, DocumentNLPMutable> documents = new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>((StoredCollection<DocumentNLPMutable, ?>)storage.getCollection(properties.getMID4NarrativeDocumentCollectionName()));
		StoredCollection<MIDDispute, ?> disputes = (StoredCollection<MIDDispute, ?>)storage.getCollection(properties.getMID4CollectionName());
		DataSet<DocumentNLPDatum<WeightedStringList>, WeightedStringList> data = new DataSet<DocumentNLPDatum<WeightedStringList>, WeightedStringList>(datumTools, null);
		Set<String> documentNames = documents.getDocumentNames();
		for (String documentName : documentNames) {
			DocumentNLP document = documents.getDocumentByName(documentName);
			MIDDispute dispute = disputes.getFirstItemByIndex("dispNum3", document.getDocumentAnnotation(AnnotationTypeNLPMID.MID_DISPUTE_NUMBER_3));
			Set<String> labelSet = new HashSet<String>();
			
			if (predictionType == PredictionType.ACTION) {
				labelSet.add(dispute.getMaxAction().toString());
				for (MIDIncident incident : dispute.getIncidents())
					labelSet.add(incident.getAction().toString());
			} else {
				labelSet.add(dispute.getMaxHostilityLevel().toString());
				for (MIDIncident incident : dispute.getIncidents())
					labelSet.add(incident.getHostilityLevel().toString());
			}
			
			WeightedStringList label = new WeightedStringList(labelSet);
			
			data.add(new DocumentNLPDatum<WeightedStringList>(datumId, document, label));
			datumId++;
		}
		
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
		
		parser.accepts("predictionType").withRequiredArg()
			.ofType(String.class)
			.describedAs("Type of predictions to make (either HOSTILITY_LEVEL or ACTION)")
			.defaultsTo("ACTION");
		
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
		String experimentInputPath = new File(properties.getContextInputDirPath(), "/MIDGSTBinary/" + experimentName + ".ctx").getAbsolutePath();
		String experimentOutputPath = new File(properties.getExperimentOutputDirPath(), "/MIDGSTBinary/" + experimentName).getAbsolutePath(); 
		
		dataTools = new MIDDataTools(new OutputWriter(
				new File(experimentOutputPath + ".debug.out"),
				new File(experimentOutputPath + ".results.out"),
				new File(experimentOutputPath + ".data.out"),
				new File(experimentOutputPath + ".model.out")
				), properties);
		
		dataTools.setRandomSeed(randomSeed);
		datumTools = DocumentNLPDatum.getWeightedStringListTools(dataTools);
		
		context = DatumContext.run(datumTools, FileUtil.getFileReader(experimentInputPath));
		
		if (context == null) {
			dataTools.getOutputWriter().debugWriteln("ERROR: Failed to deserialize context.");
			return false;
		}
	
		datumId = 0;
		
		predictionType = PredictionType.valueOf(options.valueOf("predictionType").toString());
		storage = properties.getStorage(dataTools, null);
		
		return true;
	}
}

