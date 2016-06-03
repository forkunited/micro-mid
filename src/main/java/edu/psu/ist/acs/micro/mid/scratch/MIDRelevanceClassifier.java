package edu.psu.ist.acs.micro.mid.scratch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum.Tools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.data.feature.FeatureSet;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.data.store.StoredCollectionFileSystem;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.psu.ist.acs.micro.mid.data.MID5FileReader;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

public class MIDRelevanceClassifier {
	private static final int CLASSIFICATION_BATCH_SIZE = 100;

	private static File input;
	private static int maxThreads;
	private static File modelFile;
	private static File outputDocumentDir;
	private static File outputClassificationFile;
	
	private static DataTools dataTools;
	private static Tools<Boolean> datumTools;
	
	private static FeatureSet<DocumentNLPDatum<Boolean>, Boolean> features;
	private static SupervisedModel<DocumentNLPDatum<Boolean>, Boolean> model;
	private static BufferedWriter outputClassWriter;
	private static StoredCollection<DocumentNLPMutable, Document> outputDocuments;
	
	public static void main(String[] args) {
		if (!parseArgs(args)) {
			System.out.println("ERROR: Failed to parse arguments.");
			return;
		}
		
		System.out.println("Deserializing MID relevance features and model...");
		if (!deserialize()) {
			System.out.println("ERROR: Couldn't deserialize model from " + modelFile.getAbsolutePath());
			System.exit(0);
		}
		
		if (outputDocumentDir != null) {
			List<AnnotationType<?>> annotationTypes = new ArrayList<AnnotationType<?>>();
			annotationTypes.addAll(dataTools.getAnnotationTypesNLP());
			annotationTypes.remove(AnnotationTypeNLP.SENTENCE);
			
			outputDocuments = new StoredCollectionFileSystem<DocumentNLPMutable, Document>(outputDocumentDir.getName(), 
					outputDocumentDir, 
					new SerializerDocumentNLPBSON(new DocumentNLPInMemory(dataTools), annotationTypes));
		}
		
		if (outputClassificationFile != null) {
			outputClassWriter = FileUtil.getFileWriter(outputClassificationFile.getAbsolutePath());
		}
	
		PipelineNLPStanford pipelineStanford = new PipelineNLPStanford();
		pipelineStanford.initialize(AnnotationTypeNLP.CONSTITUENCY_PARSE);
		
		List<MID5FileReader> readers = getInputReaders();
		ThreadMapper<MID5FileReader, Boolean> threadMapper = new ThreadMapper<MID5FileReader, Boolean>(
			new ThreadMapper.Fn<MID5FileReader, Boolean>() {
				@Override
				public Boolean apply(MID5FileReader reader) {
					int datumId = 0;
					Pair<String, Map<AnnotationTypeNLP<?>, Object>> nameAndAnnotations = null;
					DataSet<DocumentNLPDatum<Boolean>, Boolean> featurizedData = 
							new DataSet<DocumentNLPDatum<Boolean>, Boolean>(datumTools, null);

					try {
						while ((nameAndAnnotations = reader.readOne()) != null) {
							synchronized (args) {
								System.out.println("Processing document " + nameAndAnnotations.getFirst() + "...");
							}
							
							DocumentNLPMutable document = new DocumentNLPInMemory(dataTools,
																				  nameAndAnnotations.getFirst(), 
																				  nameAndAnnotations.getSecond().get(AnnotationTypeNLP.ORIGINAL_TEXT).toString());
						
							for (Entry<AnnotationTypeNLP<?>, Object> entry : nameAndAnnotations.getSecond().entrySet()) {
								if (!entry.getKey().equals(AnnotationTypeNLP.ORIGINAL_TEXT))
									document.setDocumentAnnotation("MID5-News", entry.getKey(), new Pair<Object, Double>(entry.getValue(), null));
							}
							
							document = pipelineStanford.run(document);
							
							featurizedData.add(new DocumentNLPDatum<Boolean>(datumId, document, null));
							datumId++;
							
							if (datumId % CLASSIFICATION_BATCH_SIZE == 0) {
								if (!classifyAndOutput(new DataFeatureMatrix<>(features.getContext(), "", featurizedData, features))) {
									System.out.println("ERROR: Failed to classify and/or output.");
									System.exit(0);
								}
								
								featurizedData = new DataSet<DocumentNLPDatum<Boolean>, Boolean>(datumTools, null);
							}
							
						}
						
						if (featurizedData.size() > 0) {
							if (!classifyAndOutput(new DataFeatureMatrix<>(features.getContext(), "", featurizedData, features))) {
								System.out.println("Error in classification or output.");
								System.exit(0);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
						reader.close();
						System.exit(0);
					}
					
					reader.close();
					return true;
				}		
			}
		);
		
		threadMapper.run(readers, maxThreads);
	
		if (outputClassWriter != null) {
			try {
				outputClassWriter.close();
			} catch (IOException e) {
				
			}
		}
	}
	
	private static List<MID5FileReader> getInputReaders() {
		List<MID5FileReader> readers = new ArrayList<MID5FileReader>();
		
		if (input.isDirectory()) {
			File[] files = input.listFiles();
			for (File file : files)
				readers.add(new MID5FileReader(file));
		} else {
			readers.add(new MID5FileReader(input));
		}
		
		return readers;
	}
	
	private static boolean deserialize() {
		BufferedReader ctxReader = FileUtil.getFileReader(modelFile.getAbsolutePath());
		DatumContext<DocumentNLPDatum<Boolean>, Boolean> context = DatumContext.run(datumTools, ctxReader);
		try {
			ctxReader.close();
		} catch (IOException e) {
			return false;
		}
		
		features = context.getFeatureSets().get(0);
		model = context.getModels().get(0);
		
		return true;
	}
	
	private static boolean classifyAndOutput(DataFeatureMatrix<DocumentNLPDatum<Boolean>, Boolean> data) {
		if (!data.precompute())
			return false;
		
		Map<DocumentNLPDatum<Boolean>, Boolean> classifications = model.classify(data);
		Map<DocumentNLPDatum<Boolean>, Map<Boolean, Double>> posterior = model.posterior(data);
		return output(classifications, posterior);
	}
	
	
	private static boolean output(Map<DocumentNLPDatum<Boolean>, Boolean> classifications, Map<DocumentNLPDatum<Boolean>, Map<Boolean, Double>> p) {
		try {
			for (Entry<DocumentNLPDatum<Boolean>, Boolean> entry : classifications.entrySet()) {
				if (outputDocuments != null) {
					DocumentNLPMutable document = (DocumentNLPMutable)entry.getKey().getDocument();
					document.setDocumentAnnotation(modelFile.getName(), 
							AnnotationTypeNLPMID.MID_CLASSIFIER_RELEVANCE_CLASS, 
							new Pair<Object, Double>(entry.getValue(), null));

					document.setDocumentAnnotation(modelFile.getName(), 
							AnnotationTypeNLPMID.MID_CLASSIFIER_RELEVANCE_SCORE, 
							new Pair<Object, Double>(p.get(entry.getKey()).get(true), null));
					
					outputDocuments.addItem(document);
				}
				
				if (outputClassWriter != null) {
					synchronized(outputClassWriter) {
						String classification = ((entry.getValue()) ? "1" : "0");
						Double docP = p.get(entry.getKey()).get(true);
						outputClassWriter.write(entry.getKey().getDocument().getName() + "\t" + classification + "\t" + docP + "\n");
					}
				}
			}
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}
	
	private static boolean parseArgs(String[] args) {
		OutputWriter output = new OutputWriter();
		OptionParser parser = new OptionParser();
		
		parser.accepts("input").withRequiredArg()
			.describedAs("Path to input file or directory")
			.ofType(File.class);
		parser.accepts("maxThreads").withRequiredArg()
			.describedAs("Maximum number of concurrent threads to use when annotating files")
			.ofType(Integer.class)
			.defaultsTo(1);
		parser.accepts("modelFile").withRequiredArg()
			.describedAs("Path to serialized model/features file")
			.ofType(File.class);
		parser.accepts("outputDocumentDir").withRequiredArg()
			.describedAs("Path to directory for output documents")
			.ofType(File.class);
		parser.accepts("outputClassificationFile").withRequiredArg()
			.describedAs("Path to output classification file")
			.ofType(File.class);
		
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
		
		output.debugWriteln("Loading data tools (gazetteers etc)...");
		dataTools = new MIDDataTools(output, (MIDProperties)null);
		datumTools = DocumentNLPDatum.getBooleanTools(dataTools);
		output.debugWriteln("Finished loading data tools.");
		
		if (options.has("input")) {
			input = (File)options.valueOf("input");
		} else {
			dataTools.getOutputWriter().debugWriteln("ERROR: Missing 'input' argument.");
			return false;
		}
		
		maxThreads = (int)options.valueOf("maxThreads");
		
		if (options.has("modelFile")) {
			modelFile = (File)options.valueOf("modelFile");
		} else {
			dataTools.getOutputWriter().debugWriteln("ERROR: Missing 'modelFile' argument.");
			return false;
		}
		
		if (options.has("outputClassificationFile")) {
			outputClassificationFile = (File)options.valueOf("outputClassificationFile");
		} else {
			dataTools.getOutputWriter().debugWriteln("ERROR: Missing 'outputClassificationFile' argument.");
			return false;
		}
	
		
		if (options.has("outputDocumentDir")) {
			outputDocumentDir = (File)options.valueOf("outputDocumentDir");
		} else {
			dataTools.getOutputWriter().debugWriteln("ERROR: Missing 'outputDocumentDir' argument.");
			return false;
		}
	
		return true;
	}
}
