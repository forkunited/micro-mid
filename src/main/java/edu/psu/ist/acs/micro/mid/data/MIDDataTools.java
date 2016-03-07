package edu.psu.ist.acs.micro.mid.data;

import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.psu.ist.acs.micro.mid.data.annotation.DataSetBuilderMIDRelevance;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

/**
 * MIDDataTools contains definitions of cleaning 
 * functions, gazetteers, and other tools used by
 * features in the event models.
 * 
 * @author Bill McDowell
 *
 */
public class MIDDataTools extends DataTools {
	private MIDProperties properties;
	
	public MIDDataTools() {
		this(new MIDProperties());
		
	}
	
	public MIDDataTools(MIDProperties properties) {
		this(new OutputWriter(), properties);
		
	}
	
	public MIDDataTools(OutputWriter outputWriter, MIDDataTools dataTools) {
		this(outputWriter, dataTools.properties);
		
		this.timer = dataTools.timer;
		
		for (Entry<String, Gazetteer> entry : dataTools.gazetteers.entrySet())
			this.gazetteers.put(entry.getKey(), entry.getValue());
	}
	
	@SuppressWarnings("unchecked")
	public MIDDataTools(OutputWriter outputWriter, MIDProperties properties) {
		super(outputWriter);
		
		this.properties = properties;
		
		// FIXME Make clean fns for this project?
		DataTools catDataTools = new CatDataTools();
		this.addCleanFn(catDataTools.getCleanFn("CatBagOfWordsFeatureCleanFn"));
		
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.ARTICLE_PUBLICATION_DATE);
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.ARTICLE_SOURCE);
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.ARTICLE_TITLE);
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.ARTICLE_BYLINE);
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.ARTICLE_DATELINE);
		
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.MID_SVM_RELEVANCE_SCORE);
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.MID_SVM_RELEVANCE_CLASS);
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.MID_GOLD_RELEVANCE_CLASS);
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.MID_GOLD_TERNARY_RELEVANCE_CLASS);
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.MID_CLASSIFIER_RELEVANCE_CLASS);
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.MID_CLASSIFIER_RELEVANCE_SCORE);
		
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.MID_DISPUTE_NUMBER_3);
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.MID_DISPUTE_NUMBER_4);	
		
		((DatumContext<DocumentNLPDatum<Boolean>, Boolean>)this.genericContexts.get("DocumentNLPBoolean"))
		.getDatumTools().addGenericDataSetBuilder(new DataSetBuilderMIDRelevance());
	}
	
	public MIDProperties getProperties() {
		return this.properties;
	}
	
	@Override
	public DataTools makeInstance() {
		return new MIDDataTools(this.outputWriter, this);
	}
}
