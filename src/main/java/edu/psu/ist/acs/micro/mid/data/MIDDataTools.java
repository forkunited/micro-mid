package edu.psu.ist.acs.micro.mid.data;

import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.Gazetteer;
import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.SerializerJSONBSON;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DatumIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.psu.ist.acs.micro.mid.data.annotation.DataSetBuilderMIDRelevance;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDDispute;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;
import edu.psu.ist.acs.micro.event.data.EventDataTools;

/**
 * MIDDataTools contains definitions of cleaning 
 * functions, gazetteers, and other tools used by
 * features in the event models.
 * 
 * @author Bill McDowell
 *
 */
public class MIDDataTools extends EventDataTools {
	public MIDDataTools() {
		this(new MIDProperties());
		
	}
	
	public MIDDataTools(MIDProperties properties) {
		this(new OutputWriter(), properties);
		
	}
	
	public MIDDataTools(OutputWriter outputWriter, MIDDataTools dataTools) {
		this(outputWriter, (MIDProperties)dataTools.properties);
		
		this.timer = dataTools.timer;
		
		for (Entry<String, Gazetteer> entry : dataTools.gazetteers.entrySet())
			this.gazetteers.put(entry.getKey(), entry.getValue());
	}
	
	@SuppressWarnings("unchecked")
	public MIDDataTools(OutputWriter outputWriter, MIDProperties properties) {
		super();
		
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
		
		this.addAnnotationTypeNLP(AnnotationTypeNLPMID.MID_DISPUTE);	
		
		((DatumContext<DocumentNLPDatum<Boolean>, Boolean>)this.genericContexts.get("DocumentNLPBoolean"))
		.getDatumTools().addGenericDataSetBuilder(new DataSetBuilderMIDRelevance());
		
		((DatumContext<DocumentNLPDatum<Boolean>, Boolean>)this.genericContexts.get("DocumentNLPBoolean"))
		.getDatumTools().addDatumIndicator(new DatumIndicator<DocumentNLPDatum<Boolean>>() {
			public String toString() { return "EarlyYears"; }
			public boolean indicator(DocumentNLPDatum<Boolean> datum) {
				return datum.getDocument().getName().contains("_2002_")
						|| datum.getDocument().getName().contains("_2003_")
						|| datum.getDocument().getName().contains("_2004_");
			}
		});
		
		((DatumContext<DocumentNLPDatum<Boolean>, Boolean>)this.genericContexts.get("DocumentNLPBoolean"))
		.getDatumTools().addDatumIndicator(new DatumIndicator<DocumentNLPDatum<Boolean>>() {
			public String toString() { return "LateYears"; }
			public boolean indicator(DocumentNLPDatum<Boolean> datum) {
				return datum.getDocument().getName().contains("_2008_")
						|| datum.getDocument().getName().contains("_2009_")
						|| datum.getDocument().getName().contains("_2010_");
			}
		});
		
		((DatumContext<DocumentNLPDatum<Boolean>, Boolean>)this.genericContexts.get("DocumentNLPBoolean"))
		.getDatumTools().addDatumIndicator(new DatumIndicator<DocumentNLPDatum<Boolean>>() {
			public String toString() { return "2005"; }
			public boolean indicator(DocumentNLPDatum<Boolean> datum) {
				return datum.getDocument().getName().contains("_2005_");
			}
		});
		
		((DatumContext<DocumentNLPDatum<Boolean>, Boolean>)this.genericContexts.get("DocumentNLPBoolean"))
		.getDatumTools().addDatumIndicator(new DatumIndicator<DocumentNLPDatum<Boolean>>() {
			public String toString() { return "2006"; }
			public boolean indicator(DocumentNLPDatum<Boolean> datum) {
				return datum.getDocument().getName().contains("_2006_");
			}
		});
		
		((DatumContext<DocumentNLPDatum<Boolean>, Boolean>)this.genericContexts.get("DocumentNLPBoolean"))
		.getDatumTools().addDatumIndicator(new DatumIndicator<DocumentNLPDatum<Boolean>>() {
			public String toString() { return "2007"; }
			public boolean indicator(DocumentNLPDatum<Boolean> datum) {
				return datum.getDocument().getName().contains("_2007_");
			}
		});
		
		((DatumContext<DocumentNLPDatum<Boolean>, Boolean>)this.genericContexts.get("DocumentNLPBoolean"))
		.getDatumTools().addDatumIndicator(new DatumIndicator<DocumentNLPDatum<Boolean>>() {
			public String toString() { return "2008"; }
			public boolean indicator(DocumentNLPDatum<Boolean> datum) {
				return datum.getDocument().getName().contains("_2008_");
			}
		});
		
		((DatumContext<DocumentNLPDatum<Boolean>, Boolean>)this.genericContexts.get("DocumentNLPBoolean"))
		.getDatumTools().addDatumIndicator(new DatumIndicator<DocumentNLPDatum<Boolean>>() {
			public String toString() { return "2009"; }
			public boolean indicator(DocumentNLPDatum<Boolean> datum) {
				return datum.getDocument().getName().contains("_2009_");
			}
		});
		
		((DatumContext<DocumentNLPDatum<Boolean>, Boolean>)this.genericContexts.get("DocumentNLPBoolean"))
		.getDatumTools().addDatumIndicator(new DatumIndicator<DocumentNLPDatum<Boolean>>() {
			public String toString() { return "2010"; }
			public boolean indicator(DocumentNLPDatum<Boolean> datum) {
				return datum.getDocument().getName().contains("_2010_");
			}
		});
	}
	
	public MIDProperties getProperties() {
		return (MIDProperties)this.properties;
	}
	
	@Override
	public DataTools makeInstance() {
		return new MIDDataTools(this.outputWriter, this);
	}
	
	@Override
	public Map<String, Serializer<?, ?>> getSerializers() {
		Map<String, Serializer<?, ?>> serializers = super.getSerializers();
		
		SerializerJSONBSON<MIDDispute> midDisputeSerializer = new SerializerJSONBSON<MIDDispute>("MIDDispute", new MIDDispute());
		
		serializers.put(midDisputeSerializer.getName(), midDisputeSerializer);
		
		return serializers;
	}
}
