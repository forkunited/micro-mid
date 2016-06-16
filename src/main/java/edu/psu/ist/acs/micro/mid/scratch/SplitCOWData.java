package edu.psu.ist.acs.micro.mid.scratch;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.StoredItemSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSetInMemoryLazy;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON;
import edu.cmu.ml.rtw.generic.data.store.Storage;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

public class SplitCOWData {
	private static Storage<?, Document> storage;
	private static MIDProperties properties;
	private static MIDDataTools dataTools;
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		dataTools = new MIDDataTools();
		properties = new MIDProperties();
		storage = (Storage<?, Document>)dataTools.getStoredItemSetManager().getStorage("MIDBson");	
		DocumentSetInMemoryLazy<DocumentNLP, DocumentNLPMutable> documents = new DocumentSetInMemoryLazy<>(dataTools.getStoredItemSetManager().getItemSet(storage.getName(), 
				properties.getMID4NarrativeDocumentCollectionName()));
		List<StoredItemSetInMemoryLazy<DocumentNLP, DocumentNLPMutable>> parts = documents.makePartition(new double[] {.8, .1, .1}, new Random(1));
		
		if (storage.hasCollection(properties.getMID4NarrativeDocumentCollectionName() + "_train"))
			storage.deleteCollection(properties.getMID4NarrativeDocumentCollectionName() + "_train");
		if (storage.hasCollection(properties.getMID4NarrativeDocumentCollectionName() + "_dev"))
			storage.deleteCollection(properties.getMID4NarrativeDocumentCollectionName() + "_dev");
		if (storage.hasCollection(properties.getMID4NarrativeDocumentCollectionName() + "_test"))
			storage.deleteCollection(properties.getMID4NarrativeDocumentCollectionName() + "_test");
		
		StoredCollection<DocumentNLPMutable, Document> trainDocuments = (StoredCollection<DocumentNLPMutable, Document>)storage.createCollection(properties.getMID4NarrativeDocumentCollectionName() + "_train", new SerializerDocumentNLPBSON(dataTools));
		StoredCollection<DocumentNLPMutable, Document> devDocuments = (StoredCollection<DocumentNLPMutable, Document>)storage.createCollection(properties.getMID4NarrativeDocumentCollectionName() + "_dev", new SerializerDocumentNLPBSON(dataTools));
		StoredCollection<DocumentNLPMutable, Document> testDocuments = (StoredCollection<DocumentNLPMutable, Document>)storage.createCollection(properties.getMID4NarrativeDocumentCollectionName() + "_test", new SerializerDocumentNLPBSON(dataTools));

		for (DocumentNLP document : parts.get(0))
			trainDocuments.addItem((DocumentNLPMutable)document);
		for (DocumentNLP document : parts.get(1))
			devDocuments.addItem((DocumentNLPMutable)document);
		for (DocumentNLP document : parts.get(2))
			testDocuments.addItem((DocumentNLPMutable)document);
	}
}
