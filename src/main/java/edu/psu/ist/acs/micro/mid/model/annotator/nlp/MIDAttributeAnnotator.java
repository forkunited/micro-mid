package edu.psu.ist.acs.micro.mid.model.annotator.nlp;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

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
import edu.cmu.ml.rtw.generic.util.WeightedStringList;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;


public class MIDAttributeAnnotator implements AnnotatorDocument<WeightedStringList> {
	private static final AnnotationType<?>[] REQUIRED_ANNOTATIONS = new AnnotationType<?>[] {
		AnnotationTypeNLP.TOKEN,
		AnnotationTypeNLP.POS,
		AnnotationTypeNLP.CONSTITUENCY_PARSE,
		AnnotationTypeNLP.DEPENDENCY_PARSE,
		AnnotationTypeNLP.NER,
		AnnotationTypeNLP.PREDICATE
	};
	
	public static final File DEFAULT_MID_ATTRIBUTE_MODEL_FILE = new File("");
	public static final String DEFAULT_MID_ATTRIBUTE_MODEL_PARSE_PATH_PREFIX = "";
	public static final String DEFAULT_MID_ATTRIBUTE_MODEL_PARSE_PATH_SUFFIX = "";
	
	private MIDDataTools dataTools;
	private Tools<DocumentNLPDatum<Boolean>, Boolean> midAttributeDatumTools;
	
	private Map<String, MethodClassificationSupervisedModel<DocumentNLPDatum<Boolean>, Boolean>> midAttributeClassifiers;

	public MIDAttributeAnnotator() {
		this(DEFAULT_MID_ATTRIBUTE_MODEL_FILE);
	}

	public MIDAttributeAnnotator(File midAttrModelFile) {
		this(midAttrModelFile, DEFAULT_MID_ATTRIBUTE_MODEL_PARSE_PATH_PREFIX, DEFAULT_MID_ATTRIBUTE_MODEL_PARSE_PATH_SUFFIX);
	}
	
	public MIDAttributeAnnotator(File midAttrModelFile, String midAttrModelParsePathPrefix, String midAttrModelParsePathSuffix) {
		this.dataTools = new MIDDataTools(new OutputWriter(), new MIDProperties());
		this.midAttributeDatumTools = DocumentNLPDatum.getBooleanTools(this.dataTools);
		
		if (!deserialize(midAttrModelFile,
						 midAttrModelParsePathPrefix,
						 midAttrModelParsePathSuffix))
			throw new IllegalArgumentException();
	}
	
	public boolean deserialize(File midAttrModelFile, String midAttrModelParsePathPrefix, String midAttrModelParsePathSuffix) {		
		this.midAttributeClassifiers = new TreeMap<>();
		
		Set<String> attrs = new HashSet<String>();
		for (MIDIncident.HostilityLevel hostLevel : MIDIncident.HostilityLevel.values()) {
			attrs.add(hostLevel.toString());
		}
		
		for (MIDIncident.Action action : MIDIncident.Action.values()) {
			attrs.add(action.toString());
		}
		
		DatumContext<DocumentNLPDatum<Boolean>, Boolean> ctx = DatumContext.run(this.midAttributeDatumTools, midAttrModelFile);
		
		for (String attr : attrs) {
			String parsePath = midAttrModelParsePathPrefix + attr + midAttrModelParsePathSuffix;
			this.midAttributeClassifiers.put(attr, 
					(MethodClassificationSupervisedModel<DocumentNLPDatum<Boolean>, Boolean>)
						ctx.getMatchClassifyMethod(Obj.curlyBracedValue(parsePath)));
		}
		
		return true;
	}

	@Override
	public String getName() {
		return "psu_mid_attr-0.0.1";
	}

	@Override
	public AnnotationType<WeightedStringList> produces() {
		return AnnotationTypeNLPMID.MID_ATTRIBUTE;
	}

	@Override
	public AnnotationType<?>[] requires() {
		return REQUIRED_ANNOTATIONS;
	}

	@Override
	public boolean measuresConfidence() {
		return false;
	}

	private DocumentNLPDatum<Boolean> constructMIDAttributeDatum(DocumentNLP document) {
		return new DocumentNLPDatum<Boolean>(this.dataTools.getIncrementId(), document, null);
	}
	
	@Override
	public Pair<WeightedStringList, Double> annotate(DocumentNLP document) {
		DocumentNLPDatum<Boolean> datum = constructMIDAttributeDatum(document);

		Map<String, Double> scores = new TreeMap<>();
		for (Entry<String, MethodClassificationSupervisedModel<DocumentNLPDatum<Boolean>, Boolean>> entry : this.midAttributeClassifiers.entrySet()) {
			Pair<Boolean, Double> c = entry.getValue().classifyWithScore(datum);
			if (c.getFirst()) {
				double score = c.getSecond();
				scores.put(entry.getKey(), score);
			}
		}
		WeightedStringList anno = new WeightedStringList(scores);
		return new Pair<WeightedStringList, Double>(anno, null);
	}
}
