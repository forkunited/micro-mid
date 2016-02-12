package edu.psu.ist.acs.micro.mid.scratch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.util.FileUtil;

public class MIDRelevanceClassifier {
	private static String modelPath;
	private static DataTools dataTools;
	private static List<Feature<DocumentNLPDatum<Boolean>, Boolean>> features;
	private static SupervisedModel<DocumentNLPDatum<Boolean>, Boolean> model;
	
	public static void main(String[] args) {
		deserialize();
	}
	
	private static boolean deserialize() {
		BufferedReader ctxReader = FileUtil.getFileReader(modelPath);
		Context<DocumentNLPDatum<Boolean>, Boolean> context = Context.deserialize(DocumentNLPDatum.getBooleanTools(dataTools), ctxReader);
		try {
			ctxReader.close();
		} catch (IOException e) {
			return false;
		}
		
		features = context.getFeatures();
		model = context.getModels().get(0);
		
		return true;
	}
}
