package edu.psu.ist.acs.micro.mid.scratch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.util.Pair;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID.TernaryRelevanceClass;

public class ConstructMID4NewsDocumentSet {
	public static void main(String[] args) {
		File inputDir = new File(args[0]);
	
		List<Pair<File, TernaryRelevanceClass>> files = getFiles(inputDir);
		for (Pair<File, TernaryRelevanceClass> file : files) {
			if (!processFile(file.getFirst(), file.getSecond())) {
				System.out.println("Error: Failed to process file " + file.getFirst() + ". ");
				System.exit(0);
			}
		}
	}
	
	public static List<Pair<File, TernaryRelevanceClass>> getFiles(File inputDir) {
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
	
	public static boolean processFile(File file, TernaryRelevanceClass ternaryClass) {
	///	BufferedReader r = FileUtil.getFileReader(file.getAbsolutePath());
		// Send to XXXxx to lower case
		
		/*
		 
XXXX

Garbage

Lexis nexis stuff

Source

Date

OR:

XXXX
cigar/true/false
lexis nexis
date

Source

Date

Headline

BYLINE: byline

SECTION: section

SOURCE: source

LENGTH: length

(DATELINE: dateline)
		 */
		
		return true;
	}
}
