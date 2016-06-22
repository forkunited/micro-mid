package edu.psu.ist.acs.micro.mid.data.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;

/**
 * DataSetBuilderMIDRelevance takes a collection of documents, and constructs 
 * a data set in which each datum consists of a 
 * document mapped to a label indicating whether or not the document is MID
 * relevant.
 * 
 * A document is MID relevant if it has a gold-standard MID relevance annotation
 * (i.e. a human annotator determined that it was relevant).
 * 
 * @author Bill McDowell
 *
 */
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
		
		if (this.limit > 0)
			documentNames = documentNames.subList(0, this.limit);
		
		Map<String, DocumentNLP> documentMap = new TreeMap<String, DocumentNLP>();
		ThreadMapper<String, Boolean> mapper = new ThreadMapper<String, Boolean>(new Fn<String, Boolean>() {
			@Override
			public Boolean apply(String documentName) {
				DocumentNLP document = documentSet.getDocumentByName(documentName, false);
				
				synchronized (documentMap) {
					documentMap.put(documentName, document);
					if (documentMap.size() % 10000 == 0)
						context.getDataTools().getOutputWriter().debugWriteln("MIDRelevance loaded " + documentMap.size() + " documents...");
				}
				
				return true;
			} 
		});
		mapper.run(documentNames, this.context.getMaxThreads());
		
		for (String documentName : documentNames) {
			boolean documentLabel = false;
			DocumentNLP document = documentMap.get(documentName);
			
			if (DataSetBuilderMIDRelevance.this.label) {
				if (document.hasAnnotationType(AnnotationTypeNLPMID.MID_GOLD_RELEVANCE_CLASS)
						&& document.getDocumentAnnotation(AnnotationTypeNLPMID.MID_GOLD_RELEVANCE_CLASS))
					documentLabel = true;
			}
			
			if (DataSetBuilderMIDRelevance.this.label == documentLabel) {
				data.add(new DocumentNLPDatum<Boolean>(this.context.getDataTools().getIncrementId(), document, documentLabel));
			}
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
