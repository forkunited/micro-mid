package edu.psu.ist.acs.micro.mid.data.annotation;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DataSetBuilder;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPDatum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.MathUtil;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;

public class DataSetBuilderMIDRelevance extends DataSetBuilder<DocumentNLPDatum<Boolean>, Boolean> {
	private String storage;
	private String collection;
	private boolean label;
	private int limit = -1;
	private String[] parameterNames = { "storage", "collection", "label", "limit" };
	
	public DataSetBuilderMIDRelevance() {
		this(null);
	}
	
	public DataSetBuilderMIDRelevance(DatumContext<DocumentNLPDatum<Boolean>, Boolean> context) {
		this.context = context;
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
		else if (parameter.equals("limit"))
			return Obj.stringValue(String.valueOf(this.limit));
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
		else if (parameter.equals("limit"))
			this.limit = Integer.valueOf(this.context.getMatchValue(parameterValue));
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
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> documentSet = 
			new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(
				this.context.getDataTools().getStoredItemSetManager().getItemSet(this.storage, this.collection));
		
		DataSet<DocumentNLPDatum<Boolean>, Boolean> data = new DataSet<DocumentNLPDatum<Boolean>, Boolean>(this.context.getDatumTools());
		
		List<String> documentNames = new ArrayList<>(documentSet.getDocumentNames());
		documentNames = MathUtil.randomPermutation(this.context.getDataTools().getGlobalRandom(), documentNames);
		
		int count = 0;
		for (String documentName : documentNames) {
			if (this.limit > 0 && count >= this.limit)
				break;
			if (count % 10000 == 1)
				this.context.getDataTools().getOutputWriter().debugWriteln("MIDRelevance loaded " + count + " documents...");
			
			DocumentNLP document = documentSet.getDocumentByName(documentName, false);
			boolean documentLabel = false;
			
			if (DataSetBuilderMIDRelevance.this.label) {
				if (document.hasAnnotationType(AnnotationTypeNLPMID.MID_GOLD_RELEVANCE_CLASS)
						&& document.getDocumentAnnotation(AnnotationTypeNLPMID.MID_GOLD_RELEVANCE_CLASS))
					documentLabel = true;
			}
			
			if (DataSetBuilderMIDRelevance.this.label == documentLabel) {
				data.add(new DocumentNLPDatum<Boolean>(this.context.getDataTools().getIncrementId(), document, documentLabel));
			}
			
			count++;
		}
		
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
