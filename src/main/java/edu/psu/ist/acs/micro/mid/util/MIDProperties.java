package edu.psu.ist.acs.micro.mid.util;

import java.util.Collection;

import org.bson.Document;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.store.Storage;
import edu.cmu.ml.rtw.generic.data.store.StorageFileSystem;
import edu.cmu.ml.rtw.generic.data.store.StorageMongo;
import edu.cmu.ml.rtw.generic.util.Properties;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;

/**
 * MIDProperties loads and represents a properties
 * configuration file (for specifying file paths and
 * other system dependent information)
 * 
 * @author Bill McDowell
 *
 */
public class MIDProperties extends Properties {
	private String contextInputDirPath;
	private String experimentOutputDirPath;
	
	private String storageMongoMicroEventDatabaseName;
	private String storageFileSystemMicroEventDirPath;
	private boolean useMongoStorage;
	
	private String midNewsSvmUnlabeledDocumentCollectionName;
	private String midNewsSvmRelevanceLabeledDocumentCollectionName;
	private String midNewsGoldRelevanceLabeledDocumentCollectionName;
	
	private String mid4CollectionName;
	private String mid4NarrativeDocumentCollectionName;
	
	public MIDProperties() {
		this(null);
	}
	
	public MIDProperties(String path) {
		super( new String[] { (path == null) ? "mid.properties" : path } );
		
		this.contextInputDirPath = loadProperty("contextInputDirPath");
		this.experimentOutputDirPath = loadProperty("experimentOutputDirPath");
		this.storageFileSystemMicroEventDirPath = loadProperty("storageFileSystemMicroEventDirPath");
		this.storageMongoMicroEventDatabaseName = loadProperty("storageMongoMicroEventDatabaseName");
		this.useMongoStorage = Boolean.valueOf(loadProperty("useMongoStorage"));
		this.midNewsSvmUnlabeledDocumentCollectionName = loadProperty("midNewsSvmUnlabeledDocumentCollectionName");
		this.midNewsSvmRelevanceLabeledDocumentCollectionName = loadProperty("midNewsSvmRelevanceLabeledDocumentCollectionName");
		this.midNewsGoldRelevanceLabeledDocumentCollectionName = loadProperty("midNewsGoldRelevanceLabeledDocumentCollectionName");
		this.mid4CollectionName = loadProperty("mid4CollectionName");
		this.mid4NarrativeDocumentCollectionName = loadProperty("mid4NarrativeDocumentCollectionName");
	}
	
	public String getContextInputDirPath() {
		return this.contextInputDirPath;
	}
	
	public String getExperimentOutputDirPath() {
		return this.experimentOutputDirPath;
	}
	
	public Storage<?,Document> getStorage(MIDDataTools dataTools, Collection<AnnotationType<?>> annotationTypes) {
		if (this.useMongoStorage) {
			return new StorageMongo("", "localhost", this.storageMongoMicroEventDatabaseName, dataTools.getSerializers());
		} else {
			return new StorageFileSystem<Document>("", this.storageFileSystemMicroEventDirPath, dataTools.getSerializers());
		}
	}
	
	public String getMIDNewsSvmUnlabeledDocumentCollectionName() {
		return this.midNewsSvmUnlabeledDocumentCollectionName;
	}
	
	public String getMIDNewsSvmRelevanceLabeledDocumentCollectionName() {
		return this.midNewsSvmRelevanceLabeledDocumentCollectionName;
	}
	
	public String getMIDNewsGoldRelevanceLabeledDocumentCollectionName() {
		return this.midNewsGoldRelevanceLabeledDocumentCollectionName;
	}
	
	public String getMID4CollectionName() {
		return this.mid4CollectionName;
	}
	
	public String getMID4NarrativeDocumentCollectionName() {
		return this.mid4NarrativeDocumentCollectionName;
	}
}