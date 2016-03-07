package edu.psu.ist.acs.micro.mid.scratch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON;
import edu.cmu.ml.rtw.generic.data.store.Storage;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorDocument;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPExtendable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLMentionCategorizer;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

public class ConstructMID5SVMNewsDocumentSet {
	private static MIDProperties properties;
	private static MIDDataTools dataTools;
	private static PipelineNLP nlpPipeline;
	private static StoredCollection<DocumentNLPMutable, Document> labeledDocuments;
	private static StoredCollection<DocumentNLPMutable, Document> unlabeledDocuments;
	private static Collection<AnnotationType<?>> annotationTypes = new ArrayList<AnnotationType<?>>();
	private static int writeBatchSize = 1;
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		String svmTruePositiveFilePath = args[0];
		String svmFalsePositiveFilePath = args[1];
		String svmNegativePath = args[2];
		boolean onlyTokens = Boolean.valueOf(args[3]);
		boolean onlyLabeled = Boolean.valueOf(args[4]);
		
		dataTools = new MIDDataTools();
		properties = new MIDProperties();
		annotationTypes.addAll(dataTools.getAnnotationTypesNLP());
		annotationTypes.remove(AnnotationTypeNLP.SENTENCE);
		
		String unlabeledCollectionName = properties.getMIDNewsSvmUnlabeledDocumentCollectionName() + ((onlyTokens) ? "_tokens" : "");
		String labeledCollectionName = properties.getMIDNewsSvmRelevanceLabeledDocumentCollectionName() + ((onlyTokens) ? "_tokens" : "");
		
		Storage<?, Document> storage = properties.getStorage(dataTools, annotationTypes);
		if (!onlyLabeled && storage.hasCollection(unlabeledCollectionName)) {
			storage.deleteCollection(unlabeledCollectionName);
		}
		
		if (storage.hasCollection(labeledCollectionName)) {
			storage.deleteCollection(labeledCollectionName);
		}
		
		labeledDocuments = (StoredCollection<DocumentNLPMutable, Document>)storage.createCollection(labeledCollectionName, new SerializerDocumentNLPBSON(dataTools));
		unlabeledDocuments = (StoredCollection<DocumentNLPMutable, Document>)storage.createCollection(unlabeledCollectionName, new SerializerDocumentNLPBSON(dataTools));
		
		PipelineNLPStanford pipelineStanford = new PipelineNLPStanford();
		
		if (onlyTokens) {
			pipelineStanford.initialize(AnnotationTypeNLP.POS);
			nlpPipeline = pipelineStanford;
		} else {
			NELLMentionCategorizer mentionCategorizer = new NELLMentionCategorizer(
					new CategoryList(CategoryList.Type.ALL_NELL_CATEGORIES, new CatDataTools()), 
					NELLMentionCategorizer.DEFAULT_MENTION_MODEL_THRESHOLD, NELLMentionCategorizer.DEFAULT_LABEL_TYPE, 
					1);
			PipelineNLPExtendable pipelineMicroCat = new PipelineNLPExtendable();
			pipelineMicroCat.extend(mentionCategorizer);
			
			nlpPipeline = pipelineStanford.weld(pipelineMicroCat);
		}
		
		
		constructDocumentsFromBulkText(svmTruePositiveFilePath, true, true);
	/*	constructDocumentsFromBulkText(svmFalsePositiveFilePath, true, false);
		
		if (!onlyLabeled)
			constructDocumentsFromBulkText(svmNegativePath, false, null);
		*/
		System.out.println("Finished writing documents.");
	}
	
	private static void constructDocumentsFromBulkText(String bulkTextPath, Boolean svmPositive, Boolean goldPositive) throws IOException {
		BufferedReader reader = FileUtil.getFileReader(bulkTextPath);
		String line = null;
		boolean documentContentLine = false;
		
		StringBuilder documentContent = new StringBuilder();
		PipelineNLP fullPipeline = null;
		String documentName = null;
		
		DateTimeFormatter verboseDateParser = DateTimeFormat.forPattern("MMMM dd, yyyy E");
		DateTimeFormatter shortDateParser = DateTimeFormat.forPattern("yyyyMMdd");
		DateTimeFormatter dateOutputFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
		List<DocumentNLPMutable> documents = new ArrayList<DocumentNLPMutable>();
		int i = 0;
		
		Set<String> documentNames = new HashSet<String>();
		int repeatDocuments = 0;
		
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0 
					|| line.equals(">>>>>>>>>>>>>>>>>>>>>>")
					|| line.equals("<<<<<<<<<<<<<<<<<<<<<<"))
				continue;
	
			if (!documentContentLine) {
				/* Parse meta-data */
				//System.out.println("Parsing document at: " + line);
				String firstLine = line;
				String otherLine = null;
				String newsSource = null;
				double svmScore = -1;
				String date = null;
				String dateLine = null;
				String byline = null;
				String key = null;
				String title = null;
				boolean readOneLine = false;
				while (!((line = reader.readLine()).trim()).equals("") || !readOneLine) {
					line = line.trim();
					readOneLine = true;
					if (line.length() == 0)
						continue;
					
					String lowerLine = line.toLowerCase();
					if (lowerLine.startsWith("news source:")) {
						// News source: (c) Japan Economic Newswire
						newsSource = line.substring("news source:".length()).trim();
					} else if (lowerLine.startsWith("svm score:")) {
						// SVM score: 1.1164
						svmScore = Double.valueOf(line.substring("svm score:".length()).trim());
					} else if (lowerLine.startsWith("date:")) {
						//Date: 20020401
						try {
							date = shortDateParser.parseDateTime(line.substring("date:".length()).trim()).toString(dateOutputFormat);
						} catch (IllegalArgumentException e) {
							
						}
					} else if (lowerLine.startsWith("source:")) {
						///Source: Associated Press Worldstream
						newsSource = line.substring("source:".length()).trim();
					} else if (lowerLine.startsWith("dateline:")) {
						//DATELINE: MOSCOW
						dateLine = line.substring("dateline:".length()).trim();
					} else if (lowerLine.startsWith("byline:")) {
						//BYLINE: VLADIMIR ISACHENKOV; Associated Press Writer
						byline = line.substring("byline:".length()).trim();
					} else if (line.startsWith("key: key:")) { // FIXME Note that this should be lowerLine
						// Key: Key: 20020401-56-0-AP_2002/April_2002/April01_2002_LN_NP1.TXT
						key = line.substring("key: key:".length()).trim();
					} else if (line.startsWith("key:")) { // FIXME Note that this should be lower line
						key = line.substring("key:".length()).trim();
					} else {
						try {
							date = verboseDateParser.parseDateTime(line).toString(dateOutputFormat);
						} catch (IllegalArgumentException e) {
							otherLine = line;
						}
					}
				}
				
				if (key == null) {
					documentName = firstLine;
					title = otherLine;
				} else {
					title = firstLine;
					documentName = key;
				}
				
				if (documentName.length() > 512)
					documentName = documentName.substring(0, 509) + "...";
				
				final String newsSourceFinal = newsSource;
				final double svmScoreFinal = svmScore;
				final String dateFinal = date;
				final String dateLineFinal = dateLine;
				final String bylineFinal = byline;
				final String titleFinal = title;
				
				documentContent = new StringBuilder();
				
				PipelineNLPExtendable metaDataPipeline = new PipelineNLPExtendable();
				
				if (titleFinal != null) {
					metaDataPipeline.extend(new AnnotatorDocument<String>() {
						public String getName() { return "MID5-News"; }
						public boolean measuresConfidence() { return false; }
						public AnnotationType<String> produces() { return AnnotationTypeNLPMID.ARTICLE_TITLE; }
						public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
						public Pair<String, Double> annotate(DocumentNLP document) {
							return new Pair<String, Double>(titleFinal, null);
						}
					});
				}
				
				if (dateFinal != null) {
					metaDataPipeline.extend(new AnnotatorDocument<String>() {
						public String getName() { return "MID5-News"; }
						public boolean measuresConfidence() { return false; }
						public AnnotationType<String> produces() { return AnnotationTypeNLPMID.ARTICLE_PUBLICATION_DATE; }
						public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
						public Pair<String, Double> annotate(DocumentNLP document) {
							return new Pair<String, Double>(dateFinal, null); 
						}
					});
				}
				
				if (newsSourceFinal != null) {
					metaDataPipeline.extend(new AnnotatorDocument<String>() {
						public String getName() { return "MID5-News"; }
						public boolean measuresConfidence() { return false; }
						public AnnotationType<String> produces() { return AnnotationTypeNLPMID.ARTICLE_SOURCE; }
						public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
						public Pair<String, Double> annotate(DocumentNLP document) {
							return new Pair<String, Double>(newsSourceFinal, null);
						}
					});
				}
				
				if (svmScoreFinal >= 0) {
					metaDataPipeline.extend(new AnnotatorDocument<Double>() {
						public String getName() { return "MID5-News"; }
						public boolean measuresConfidence() { return false; }
						public AnnotationType<Double> produces() { return AnnotationTypeNLPMID.MID_SVM_RELEVANCE_SCORE; }
						public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
						public Pair<Double, Double> annotate(DocumentNLP document) {
							return new Pair<Double, Double>(svmScoreFinal, null);
						}
					});
				}
				
				if (dateLineFinal != null) {
					metaDataPipeline.extend(new AnnotatorDocument<String>() {
						public String getName() { return "MID5-News"; }
						public boolean measuresConfidence() { return false; }
						public AnnotationType<String> produces() { return AnnotationTypeNLPMID.ARTICLE_DATELINE; }
						public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
						public Pair<String, Double> annotate(DocumentNLP document) {
							return new Pair<String, Double>(dateLineFinal, null);
						}
					});
				}
				
				if (bylineFinal != null) {
					metaDataPipeline.extend(new AnnotatorDocument<String>() {
						public String getName() { return "MID5-News"; }
						public boolean measuresConfidence() { return false; }
						public AnnotationType<String> produces() { return AnnotationTypeNLPMID.ARTICLE_BYLINE; }
						public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
						public Pair<String, Double> annotate(DocumentNLP document) {
							return new Pair<String, Double>(bylineFinal, null);
						}
					});
				}
				
				metaDataPipeline.extend(new AnnotatorDocument<Boolean>() {
					public String getName() { return "MID5-News"; }
					public boolean measuresConfidence() { return false; }
					public AnnotationType<Boolean> produces() { return AnnotationTypeNLPMID.MID_SVM_RELEVANCE_CLASS; }
					public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
					public Pair<Boolean, Double> annotate(DocumentNLP document) {
						return new Pair<Boolean, Double>(svmPositive, null);
					}
				});
				
				if (goldPositive != null) {
					metaDataPipeline.extend(new AnnotatorDocument<Boolean>() {
						public String getName() { return "MID5-News"; }
						public boolean measuresConfidence() { return false; }
						public AnnotationType<Boolean> produces() { return AnnotationTypeNLPMID.MID_GOLD_RELEVANCE_CLASS; }
						public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
						public Pair<Boolean, Double> annotate(DocumentNLP document) {
							return new Pair<Boolean, Double>(goldPositive, null);
						}
					});
				}
				
				fullPipeline = nlpPipeline.weld(metaDataPipeline);
				
				documentContentLine = true;
			} else if (line.equals("---------------------------------------------------------------")) {
				/* End of document text, so construct document */
				DocumentNLPMutable document = new DocumentNLPInMemory(dataTools, documentName, documentContent.toString());
				fullPipeline.run(document);
				
				if (writeBatchSize > 1) {
					i++;
					if (i % writeBatchSize == 0) {
						if (goldPositive != null)
							labeledDocuments.addItems(documents);
						else
							unlabeledDocuments.addItems(documents);
						
						documents = new ArrayList<DocumentNLPMutable>();
					}
					
					documents.add(document);
				} else {
					if (goldPositive != null)
						labeledDocuments.addItem(document);
					else
						unlabeledDocuments.addItem(document);
					
				}
				
				if (documentNames.contains(document.getName())) {
					System.out.println("Repeat name " + document.getName());
					repeatDocuments++;
				} else {
					documentNames.add(document.getName());
				}
				
				documentContentLine = false;
			} else if (documentContentLine) {
				documentContent.append(line).append(" ");
			}
		}
		
		if (writeBatchSize > 1) {
			if (goldPositive != null)
				labeledDocuments.addItems(documents);
			else
				unlabeledDocuments.addItems(documents);
		}
		
		System.out.println("Repeat docs: " + repeatDocuments);
	}
}
