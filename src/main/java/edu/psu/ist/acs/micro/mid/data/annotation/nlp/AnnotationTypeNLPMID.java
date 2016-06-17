package edu.psu.ist.acs.micro.mid.data.annotation.nlp;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDDispute;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident.HostilityLevel;

/**
 * 
 * AnnotationTypeNLPEvent represents types of annotations that
 * the micro-event project can add to NLP documents
 * 
 * @author Bill McDowell
 *
 */
public class AnnotationTypeNLPMID {
	public static enum TernaryRelevanceClass {
		TRUE,
		FALSE,
		CIGAR
	}
	
	public static final AnnotationTypeNLP<Double> MID_SVM_RELEVANCE_SCORE = new AnnotationTypeNLP<Double>("mid-svm-rel-score", Double.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_SVM_RELEVANCE_CLASS = new AnnotationTypeNLP<Boolean>("mid-svm-rel-class", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_GOLD_RELEVANCE_CLASS = new AnnotationTypeNLP<Boolean>("mid-gold-rel-class", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_CLASSIFIER_RELEVANCE_CLASS = new AnnotationTypeNLP<Boolean>("mid-classifier-rel-class", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Double> MID_CLASSIFIER_RELEVANCE_SCORE = new AnnotationTypeNLP<Double>("mid-classifier-rel-score", Double.class, Target.DOCUMENT);

	public static final AnnotationTypeNLP<TernaryRelevanceClass> MID_GOLD_TERNARY_RELEVANCE_CLASS = new AnnotationTypeNLP<TernaryRelevanceClass>("mid-gold-ternary-rel-class", TernaryRelevanceClass.class, Target.DOCUMENT);
	
	public static final AnnotationTypeNLP<MIDDispute> MID_DISPUTE = new AnnotationTypeNLP<MIDDispute>("mid-disp", MIDDispute.class, Target.DOCUMENT);

	public static final AnnotationTypeNLP<String> ARTICLE_TITLE = new AnnotationTypeNLP<String>("article-title", String.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_PUBLICATION_DATE = new AnnotationTypeNLP<String>("article-pub-date", String.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_SOURCE = new AnnotationTypeNLP<String>("article-source", String.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_BYLINE = new AnnotationTypeNLP<String>("article-byline", String.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_DATELINE = new AnnotationTypeNLP<String>("article-dateline", String.class, Target.DOCUMENT);

	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_NO_MILITARIZED_ACTION = new AnnotationTypeNLP<Boolean>("mid-attr-no-militarized-action", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_THREAT_TO_USE_FORCE = new AnnotationTypeNLP<Boolean>("mid-attr-threat-to-use-force", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_THREAT_TO_BLOCKADE = new AnnotationTypeNLP<Boolean>("mid-attr-threat-to-blockade", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_THREAT_TO_OCCUPY_TERRITORY = new AnnotationTypeNLP<Boolean>("mid-attr-threat-to-occupy-territory", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_THREAT_TO_DECLARE_WAR = new AnnotationTypeNLP<Boolean>("mid-attr-threat-to-declare-war", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_THREAT_TO_USE_CBR_WEAPONS = new AnnotationTypeNLP<Boolean>("mid-attr-threat-to-use-cbr-weapons", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_THREAT_TO_JOIN_WAR = new AnnotationTypeNLP<Boolean>("mid-attr-threat-to-join-war", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_DISPLAY_OF_FORCE = new AnnotationTypeNLP<Boolean>("mid-attr-display-of-force", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_SHOW_OF_FORCE = new AnnotationTypeNLP<Boolean>("mid-attr-show-of-force", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_ALERT = new AnnotationTypeNLP<Boolean>("mid-attr-alert", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_NUCLEAR_ALERT = new AnnotationTypeNLP<Boolean>("mid-attr-nuclear-alert", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_MOBILIZATION = new AnnotationTypeNLP<Boolean>("mid-attr-mobilization", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_FORTIFY_BORDER = new AnnotationTypeNLP<Boolean>("mid-attr-fortify-border", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_BORDER_VIOLATION = new AnnotationTypeNLP<Boolean>("mid-attr-border-violation", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_USE_OF_FORCE = new AnnotationTypeNLP<Boolean>("mid-attr-use-of-force", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_BLOCKADE = new AnnotationTypeNLP<Boolean>("mid-attr-blockade", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_OCCUPATION_OF_TERRITORY = new AnnotationTypeNLP<Boolean>("mid-attr-occupation-of-territory", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_SEIZURE = new AnnotationTypeNLP<Boolean>("mid-attr-seizure", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_ATTACK = new AnnotationTypeNLP<Boolean>("mid-attr-attack", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_CLASH = new AnnotationTypeNLP<Boolean>("mid-attr-clash", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_DECLARATION_OF_WAR = new AnnotationTypeNLP<Boolean>("mid-attr-declaration-of-war", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_USE_OF_CBR_WEAPONS = new AnnotationTypeNLP<Boolean>("mid-attr-use-of-cbr-weapons", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_WAR = new AnnotationTypeNLP<Boolean>("mid-attr-war", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_BEGIN_INTERSTATE_WAR = new AnnotationTypeNLP<Boolean>("mid-attr-begin-interstate-war", Boolean.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Boolean> MID_ATTRIBUTE_JOIN_INTERSTATE_WAR = new AnnotationTypeNLP<Boolean>("mid-attr-join-interstate-war", Boolean.class, Target.DOCUMENT);
}