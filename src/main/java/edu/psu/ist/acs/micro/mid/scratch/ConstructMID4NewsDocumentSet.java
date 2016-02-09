package edu.psu.ist.acs.micro.mid.scratch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID.TernaryRelevanceClass;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

public class ConstructMID4NewsDocumentSet {
	private static DateTimeFormatter dateOutputFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
	private static MIDDataTools dataTools;
	private static MIDProperties properties;
	private static Collection<AnnotationType<?>> annotationTypes = new ArrayList<AnnotationType<?>>();
	private static StoredCollection<DocumentNLPMutable, Document> documents;
	private static PipelineNLP nlpPipeline;
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		File inputDir = new File(args[0]);
		boolean onlyTokens = Boolean.valueOf(args[1]);
		
		dataTools = new MIDDataTools();
		properties = new MIDProperties();
		annotationTypes.addAll(dataTools.getAnnotationTypesNLP());
		annotationTypes.remove(AnnotationTypeNLP.SENTENCE);
		
		String collectionName = properties.getMIDNewsGoldRelevanceLabeledDocumentCollectionName() + ((onlyTokens) ? "_tokens" : "");
	
		Storage<?, Document> storage = properties.getStorage(dataTools, annotationTypes);
		if (storage.hasCollection(collectionName)) {
			storage.deleteCollection(collectionName);
		}
		
		documents = (StoredCollection<DocumentNLPMutable, Document>)storage.createCollection(collectionName, new SerializerDocumentNLPBSON(dataTools));
		
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
		
		List<Pair<File, TernaryRelevanceClass>> files = getFiles(inputDir);
		for (Pair<File, TernaryRelevanceClass> file : files) {
			if (!processFile(file.getFirst(), file.getSecond())) {
				System.out.println("Error: Failed to process file " + file.getFirst() + ". ");
				System.exit(0);
			}
		}
	}
	
	private static List<Pair<File, TernaryRelevanceClass>> getFiles(File inputDir) {
		File[] allFiles = inputDir.listFiles();
		List<Pair<File, TernaryRelevanceClass>> retFiles = new ArrayList<Pair<File, TernaryRelevanceClass>>();
	
		for (File file : allFiles) {
			if (!file.getName().startsWith("mid"))
				continue;
			String fileNameLwr = file.getName().toLowerCase();
			if (fileNameLwr.contains("false")) {
				retFiles.add(new Pair<File, TernaryRelevanceClass>(file, TernaryRelevanceClass.FALSE));
			} else if (fileNameLwr.contains("true")) {
				retFiles.add(new Pair<File, TernaryRelevanceClass>(file, TernaryRelevanceClass.TRUE));
			} else if (fileNameLwr.contains("cigar")) {
				retFiles.add(new Pair<File, TernaryRelevanceClass>(file, TernaryRelevanceClass.CIGAR));
			}
		}
	
		return retFiles;
	}
	
	private static boolean processFile(File file, TernaryRelevanceClass ternaryClass) {
		System.out.println("Processing file " + file.getName() + "...");
		BufferedReader r = FileUtil.getFileReader(file.getAbsolutePath());
		
		try {
			String line = null;
			StringBuilder documentStr = new StringBuilder();
			String prevDocument = null;
			int id = 0;
			while ((line = r.readLine()) != null) {
				String[] lineTokens = line.split("\\s+");
				
				if (lineTokens.length > 0 && lineTokens[0].toLowerCase().matches("x+")) {
					if (!processDocument(file.getName(), file.getName() + "." + id, documentStr.toString().trim(), ternaryClass)) {
						if (prevDocument != null) {
							System.out.println("--------------------------------\n\n\nPrior to failure, the most recent processed document was: " + prevDocument);
						}
						
						r.close();
						System.exit(0);
					}
					prevDocument = documentStr.toString();
					documentStr = new StringBuilder();
					id++;
				} else {
					documentStr.append(line.trim() + "\n");
				}
			}
			
		
			if (documentStr.toString().trim().length() > 0) {
				String[] lines = documentStr.toString().split("\\n");
				if (lines.length > 4) { // Handle edge case where there are garbage lines at the end
					if (!processDocument(file.getName(), file.getName() + "." + id, documentStr.toString().trim(), ternaryClass)) {
						if (prevDocument != null) {
							System.out.println("--------------------------------\n\n\nPrior to failure, the most recent processed document was: " + prevDocument);
						}
						
						r.close();
						System.exit(0);
					}
				}
			}
			
			r.close();
		} catch (IOException e) {
			System.err.println("Failed to process file " + file.getName());
			e.printStackTrace();
			System.exit(0);
		}
		
		return true;
	}
	
	private static boolean processDocument(String sourceFileName, String documentName, String text, TernaryRelevanceClass ternaryClass) {
		if (text.length() == 0)
			return true;
			
		boolean error = false;
		if (hasFormatWithClassHeader(text)) {
			if (!processDocumentFormatWithClassHeader(documentName, text, ternaryClass))
				error = true;
		} else if (hasFormatUnitedNations(text)) {
			if (!processDocumentFormatUnitedNations(documentName, text, ternaryClass))
				error = true;
		} else if (hasFormatNoClassHeader(text)) {
			if (!processDocumentFormatNoClassHeader(documentName, text, ternaryClass))
				error = true;
		} else {
			System.out.println("Document from " + sourceFileName + " has unrecognized format: " + text);
			return false;
		}
		
		if (error) {
			System.out.println("Error processing document from " + sourceFileName + ": " + text);
			return false;
		} else {
			return true;
		}
	}
	
	
	private static boolean hasFormatWithClassHeader(String text) {
		String[] lines = text.split("\n");
		String recentLine = null;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].trim().length() == 0)
				break;
			recentLine = lines[i].trim();
		}

		if (recentLine == null)
			return false;
		
		String[] dateParts = recentLine.split("/");
		if (dateParts.length < 3)
			return false;
		
		return StringUtils.isNumeric(dateParts[0]) && StringUtils.isNumeric(dateParts[1]) && StringUtils.isNumeric(dateParts[2]);
	}
	
	/*
	 * Process document text of format: 
	 *  
	 *  [Lines of garbage]
     *  false|true|cigar
     *  Line of garbage
     *  MM/DD/YYYY
     *
     *  Source (e.g. The New York Times)
     *
	 *  Date (e.g. November 14, 2000, Tuesday, Late Edition - Final)
     *
     *  Multiline title/headline
     *  Multiline title/headline
     *  ...
     *  
     *  BYLINE: byline
     *
     *  SECTION: section
     *  
     *  SOURCE: source
     *  
     *  LENGTH: length
     *  
     *  (DATELINE: dateline)
     *
     *  Several paragraphs of text
     *
     *  GARBAGE META-DATA LINES MOSTLY CAPITALIZED WITH NUMBERS
     *
	 */
	private static boolean processDocumentFormatWithClassHeader(String documentName, String text, TernaryRelevanceClass ternaryClass) {
		BufferedReader r = new BufferedReader(new StringReader(text));
		List<Pair<AnnotationTypeNLP<String>, String>> metaData = new ArrayList<Pair<AnnotationTypeNLP<String>, String>>();
		DateTimeFormatter dateParser = DateTimeFormat.forPattern("MM/dd/yyyy");
		String documentText = null;
		try {
			if (r.readLine() == null || r.readLine() == null) // Read two garbage lines
				return false;
			
			String[] firstLines = readUntilEmptyLine(r).split("\\s+");			
			String date = firstLines[firstLines.length - 1];
			metaData.add(
					new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_PUBLICATION_DATE, 
					dateParser.parseDateTime(date).toString(dateOutputFormat)));
			
			String source = readUntilNonEmptyLine(r);
			if (source == null)
				return false;
			else 
				metaData.add(new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_SOURCE, source));

			String uselessDate = readUntilNonEmptyLine(r);
			if (uselessDate == null)
				return false;
			
			// Multiline title
			String title = readUntilNonEmptyLine(r);
			if (title == null)
				return false;
			else 
				metaData.add(new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_TITLE, title));
			
			Pair<String, String> metaDataAndFirstTextLine = readUntilText(r);
			if (metaDataAndFirstTextLine == null)
				return false;
			
			metaData.addAll(parseMetaData(metaDataAndFirstTextLine.getFirst()));
			
			String restOfText = readUntilGarbageMetaData(r);
			if (restOfText == null)
				return false;

			documentText = metaDataAndFirstTextLine.getSecond() + restOfText;
		
			r.close();
		} catch (IOException e) {
			return false;
		}
		
		return constructAndSaveDocumentNLP(documentName, metaData, documentText, ternaryClass);
	}
	
	private static boolean hasFormatNoClassHeader(String text) {
		String[] lines = text.split("\n");
		
		// Hack to check if date of form MMMM dd, yyyy occurs in first 5 lines
		int numNonEmptyLines = 0;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].length() == 0)
				continue;
			
			if (isLineLongDate(lines[i]))
				return true;
			
			numNonEmptyLines++;
			if (numNonEmptyLines == 5)
				break;
		}
		
		return false; 
	}
	
	private static boolean isLineLongDate(String line) {
		String[] lineParts = line.split(",");
		if (lineParts.length < 2)
			return false;
		
		String[] monthDay = lineParts[0].split("\\s+");
		String year = lineParts[1].trim().split("\\s+")[0];
		
		return StringUtils.isNumeric(year) && StringUtils.isNumeric(monthDay[1]);
	}
	
	/*
	 * [Garbage line
	 * 
	 * http://... (Garbage URL)]
	 * 
	 * Source 
	 * 
	 * MMM DD, YYYY[, [...]] (date line)  (or Title (can be reverse order))
	 * 
	 * Title (or Date (can be reverse order))
	 * 
     *  BYLINE: byline
     *
     *  SECTION: section
     *  
     *  SOURCE: source
     *  
     *  LENGTH: length
     *  
     *  (DATELINE: dateline)
     *
     *  Several paragraphs of text
     *
     *  GARBAGE META-DATA LINES MOSTLY CAPITALIZED WITH NUMBERS
     */
	private static boolean processDocumentFormatNoClassHeader(String documentName, String text, TernaryRelevanceClass ternaryClass) {
		BufferedReader r = new BufferedReader(new StringReader(text));
		List<Pair<AnnotationTypeNLP<String>, String>> metaData = new ArrayList<Pair<AnnotationTypeNLP<String>, String>>();
		DateTimeFormatter dateParser = DateTimeFormat.forPattern("MMMM dd, yyyy");
		String documentText = null;
		try {
			String line1 = readUntilNonEmptyLine(r);
			if (line1 == null)
				return false;
			
			String line2 = readUntilNonEmptyLine(r);
			if (line2 == null)
				return false;
			
			String line3 = readUntilNonEmptyLine(r);
			if (line3 == null)
				return false;
			
			String source = null;
			String date = null;
			String title = null;
			
			if (isLineLongDate(line1)) {
				System.out.println("WARNING: Date occurred on first line for document " + documentName + ".  Skipping...");
				return true;
			} else if (isLineLongDate(line2)) {
				source = line1;
				date = line2;
				title = line3;
			} else if (isLineLongDate(line3)) {
				source = line1;
				title = line2;
				date = line3;
			} else {
				String line4 = readUntilNonEmptyLine(r);
				if (isLineLongDate(line4)) {
					source = line2;
					title = line3;
					date = line4;
				} else {
					String line5 = readUntilNonEmptyLine(r);
					if (isLineLongDate(line5)) {
						source = line3;
						title = line4;
						date = line5;
					} else {
						return false;
					}
				}
			}
			
			metaData.add(new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_SOURCE, source));

			String[] dateParts = date.split(",");
			date = dateParts[0].trim() + ", " + dateParts[1].trim().split("\\s+")[0];
			metaData.add(
					new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_PUBLICATION_DATE, 
					dateParser.parseDateTime(date).toString(dateOutputFormat)));
			
			metaData.add(new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_TITLE, title));
			
			Pair<String, String> metaDataAndFirstTextLine = readUntilText(r);
			if (metaDataAndFirstTextLine == null)
				return false;
			
			metaData.addAll(parseMetaData(metaDataAndFirstTextLine.getFirst()));
			
			String restOfText = readUntilGarbageMetaData(r);
			if (restOfText == null)
				return false;

			documentText = metaDataAndFirstTextLine.getSecond() + restOfText;
		
			r.close();
		} catch (IOException e) {
			return false;
		}
		
		return constructAndSaveDocumentNLP(documentName, metaData, documentText, ternaryClass);
	}

	
	private static boolean hasFormatUnitedNations(String text) {
		String lowerText = text.toLowerCase();
		return lowerText.contains("united\nnations\n") && lowerText.contains("general assembly\nsecurity council");
	}
	
	/*
	 * United nations format:
	 * 
	 * Several lines of garbage
	 * Several lines of garbage
	 * Several lines of garbage
	 * ...
	 * 
	 * Several lines of garbage
	 * Several lines of garbage
	 * ...
	 * 
	 * ...
	 * 
	 * UNITED
	 * NATIONS
	 * Some characters
	 * 
	 * General Assembly
	 * Security Council
	 * 
	 * Line of garbage
	 * Line of garbage
	 * dd MMMM yyyy
	 * 
	 * Multiline title part 1
	 * Multiline title part 1
	 * ...
	 * 
	 * Multiline title part 2
	 * Multiline title part 2
	 * ...
	 * 
	 * Several paragraphs of text
	 * 
	 */
	private static boolean processDocumentFormatUnitedNations(String documentName, String text, TernaryRelevanceClass ternaryClass) {
		BufferedReader r = new BufferedReader(new StringReader(text));
		List<Pair<AnnotationTypeNLP<String>, String>> metaData = new ArrayList<Pair<AnnotationTypeNLP<String>, String>>();
		DateTimeFormatter dateParser = DateTimeFormat.forPattern("dd MMMM yyyy");
		String documentText = null;
		try {
			String line = null;
			while ((line = r.readLine()) != null) {
				if (line.equals("General Assembly"))
					break;
			}
			
			if (line == null)
				return false;
			
			String securityCouncilLine = readUntilNonEmptyLine(r);
			if (!securityCouncilLine.equals("Security Council"))
				return false;
			
			
			String garbage = readUntilNonEmptyLine(r);
			if (garbage == null)
				return false;
			
			String garbage2 = readUntilNonEmptyLine(r);
			if (garbage2 == null)
				return false;
			
			metaData.add(new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_SOURCE, "United Nations"));

			String date = readUntilNonEmptyLine(r);
			if (date == null)
				return false;
			metaData.add(
					new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_PUBLICATION_DATE, 
					dateParser.parseDateTime(date.trim()).toString(dateOutputFormat)));
			
			// Multiline title
			String title = readUntilNonEmptyLine(r);
			if (title == null)
				return false;
			
			// Multiline title 2
			String title2 = readUntilNonEmptyLine(r);
			if (title2 == null)
				return false;
			
			metaData.add(new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_TITLE, title + " "+ title2));
			
			// Read text
			StringBuilder textBuilder = new StringBuilder();
			while ((line = r.readLine()) != null) {
				textBuilder.append(line + "\n");
			}
		
			documentText = textBuilder.toString();
			
			r.close();
		} catch (IOException e) {
			return false;
		}
		
		return constructAndSaveDocumentNLP(documentName, metaData, documentText, ternaryClass);
	}
	
	private static List<Pair<AnnotationTypeNLP<String>, String>> parseMetaData(String text) {
		List<Pair<AnnotationTypeNLP<String>, String>> metaData = new ArrayList<Pair<AnnotationTypeNLP<String>, String>>();
		String[] lines = text.split("\n");
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].startsWith("BYLINE:")) {
				String byline = lines[i].substring("BYLINE:".length()).trim();
				metaData.add(new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_BYLINE, byline));
			} else if (lines[i].startsWith("DATELINE:")) {
				String dateline = lines[i].substring("DATELINE:".length()).trim();
				metaData.add(new Pair<AnnotationTypeNLP<String>, String>(AnnotationTypeNLPMID.ARTICLE_DATELINE, dateline));
			}
		}
		
		return metaData;
	}
	
	private static boolean isText(String line) {
		if (line.length() == 0 || isGarbageMetaData(line))
			return false;
		
		String[] lineTokens = line.split("\\s+");
		if (lineTokens[0].endsWith(":"))
			return line.length() > 70;
		return true;
	}
	
	private static boolean isGarbageMetaData(String line) {
		if (line.length() == 0)
			return false;
		
		line = line.replaceAll("\\s+", "");
		char[] cs = line.toCharArray();
		double numAlertChars = 0.0;
		for (char c : cs) {
			if (c == ')' || c == '(' || Character.isDigit(c) || c == '%' || c == ':' || Character.isUpperCase(c)) {
				numAlertChars++;
			}
		}
	
		return numAlertChars / cs.length > 0.5 && line.contains(":");
	}
	
	private static String readUntilNonEmptyLine(BufferedReader r) {
		String line = null;
		try {
			while ((line = r.readLine()) != null) {
				if (line.length() > 0) {
					return line + " " + readUntilEmptyLine(r);
				}
			}
		} catch (IOException e) {
			return null;
		}
		
		return null;
	}
	
	private static String readUntilEmptyLine(BufferedReader r) {
		String line = null;
		try {
			StringBuilder nonEmptyLines = new StringBuilder();
			while ((line = r.readLine()) != null) {
				if (line.length() != 0) 
					nonEmptyLines.append(line + " ");
				else
					return nonEmptyLines.toString();
			}
		} catch (IOException e) {
			return null;
		}
		
		return null;
	}
	
	private static Pair<String, String> readUntilText(BufferedReader r) {
		String line = null;
		try {
			StringBuilder beforeLines = new StringBuilder();
			while ((line = r.readLine()) != null) {
				if (!isText(line)) 
					beforeLines.append(line + "\n");
				else
					return new Pair<String, String>(beforeLines.toString(), line);
			}
		} catch (IOException e) {
			return null;
		}
		
		return null;
	}
	
	private static String readUntilGarbageMetaData(BufferedReader r) {
		String line = null;
		try {
			StringBuilder textLines = new StringBuilder();
			while ((line = r.readLine()) != null) {
				if (!isGarbageMetaData(line)) 
					textLines.append(line + "\n");
				else
					return textLines.toString();
			}
			
			return textLines.toString();
		} catch (IOException e) {
			return null;
		}
	}
	
	private static boolean constructAndSaveDocumentNLP(String name, List<Pair<AnnotationTypeNLP<String>, String>> metaData, String text, TernaryRelevanceClass ternaryClass) {
		PipelineNLPExtendable metaDataPipeline = new PipelineNLPExtendable();
		
		for (Pair<AnnotationTypeNLP<String>, String> metaDatum : metaData) {
			metaDataPipeline.extend(new AnnotatorDocument<String>() {
				public String getName() { return "MID4-News"; }
				public boolean measuresConfidence() { return false; }
				public AnnotationType<String> produces() { return metaDatum.getFirst(); }
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
				public Pair<String, Double> annotate(DocumentNLP document) {
					return new Pair<String, Double>(metaDatum.getSecond(), null);
				}
			});
		}
			
		PipelineNLP fullPipeline = nlpPipeline.weld(metaDataPipeline);
		
		DocumentNLPMutable document = new DocumentNLPInMemory(dataTools, name, text);
		document = fullPipeline.run(document);
		
		documents.addItem(document);
		
		return true;
	}
}
