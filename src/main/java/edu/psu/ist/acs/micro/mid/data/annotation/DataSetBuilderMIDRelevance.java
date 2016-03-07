package edu.psu.ist.acs.micro.mid.data.annotation;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DataSetBuilder;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;

public class DataSetBuilderMIDRelevance extends DataSetBuilder<DocumentNLPDatum<Boolean>, Boolean> {
	private String storage;
	private String collection;
	private boolean label;
	private String[] parameterNames = { "storage", "collection", "label" };
	
	private static Integer datumId = 1;
	
	
	public DataSetBuilderMIDRelevance() {
		this(null);
	}
	
	public DataSetBuilderMIDRelevance(DatumContext<DocumentNLPDatum<Boolean>, Boolean> context) {
		this.context = context;
	}
	
	private int getNextDatumId() {
		int nextDatumId = 0;
		synchronized (datumId) {
			nextDatumId = datumId;
			datumId++;
		}
		return nextDatumId;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("storage"))
			return Obj.stringValue(this.storage);
		else if (parameter.equals("collection"))
			return Obj.stringValue(this.collection);
		else if (parameter.equals("label"))
			return Obj.stringValue(String.valueOf(this.label));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("storage"))
			this.storage = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("collection"))
			this.collection = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("label"))
			this.label = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}


	@Override
	public DataSetBuilder<DocumentNLPDatum<Boolean>, Boolean> makeInstance(
			DatumContext<DocumentNLPDatum<Boolean>, Boolean> context) {
		return new DataSetBuilderMIDRelevance(context);
	}

	@Override
	public DataSet<DocumentNLPDatum<Boolean>, Boolean> build() {
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> documentSet = new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(
				this.context.getDataTools().getStoredItemSetManager().getItemSet(this.storage, this.collection));
		
		DataSet<DocumentNLPDatum<Boolean>, Boolean> data = new DataSet<DocumentNLPDatum<Boolean>, Boolean>(this.context.getDatumTools());
		
		documentSet.map(new Fn<DocumentNLP, Boolean>() {
			@Override
			public Boolean apply(DocumentNLP document) {
				boolean documentLabel = false;
				
				if (DataSetBuilderMIDRelevance.this.label) {
					if (document.hasAnnotationType(AnnotationTypeNLPMID.MID_GOLD_RELEVANCE_CLASS)
							&& document.getDocumentAnnotation(AnnotationTypeNLPMID.MID_GOLD_RELEVANCE_CLASS))
						documentLabel = true;
				}
				
				if (DataSetBuilderMIDRelevance.this.label == documentLabel) {
					synchronized (data) {
						data.add(new DocumentNLPDatum<Boolean>(
							getNextDatumId(), document, documentLabel));
					}
				}
				
				return true;
			}
			
		}, this.context.getMaxThreads(), this.context.getDataTools().getGlobalRandom());
		
		return data;
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}

	@Override
	public String getGenericName() {
		return "MIDRelevance";
	}

}
