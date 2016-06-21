package edu.psu.ist.acs.micro.mid.model.annotator.nlp;

import java.io.File;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorDocument;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassificationSupervisedModel;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

public class MIDRelevanceAnnotator implements AnnotatorDocument<Boolean> {
	private static final AnnotationType<?>[] REQUIRED_ANNOTATIONS = new AnnotationType<?>[] {
		AnnotationTypeNLP.TOKEN,
		AnnotationTypeNLP.POS
	};
	
	public static final File DEFAULT_MID_RELEVANCE_MODEL_FILE = new File("models/Relevance_Test_StanfordLinearCTOpt_NoProperNoun2_Unlabeled100");
	public static final String DEFAULT_MID_RELEVANCE_MODEL_PARSE_PATH = "eval.methodRelevance.methodFinal";
	
	private MIDDataTools dataTools;
	private Tools<DocumentNLPDatum<Boolean>, Boolean> midRelevanceDatumTools;
	
	private MethodClassificationSupervisedModel<DocumentNLPDatum<Boolean>, Boolean> midRelevanceClassifier;

	public MIDRelevanceAnnotator() {
		this(DEFAULT_MID_RELEVANCE_MODEL_FILE);
	}

	public MIDRelevanceAnnotator(File midRelevanceModelFile) {
		this(midRelevanceModelFile, DEFAULT_MID_RELEVANCE_MODEL_PARSE_PATH);
	}
	
	public MIDRelevanceAnnotator(File midRelevanceModelFile, String midRelevanceModelParsePath) {
		this.dataTools = new MIDDataTools(new OutputWriter(), new MIDProperties());
		this.midRelevanceDatumTools = DocumentNLPDatum.getBooleanTools(this.dataTools);
		
		if (!deserialize(midRelevanceModelFile,
						 midRelevanceModelParsePath))
			throw new IllegalArgumentException();
	}
	
	public boolean deserialize(File midRelevanceModelFile, String midRelevanceModelParsePath) {		
		this.midRelevanceClassifier = (MethodClassificationSupervisedModel<DocumentNLPDatum<Boolean>, Boolean>)
				DatumContext.run(this.midRelevanceDatumTools, midRelevanceModelFile).getMatchClassifyMethod(Obj.curlyBracedValue(midRelevanceModelParsePath));
		return true;
	}

	@Override
	public String getName() {
		return "psu_mid_rel-0.0.1";
	}

	@Override
	public AnnotationType<Boolean> produces() {
		return AnnotationTypeNLPMID.MID_CLASSIFIER_RELEVANCE_CLASS;
	}

	@Override
	public AnnotationType<?>[] requires() {
		return REQUIRED_ANNOTATIONS;
	}

	@Override
	public boolean measuresConfidence() {
		return true;
	}

	private DocumentNLPDatum<Boolean> constructMIDRelevanceDatum(DocumentNLP document) {
		return new DocumentNLPDatum<Boolean>(this.dataTools.getIncrementId(), document, null);
	}
	
	@Override
	public Pair<Boolean, Double> annotate(DocumentNLP document) {
		return this.midRelevanceClassifier.classifyWithScore(constructMIDRelevanceDatum(document));
	}
}
