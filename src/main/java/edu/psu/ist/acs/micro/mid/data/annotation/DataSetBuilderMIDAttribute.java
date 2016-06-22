package edu.psu.ist.acs.micro.mid.data.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
 * DataSetBuilderMIDAttribute takes a collection of documents and a MID 
 * attribute, and constructs a data set in which each datum consists of a 
 * document mapped to a label indicating whether or not the document has the
 * attribute.  Currently, valid attributes consist of all HostilityLevel and
 * Action values from the enums defined in 
 * edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident.  So for example, one
 * attribute indicates whether a given document describes a "DISPLAY_OF_FORCE"
 * attribute from the Action enum.  
 * 
 * A document is determined to have a MID attribute if
 * it is annotated with a edu.psu.ist.acs.micro.mid.data.annotation.MIDDispute that
 * has the attribute as a "max action" or "max hostility level", 
 * or if the MIDDispute contains a MIDIncident with the attribute 
 * (see the documentHasAttribute method below).
 * 
 * @author Bill McDowell
 *
 */
public class DataSetBuilderMIDAttribute extends DataSetBuilder<DocumentNLPDatum<Boolean>, Boolean> {
	private String storage;
	private String collection;
	private String attribute;
	private String[] parameterNames = { "storage", "collection", "attribute" };
	
	public DataSetBuilderMIDAttribute() {
		this(null);
	}
	
	public DataSetBuilderMIDAttribute(DatumContext<DocumentNLPDatum<Boolean>, Boolean> context) {
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
		else if (parameter.equals("attribute"))
			return Obj.stringValue(String.valueOf(this.attribute));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("storage"))
			this.storage = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("collection"))
			this.collection = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("attribute"))
			this.attribute = this.context.getMatchValue(parameterValue);
		else
			return false;
		return true;
	}


	@Override
	public DataSetBuilder<DocumentNLPDatum<Boolean>, Boolean> makeInstance(
			DatumContext<DocumentNLPDatum<Boolean>, Boolean> context) {
		return new DataSetBuilderMIDAttribute(context);
	}

	@Override
	public DataSet<DocumentNLPDatum<Boolean>, Boolean> build() {
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> documentSet = 
			new DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>(
				this.context.getDataTools().getStoredItemSetManager().getItemSet(this.storage, this.collection));
		
		DataSet<DocumentNLPDatum<Boolean>, Boolean> data = new DataSet<DocumentNLPDatum<Boolean>, Boolean>(this.context.getDatumTools());
		
		List<String> documentNames = new ArrayList<>(documentSet.getDocumentNames());
		documentNames = MathUtil.randomPermutation(this.context.getDataTools().getGlobalRandom(), documentNames);
		
		Map<String, DocumentNLP> documentMap = new TreeMap<String, DocumentNLP>();
		ThreadMapper<String, Boolean> mapper = new ThreadMapper<String, Boolean>(new Fn<String, Boolean>() {
			@Override
			public Boolean apply(String documentName) {
				DocumentNLP document = documentSet.getDocumentByName(documentName, false);
				
				synchronized (documentMap) {
					documentMap.put(documentName, document);
				}
				
				return true;
			} 
		});
		mapper.run(documentNames, this.context.getMaxThreads());
		
		for (String documentName : documentNames) {
			DocumentNLP document = documentMap.get(documentName);
			boolean documentLabel = documentHasAttribute(document);
			
			data.add(new DocumentNLPDatum<Boolean>(this.context.getDataTools().getIncrementId(), 
					document, 
					documentLabel));
		}
		
		return data;
	}
	
	private boolean documentHasAttribute(DocumentNLP document) {
		MIDDispute dispute = document.getDocumentAnnotation(AnnotationTypeNLPMID.MID_DISPUTE);
		Set<String> disputeAttrs = new TreeSet<String>();
		
		disputeAttrs.add(dispute.getMaxAction().toString());
		for (MIDIncident incident : dispute.getIncidents())
			disputeAttrs.add(incident.getAction().toString());
		disputeAttrs.add(dispute.getMaxHostilityLevel().toString());
		for (MIDIncident incident : dispute.getIncidents())
			disputeAttrs.add(incident.getHostilityLevel().toString());

		return disputeAttrs.contains(this.attribute);
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
		return "MIDAttribute";
	}
}
