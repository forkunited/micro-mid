package edu.psu.ist.acs.micro.mid.data.annotation.nlp;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDDispute;

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


}