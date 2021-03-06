package edu.psu.ist.acs.micro.mid.scratch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.StoredItemSet;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.time.NormalizedTimeValue;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.time.TimeExpression;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.time.TimeExpression.TimeMLDocumentFunction;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.time.TimeExpression.TimeMLType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPHTML;
import edu.cmu.ml.rtw.generic.data.store.StorageFileSystem;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPExtendable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPMateTools;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLMentionCategorizer;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.AnnotationTypeNLPEvent;
import edu.psu.ist.acs.micro.event.data.annotation.nlp.event.EventMention;
import edu.psu.ist.acs.micro.event.model.annotator.nlp.EventAnnotator;
import edu.psu.ist.acs.micro.mid.data.MID5FileReader;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;
import edu.psu.ist.acs.micro.mid.model.annotator.nlp.MIDAttributeAnnotator;
import edu.psu.ist.acs.micro.mid.model.annotator.nlp.MIDRelevanceAnnotator;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

/**
 * 
 * RunMIDPipeline annotates documents in the format
 * parsed by edu.psu.ist.acs.micro.mid.data.MID5FileReader.  
 * All documents are annotated with Stanford CoreNLP tokenization,
 * sentence splitting, lemmatization, pos tagging, and MID relevance.
 * If a document is classified as MID relevant, then it is also
 * annotated with Stanford CoreNLP annotations
 * dependency parsing, constituency parsing, NER, and 
 * time expressions, MateTools SRL, event mentions (with
 * TimeML attributes), NELL noun-phrase 
 * categories, and MID attributes (actions and hostility level).
 * MID relevance and MID attributes are annotated using 
 * edu.psu.ist.acs.micro.mid.model.annotator.nlp.MIDRelevanceAnnotator
 * and 
 * edu.psu.ist.acs.micro.mid.model.annotator.nlp.MIDAttributeAnnotator.
 * 
 * Annotated documents are output in BSON and HTML formats, but the HTML
 * is only output for documents that are classified as MID relevant.
 * 
 * Arguments:
 * --input - Input file or directory
 * --maxThreads - Maximum number of threads
 * --storageDir - Directory in which to store output BSON annotations
 * --htmlStorageDir - Directory in which to store output HTML annotations
 * --outputRelevanceFile - File in which to output MID relevance
 * --propertiesFile - Properties configuration file
 * 
 * --help will display a help menu.
 * 
 * Arguments should be prefixed by argument names of the form
 * "--[argumentName] "
 * 
 * @author Bill McDowell
 *
 */
public class RunMIDPipeline {
	private static final int MAX_SENTENCE_LENGTH = 30;
	private static final int BATCH_SIZE = 10;
	private static final String STORAGE_NAME = "PipelineStorage";
	private static final String HTML_STORAGE_NAME = "PipelineHtmlStorage";
	
	private static File input;
	private static int maxThreads;
	private static File outputRelevanceFile;
	private static boolean storeHtml = false;
	
	private static MIDDataTools dataTools;
	private static BufferedWriter outputRelevanceWriter;
	private static StoredCollection<DocumentNLPMutable, ?> outputDocuments;
	
	private static StoredItemSet<TimeExpression, TimeExpression> storedTimexes;
	private static StoredItemSet<NormalizedTimeValue, NormalizedTimeValue> storedTimeValues;
	
	private static PipelineNLP pipelineLong;
	private static PipelineNLP pipelineShort;
	
	public static void main(String[] args) {
		if (!parseArgs(args)) {
			System.out.println("ERROR: Failed to parse arguments.");
			return;
		}
		
		List<AnnotationType<?>> annotationTypes = new ArrayList<AnnotationType<?>>();
		annotationTypes.addAll(dataTools.getAnnotationTypesNLP());
		annotationTypes.remove(AnnotationTypeNLP.SENTENCE);
		
		if (dataTools.getStoredItemSetManager().getStorage(STORAGE_NAME).hasCollection("docs"))
			dataTools.getStoredItemSetManager().getStorage(STORAGE_NAME).deleteCollection("docs");
		
		if (dataTools.getStoredItemSetManager().getStorage(HTML_STORAGE_NAME).hasCollection("docs"))
			dataTools.getStoredItemSetManager().getStorage(HTML_STORAGE_NAME).deleteCollection("docs");
		
		outputDocuments = dataTools.getStoredItemSetManager().getItemSet(
			STORAGE_NAME, 
			"docs", 
			true, 
			new SerializerDocumentNLPBSON(new DocumentNLPInMemory(dataTools), annotationTypes)).getStoredItems();
		
		if (storeHtml) {
			dataTools.getStoredItemSetManager().getItemSet(
					HTML_STORAGE_NAME, 
					"docs", 
					true, 
					new SerializerDocumentNLPHTML(new DocumentNLPInMemory(dataTools))).getStoredItems();
		}
		
		if (outputRelevanceFile != null) {
			outputRelevanceWriter = FileUtil.getFileWriter(outputRelevanceFile.getAbsolutePath());
		}
	
		if (!constructPipelines()) {
			System.out.println("ERROR: Failed to construct pipelines.");
		}
		
		List<MID5FileReader> readers = getInputReaders(maxThreads);
		ThreadMapper<MID5FileReader, Boolean> threadMapper = new ThreadMapper<MID5FileReader, Boolean>(
			new ThreadMapper.Fn<MID5FileReader, Boolean>() {
				@Override
				public Boolean apply(MID5FileReader reader) {
					int datumId = 0;
					Pair<String, Map<AnnotationTypeNLP<?>, Object>> nameAndAnnotations = null;
					List<DocumentNLPMutable> documents = new ArrayList<>();
					try {
						while ((nameAndAnnotations = reader.readOne()) != null) {
							synchronized (args) {
								System.out.println("Processing document " + nameAndAnnotations.getFirst() + " (" + reader.getReadCount() + ")...");
							}
							
							DocumentNLPMutable document = new DocumentNLPInMemory(dataTools,
																				  nameAndAnnotations.getFirst(), 
																				  nameAndAnnotations.getSecond().get(AnnotationTypeNLP.ORIGINAL_TEXT).toString());
						
							for (Entry<AnnotationTypeNLP<?>, Object> entry : nameAndAnnotations.getSecond().entrySet()) {
								if (entry.getKey().equals(AnnotationTypeNLPMID.ARTICLE_PUBLICATION_DATE)) {
									String timexId = String.valueOf(dataTools.getIncrementId());
									String valueId = String.valueOf(dataTools.getIncrementId());
									StoreReference timexRef = new StoreReference(storedTimexes.getStoredItems().getStorageName(), storedTimexes.getName(), "id", String.valueOf(timexId));
									StoreReference valueRef = new StoreReference(storedTimeValues.getStoredItems().getStorageName(), storedTimeValues.getName(), "id", String.valueOf(valueId));
									
									List<StoreReference> timexRefs = new ArrayList<>();
									timexRefs.add(timexRef);
									
									String valueStr = entry.getValue().toString();
									NormalizedTimeValue value = new NormalizedTimeValue(document.getDataTools(), 
																		    			valueRef, 
																		    			valueId, 
																		    			valueStr, 
																		    			timexRefs);
							        
							        TimeExpression timex = new TimeExpression(document.getDataTools(), 
											  timexRef,
											  new TokenSpan(document, -1, -1, -1),
											  timexId,
											  "",
											  TimeMLType.TIME,
											  null,
											  null,
											  null,
											  null,
											  valueRef,
											  TimeMLDocumentFunction.CREATION_TIME,
											  false,
											  null,
											  null,
											  null);
							        
							        storedTimexes.addItem(timex);
							        storedTimeValues.addItem(value);
							        
							        document.setDocumentAnnotation("MID5-News", AnnotationTypeNLPEvent.CREATION_TIME, new Pair<Object, Double>(timex, 1.0)); 
								}
								
								if (!entry.getKey().equals(AnnotationTypeNLP.ORIGINAL_TEXT)) {
									document.setDocumentAnnotation("MID5-News", entry.getKey(), new Pair<Object, Double>(entry.getValue(), null));
								}
							}
							
							document = pipelineShort.run(document);
							boolean midRelevant = document.getDocumentAnnotation(AnnotationTypeNLPMID.MID_CLASSIFIER_RELEVANCE_CLASS);
							if (midRelevant)
								document = pipelineLong.run(document);
							
							
							documents.add(document);
							datumId++;
							
							if (datumId % BATCH_SIZE == 0) {
								if (!output(documents)) {
									System.out.println("ERROR: Failed to classify and/or output.");
									System.exit(0);
								}
								
								documents = new ArrayList<>();
							}
							
						}
						
						if (documents.size() > 0) {
							if (!output(documents)) {
								System.out.println("Error in classification or output.");
								System.exit(0);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
						reader.close();
						System.exit(0);
					}
					
					return true;
				}		
			}
		);
		
		threadMapper.run(readers, maxThreads);
		
		for (MID5FileReader r : readers) {
			r.close();
		}
			
		if (outputRelevanceWriter != null) {
			try {
				outputRelevanceWriter.close();
			} catch (IOException e) {
				
			}
		}
		
		sortRelevance();
	}
	
	private static void sortRelevance() {
		BufferedReader r = FileUtil.getFileReader(outputRelevanceFile.getAbsolutePath());
		List<Pair<String, Double>> relevanceResults = new ArrayList<>();
		String line = null;
		
		try {
			while ((line = r.readLine()) != null) {
				int tabIndex = line.lastIndexOf('\t');
				String prefix = line.substring(0, tabIndex);
				double score = Double.valueOf(line.substring(tabIndex + 1));
				relevanceResults.add(new Pair<String, Double>(prefix, score));
			}
			r.close();
			
			relevanceResults.sort(new Comparator<Pair<String, Double>>() {
				@Override
				public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
					return o2.getSecond().compareTo(o1.getSecond());
				}
			});
			
			outputRelevanceWriter = FileUtil.getFileWriter(outputRelevanceFile.getAbsolutePath());
			for (Pair<String, Double> result : relevanceResults)
				outputRelevanceWriter.write(result.getFirst() + "\t" + result.getSecond() + "\n");
			outputRelevanceWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private static boolean constructPipelines() {
		if (dataTools.getStoredItemSetManager().getStorage(STORAGE_NAME).hasCollection("tvalues"))
			dataTools.getStoredItemSetManager().getStorage(STORAGE_NAME).deleteCollection("tvalues");
		
		storedTimeValues = 
				dataTools.getStoredItemSetManager().getItemSet(
					STORAGE_NAME, 
					"tvalues", 
					true, 
					(Serializer<NormalizedTimeValue, Document>)dataTools.getSerializers().get("JSONBSONNormalizedTimeValue"));
		
		if (dataTools.getStoredItemSetManager().getStorage(STORAGE_NAME).hasCollection("timexes"))
			dataTools.getStoredItemSetManager().getStorage(STORAGE_NAME).deleteCollection("timexes");
		
		storedTimexes = 
				dataTools.getStoredItemSetManager().getItemSet(
						STORAGE_NAME, 
						"timexes", 
						true, 
						(Serializer<TimeExpression, Document>)dataTools.getSerializers().get("JSONBSONTimeExpression"));
	
		if (dataTools.getStoredItemSetManager().getStorage(STORAGE_NAME).hasCollection("evmentions"))
			dataTools.getStoredItemSetManager().getStorage(STORAGE_NAME).deleteCollection("evmentions");
		
		StoredItemSet<EventMention, EventMention> storedEventMentions = 
				dataTools.getStoredItemSetManager().getItemSet(
					STORAGE_NAME, 
					"evmentions", 
					true, 
					(Serializer<EventMention, Document>)dataTools.getSerializers().get("JSONBSONEventMention"));
		
		PipelineNLPStanford pipelineStanfordShort = new PipelineNLPStanford(MAX_SENTENCE_LENGTH);
		pipelineStanfordShort.initialize(AnnotationTypeNLP.CONSTITUENCY_PARSE);
		PipelineNLPExtendable pipelineOtherShort = new PipelineNLPExtendable();
		pipelineOtherShort.extend(new MIDRelevanceAnnotator());
		pipelineShort = pipelineStanfordShort.weld(pipelineOtherShort);
		
		
		PipelineNLPStanford pipelineStanfordLong = new PipelineNLPStanford(MAX_SENTENCE_LENGTH);
		pipelineStanfordLong.initialize(AnnotationTypeNLP.COREF, null, storedTimexes, storedTimeValues);
		PipelineNLPMateTools pipelineMateTools = new PipelineNLPMateTools(dataTools.getProperties(), MAX_SENTENCE_LENGTH);
		PipelineNLPExtendable pipelineOtherLong = new PipelineNLPExtendable();
		pipelineOtherLong.extend(new MIDRelevanceAnnotator());
		pipelineOtherLong.extend(new NELLMentionCategorizer(
				new CategoryList(CategoryList.Type.ALL_NELL_CATEGORIES, new CatDataTools()), 
				NELLMentionCategorizer.DEFAULT_MENTION_MODEL_THRESHOLD, NELLMentionCategorizer.DEFAULT_LABEL_TYPE, 
				1));
		pipelineOtherLong.extend(new EventAnnotator(storedEventMentions, dataTools));
		pipelineOtherLong.extend(new MIDAttributeAnnotator());
		pipelineLong = pipelineStanfordLong.weld(pipelineMateTools).weld(pipelineOtherLong);
		
		return true;
	}
	
	private static List<MID5FileReader> getInputReaders(int minReaders) {
		List<MID5FileReader> readers = new ArrayList<MID5FileReader>();
		
		if (input.isDirectory()) {
			File[] files = input.listFiles();
			for (File file : files)
				readers.add(new MID5FileReader(file));
		} else {
			readers.add(new MID5FileReader(input));
		}
		
		int i = 0;
		while (minReaders > 0 && readers.size() < minReaders) {
			readers.add(new MID5FileReader(readers.get(i)));
			i++;
		}
		
		return readers;
	}
	
	private static boolean output(List<DocumentNLPMutable> documents) {
		try {
			for (DocumentNLPMutable document : documents) {
				if (outputDocuments != null)
					outputDocuments.addItem(document);
				
				Boolean relevant = document.getDocumentAnnotation(AnnotationTypeNLPMID.MID_CLASSIFIER_RELEVANCE_CLASS);
				
				if (outputRelevanceWriter != null) {
					double confidence = document.getDocumentAnnotationConfidence(AnnotationTypeNLPMID.MID_CLASSIFIER_RELEVANCE_CLASS);
					if (!relevant)
						confidence = 1.0 - confidence;
					outputRelevanceWriter.write(document.getName() + "\t" + relevant + "\t" + confidence + "\n");
				}
				
				if (storeHtml && relevant) {
					dataTools.getStoredItemSetManager().getItemSet(
							HTML_STORAGE_NAME, 
							"docs").getStoredItems().addItem(document);
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
		parser.accepts("storageDir").withRequiredArg()
			.describedAs("Path to directory for storing output documents and other objects")
			.ofType(File.class);
		parser.accepts("htmlStorageDir").withRequiredArg()
			.describedAs("Path to directory for storing output html")
			.ofType(File.class);
		parser.accepts("outputRelevanceFile").withRequiredArg()
			.describedAs("Path to output relevance classification file")
			.ofType(File.class);
		parser.accepts("propertiesFile").withRequiredArg()
			.describedAs("Path to properties configuration")
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
		
		if (!options.has("propertiesFile")) {
			dataTools.getOutputWriter().debugWriteln("ERROR: Missing 'propertiesFile' argument.");
			return false;
		}
		
		output.debugWriteln("Loading data tools (gazetteers etc)...");
		dataTools = new MIDDataTools(output, new MIDProperties(((File)options.valueOf("propertiesFile")).getAbsolutePath()));
		output.debugWriteln("Finished loading data tools.");
		
		if (options.has("input")) {
			input = (File)options.valueOf("input");
		} else {
			dataTools.getOutputWriter().debugWriteln("ERROR: Missing 'input' argument.");
			return false;
		}
		
		maxThreads = (int)options.valueOf("maxThreads");
		
		if (options.has("outputRelevanceFile")) {
			outputRelevanceFile = (File)options.valueOf("outputRelevanceFile");
		} else {
			dataTools.getOutputWriter().debugWriteln("ERROR: Missing 'outputRelevanceFile' argument.");
			return false;
		}
	
		
		if (options.has("storageDir")) {
			dataTools.getStoredItemSetManager().addStorage(
				new StorageFileSystem<Document>(STORAGE_NAME, ((File)options.valueOf("storageDir")).getPath(), dataTools.getSerializers()));
		} else {
			dataTools.getOutputWriter().debugWriteln("ERROR: Missing 'storageDir' argument.");
			return false;
		}
		
		if (options.has("storageDir")) {
			storeHtml = true;
			dataTools.getStoredItemSetManager().addStorage(
				new StorageFileSystem<Document>(HTML_STORAGE_NAME, ((File)options.valueOf("htmlStorageDir")).getPath(), dataTools.getSerializers()));
		} 
	
		return true;
	}
}
