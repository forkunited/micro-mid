package edu.psu.ist.acs.micro.mid.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Singleton;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;

/**
 * MID5FileReader reads documents from files in the following bulk
 * format:
 * 
 * ----------------------------------
 * 
 * Key: [document-name-0]
 * meta-data-0: [meta-data0-0]
 * meta-data-1: [meta-data0-1]
 * [...]
 * meta-data-n: [meta-data0-n]
 * 
 * >>>>>>>>>>
 * 
 * [document-content0]
 * 
 * <<<<<<<<<<
 * 
 * ------------------------------------
 * 
 * Key: [document-name-1]
 * meta-data-0: [meta-data1-0]
 * meta-data-1: [meta-data1-1]
 * [...]
 * meta-data-n: [meta-data1-n]
 * 
 * >>>>>>>>>>
 * 
 * [document-content1]
 * 
 * <<<<<<<<<<
 * 
 * ------------------------------------
 * 
 * [...]
 * 
 * @author Bill McDowell
 *
 */
public class MID5FileReader {
	private BufferedReader r;
	private Singleton<Integer> readCount;
	
	public MID5FileReader(String bulkFilePath) {
		this.r = FileUtil.getFileReader(bulkFilePath);
		this.readCount = new Singleton<Integer>(0);
	}
	
	public MID5FileReader(File bulkFile) {
		this.r = FileUtil.getFileReader(bulkFile.getAbsolutePath());
		this.readCount = new Singleton<Integer>(0);
	}
	
	public MID5FileReader(MID5FileReader reader) {
		this.r = reader.r;
		this.readCount = reader.readCount;
	}
	
	/**
	 * 
	 * @param r
	 * @return a pair containing the next document name and the document's annotations read
	 * next from the bulk file reader.  The text content of the document is contained in
	 * the "ORIGINAL_TEXT" annotation in the annotation map.
	 * 
	 * @throws IOException
	 *
	 */
	public Pair<String, Map<AnnotationTypeNLP<?>, Object>> readOne() throws IOException {
		synchronized (this.r) {
			return readOne(this.r);
		}
	}
	
	private Pair<String, Map<AnnotationTypeNLP<?>, Object>> readOne(BufferedReader r) throws IOException {
		Map<AnnotationTypeNLP<?>, Object> annotations = new HashMap<AnnotationTypeNLP<?>, Object>();
		String documentName = null;
		
		String line = null;
		boolean documentContentLine = false;
		StringBuilder documentContent = new StringBuilder();
		
		DateTimeFormatter verboseDateParser = DateTimeFormat.forPattern("MMMM dd, yyyy E");
		DateTimeFormatter shortDateParser = DateTimeFormat.forPattern("yyyyMMdd");
		DateTimeFormatter dateOutputFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
		
		while ((line = r.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0 
					|| line.matches(">>>>+")
					|| line.matches("<<<<+"))
				continue;
	
			if (!documentContentLine) {
				/* Parse meta-data */
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
				while (!((line = r.readLine()).trim()).equals("") || !readOneLine) {
					line = line.trim();
					readOneLine = true;
					if (line.length() == 0)
						continue;
					
					String lowerLine = line.toLowerCase();
					if (lowerLine.startsWith("headline:")) {
						title = line.substring("headline:".length()).trim();
					} else if (lowerLine.startsWith("news source:")) {
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
					} else if (lowerLine.startsWith("key: key:")) { // FIXME Note that this should be lowerLine
						// Key: Key: 20020401-56-0-AP_2002/April_2002/April01_2002_LN_NP1.TXT
						key = line.substring("key: key:".length()).trim();
					} else if (lowerLine.startsWith("key:")) { // FIXME Note that this should be lower line
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
					if (title == null)
						title = otherLine;
				} else {
					if (title == null)
						title = firstLine;
					documentName = key;
				}
				
				if (documentName.startsWith("Key:"))
					documentName = documentName.substring(4).trim();
				
				if (documentName.length() > 512)
					documentName = documentName.substring(0, 509) + "...";
				
				if (newsSource != null)
					annotations.put(AnnotationTypeNLPMID.ARTICLE_SOURCE, newsSource);
				if (svmScore >= 0)
					annotations.put(AnnotationTypeNLPMID.MID_SVM_RELEVANCE_SCORE, svmScore);
				if (date != null) {
					annotations.put(AnnotationTypeNLPMID.ARTICLE_PUBLICATION_DATE, date);
				}
				if (dateLine != null)
					annotations.put(AnnotationTypeNLPMID.ARTICLE_DATELINE, dateLine);
				if (byline != null)
					annotations.put(AnnotationTypeNLPMID.ARTICLE_BYLINE, byline);
				if (title != null)
					annotations.put(AnnotationTypeNLPMID.ARTICLE_TITLE, title);
				
				documentContent = new StringBuilder();
				
				documentContentLine = true;
			} else if (line.matches("-----+")) {
				annotations.put(AnnotationTypeNLP.ORIGINAL_TEXT, documentContent.toString());
				documentContentLine = false;
				return new Pair<String, Map<AnnotationTypeNLP<?>, Object>>(documentName, annotations);
			} else if (documentContentLine) {
				documentContent.append(line).append("\n");
			}
		}
	
		this.readCount.set(this.readCount.get() + 1);
		
		if (documentContent.length() > 0 && annotations.size() > 0) {
			annotations.put(AnnotationTypeNLP.ORIGINAL_TEXT, documentContent.toString());
			return new Pair<String, Map<AnnotationTypeNLP<?>, Object>>(documentName, annotations);
		} else
			return null;
	}
	
	public int getReadCount() {
		return this.readCount.get();
	}
	
	public boolean close() {
		try {
			r.close();
		} catch (Exception e) {
			return false;
		}
		
		return true;
	}
}
