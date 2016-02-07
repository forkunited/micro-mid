package edu.psu.ist.acs.micro.mid.data.annotation.nlp;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;

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
	
	public static final AnnotationTypeNLP<TernaryRelevanceClass> MID_GOLD_TERNARY_RELEVANCE_CLASS = new AnnotationTypeNLP<TernaryRelevanceClass>("mid-gold-ternary-rel-class", TernaryRelevanceClass.class, Target.DOCUMENT);
	
	public static final AnnotationTypeNLP<Integer> MID_DISPUTE_NUMBER_3 = new AnnotationTypeNLP<Integer>("mid-disp-num-3", Integer.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<Integer> MID_DISPUTE_NUMBER_4 = new AnnotationTypeNLP<Integer>("mid-disp-num-4", Integer.class, Target.DOCUMENT);
	
	public static final AnnotationTypeNLP<String> ARTICLE_TITLE = new AnnotationTypeNLP<String>("article-title", String.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_PUBLICATION_DATE = new AnnotationTypeNLP<String>("article-pub-date", String.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_SOURCE = new AnnotationTypeNLP<String>("article-source", String.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_BYLINE = new AnnotationTypeNLP<String>("article-byline", String.class, Target.DOCUMENT);
	public static final AnnotationTypeNLP<String> ARTICLE_DATELINE = new AnnotationTypeNLP<String>("article-dateline", String.class, Target.DOCUMENT);


}