package edu.psu.ist.acs.micro.mid.scratch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.joda.time.LocalDate;
import org.joda.time.Partial;
import org.joda.time.YearMonth;

import edu.cmu.ml.rtw.generic.data.Serializer;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.SerializerDocumentNLPBSON;
import edu.cmu.ml.rtw.generic.data.store.Storage;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.data.store.StoredCollection;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorDocument;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPExtendable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.cat.data.CatDataTools;
import edu.cmu.ml.rtw.micro.cat.data.annotation.CategoryList;
import edu.cmu.ml.rtw.micro.cat.data.annotation.nlp.NELLMentionCategorizer;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.data.annotation.COWCountry;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDDispute;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDDispute.Outcome;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDDispute.Settlement;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident.Action;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident.FatalityLevel;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident.HostilityLevel;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident.Participant;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident.RevisionType;
import edu.psu.ist.acs.micro.mid.data.annotation.nlp.AnnotationTypeNLPMID;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

public class ConstructCOWData {
	private static Storage<?, Document> storage;
	private static MIDProperties properties;
	private static MIDDataTools dataTools;
	
	/**
	 * @param args
	 * 	0 => Input text document containing older narratives from PDF
	 * 	1 => Input text document containing newer narratives from PDF
	 * 	2 => Input MID incident participant CSV file
	 *  3 => Input MID incident CSV file
	 *  4 => Input MID CSV file
	 *  5 => Only disputes?
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		dataTools = new MIDDataTools();
		properties = new MIDProperties();
		boolean onlyDisputes = Boolean.valueOf(args[5]);
		
		List<AnnotationType<?>> annotationTypes = new ArrayList<AnnotationType<?>>();
		annotationTypes.addAll(dataTools.getAnnotationTypesNLP());
		annotationTypes.remove(AnnotationTypeNLP.SENTENCE);
		storage = properties.getStorage(new MIDDataTools(), annotationTypes);
		
		Map<Integer, List<MIDIncident.Participant>> incidentParticipants = parseIncidentParticipants(args[2]);
		Map<Integer, List<MIDIncident>> incidents = parseIncidents(args[3], incidentParticipants);
		Map<Integer, MIDDispute> disputes = parseDisputes(args[4], incidents);
		
		if (!onlyDisputes) {
			Map<Integer, Pair<Integer, String>> narratives = parseNarrativesOldFormat(args[0]);
			narratives.putAll(parseNarrativesNewFormat(args[1]));
			outputNarratives(narratives, disputes);
		}
		
		
		outputDisputes(disputes);
	}
	
	/**
	 * @param path to narrative text file
	 * @return map from dispute number (version 3) to dispute number (version 4) and narrative text. (Note
	 * dispute number (version 4) is always null, but is returned as a place holder so that the narrative map
	 * can be combined with the new format narrative map.
	 */
	private static Map<Integer, Pair<Integer, String>> parseNarrativesOldFormat(String path) {
		System.out.println("Parsing narratives (old format)...");
		Map<Integer, Pair<Integer, String>> narratives = new HashMap<Integer, Pair<Integer, String>>();
		String allNarratives = FileUtil.readFile(path);
		allNarratives.replaceAll("\nMID Number Story" , "");
		
		Matcher m = Pattern.compile("(3|4)[0-9][0-9][0-9] ").matcher(allNarratives);
		while (m.find()) {
			int dispNum3Start = m.start();
			int narrativeTextStart = m.start() + 5;
			Integer dispNum3 = Integer.valueOf(allNarratives.substring(dispNum3Start, dispNum3Start + 4));
			if (m.find(m.start()+1)) {
				narratives.put(dispNum3, new Pair<Integer, String>(null, allNarratives.substring(narrativeTextStart, m.start())));
			} else {
				narratives.put(dispNum3, new Pair<Integer, String>(null, allNarratives.substring(narrativeTextStart)));
			}
			
			m.find(dispNum3Start); // Reset matcher back to current match
		}
		
		
		return narratives;
	}
	
	/** 
	 * @param path to narrative text file
	 * @return map from dispute number (version 3) to dispute number (version 4) and narrative text
	 * @throws IOException 
	 */
	private static Map<Integer, Pair<Integer, String>> parseNarrativesNewFormat(String path) throws IOException {
		System.out.println("Parsing narratives (new format)...");
		
		Map<Integer, Pair<Integer, String>> narratives = new HashMap<Integer, Pair<Integer, String>>();
		BufferedReader r = FileUtil.getFileReader(path);
		String dispNum3Str = null;
		while ((dispNum3Str = r.readLine()) != null) {
			Integer dispNum3 = Integer.valueOf(dispNum3Str);
			Integer dispNum4 = Integer.valueOf(r.readLine());
			String narrative = r.readLine();
			narratives.put(dispNum3, new Pair<Integer, String>(dispNum4, narrative));
		}

		r.close();
	
		return narratives;
	}
	
	private static Map<Integer, List<MIDIncident.Participant>> parseIncidentParticipants(String path) {
		List<Map<String, String>> incidentParticpantMaps = FileUtil.readSVFile(path, ",");
		Map<Integer, List<MIDIncident.Participant>> incidentParticipants = new HashMap<Integer, List<MIDIncident.Participant>>();
		for (Map<String, String> map : incidentParticpantMaps) {
			int incidNum3 = Integer.valueOf(map.get("IncidNum3"));
			String country = COWCountry.valueOf(map.get("StAbb")).getName();
			
			int stDay = Integer.valueOf(map.get("StDay"));
			int stMon = Integer.valueOf(map.get("StMon"));
			int stYear = Integer.valueOf(map.get("StYear"));
			Partial startDate = (stDay < 0) ? new Partial(new YearMonth(stYear, stMon)) 
											: new Partial(new LocalDate(stYear, stMon, stDay));
			
			int endDay = Integer.valueOf(map.get("EndDay"));
			int endMon = Integer.valueOf(map.get("EndMon"));
			int endYear = Integer.valueOf(map.get("EndYear"));
			Partial endDate = (endDay < 0) ? new Partial(new YearMonth(endYear, endMon)) 
										  : new Partial(new LocalDate(endYear, endMon, endDay));
		
			boolean inSideA = Boolean.valueOf(map.get("InSide A"));
			boolean sideA = Boolean.valueOf(map.get("SideA"));
			FatalityLevel fatalityLevel = FatalityLevel.valueOf(Integer.valueOf(map.get("Fatality")));
			Integer fatalities = (Integer.valueOf(map.get("FatalPre")) < 0) ? null : Integer.valueOf(map.get("FatalPre"));
			Action action = Action.valueOf(Integer.valueOf(map.get("Action")));
			HostilityLevel hostilityLevel = HostilityLevel.valueOf(Integer.valueOf(map.get("HostLev")));
			RevisionType revType1 = RevisionType.valueOf(Integer.valueOf(map.get("RevType1")));
			RevisionType revType2 = RevisionType.valueOf(Integer.valueOf(map.get("RevType2")));
			String version = map.get("Version");
			
			if (!incidentParticipants.containsKey(incidNum3)) {
				incidentParticipants.put(incidNum3, new ArrayList<Participant>());
			}
			
			incidentParticipants.get(incidNum3).add(new MIDIncident.Participant(null, 
																				country, 
																				startDate, 
																				endDate, 
																				inSideA, 
																				sideA, 
																				fatalityLevel, 
																				fatalities, 
																				action, 
																				hostilityLevel, 
																				revType1, 
																				revType2, 
																				version));
		}
		
		return incidentParticipants;
	}
	
	private static Map<Integer, List<MIDIncident>> parseIncidents(String path, Map<Integer, List<MIDIncident.Participant>> incidentParticipants) {
		List<Map<String, String>> incidentMaps = FileUtil.readSVFile(path, ",");
		Map<Integer, List<MIDIncident>> incidents = new HashMap<Integer, List<MIDIncident>>();
		for (Map<String, String> map : incidentMaps) {
			int dispNum3 = Integer.valueOf(map.get("DispNum3"));
			int incidNum3 = Integer.valueOf(map.get("IncidNum3"));
			Integer incidNum4 = (Integer.valueOf(map.get("IncidNum4")) < 0) ? null : Integer.valueOf(map.get("IncidNum4"));

			int stDay = Integer.valueOf(map.get("StDay"));
			int stMon = Integer.valueOf(map.get("StMon"));
			int stYear = Integer.valueOf(map.get("StYear"));
			Partial startDate = (stDay < 0) ? new Partial(new YearMonth(stYear, stMon)) 
											: new Partial(new LocalDate(stYear, stMon, stDay));
			
			int endDay = Integer.valueOf(map.get("EndDay"));
			int endMon = Integer.valueOf(map.get("EndMon"));
			int endYear = Integer.valueOf(map.get("EndYear"));
			Partial endDate = (endDay < 0) ? new Partial(new YearMonth(endYear, endMon)) 
										  : new Partial(new LocalDate(endYear, endMon, endDay));
		
			Integer duration = (Integer.valueOf(map.get("Duration")) < 0) ? null : Integer.valueOf(map.get("Duration"));
			Integer TBI = (Integer.valueOf(map.get("TBI")) < 0) ? null : Integer.valueOf(map.get("TBI"));
			FatalityLevel fatalityLevel = FatalityLevel.valueOf(Integer.valueOf(map.get("Fatality")));
			Integer fatalities = (Integer.valueOf(map.get("FatalPre")) < 0) ? null : Integer.valueOf(map.get("FatalPre"));
			Action action = Action.valueOf(Integer.valueOf(map.get("Action")));
			HostilityLevel hostilityLevel = HostilityLevel.valueOf(Integer.valueOf(map.get("HostLev")));
			Integer numA = (Integer.valueOf(map.get("NumA")) < 0) ? null : Integer.valueOf(map.get("NumA"));
			RevisionType revType1 = RevisionType.valueOf(Integer.valueOf(map.get("RevType1")));
			RevisionType revType2 = RevisionType.valueOf(Integer.valueOf(map.get("RevType2")));
			String version = map.get("Version");
	
			if (!incidents.containsKey(dispNum3)) {
				incidents.put(dispNum3, new ArrayList<MIDIncident>());
			}
			
			incidents.get(dispNum3).add(new MIDIncident(null,
														 incidNum3,
														 incidNum4,
														 startDate,
														 endDate,
														 duration,
														 TBI,
														 fatalityLevel,
														 fatalities,
														 action,
														 hostilityLevel,
														 numA,
														 revType1,
														 revType2,
														 version,
														 (incidentParticipants.containsKey(incidNum3))? incidentParticipants.get(incidNum3) : new ArrayList<MIDIncident.Participant>()));
		}
		
		return incidents;
	}
	
	private static Map<Integer, MIDDispute> parseDisputes(String path, Map<Integer, List<MIDIncident>> incidents) {
		List<Map<String, String>> disputeMaps = FileUtil.readSVFile(path, ",");
		Map<Integer, MIDDispute> disputes = new HashMap<>();
		for (Map<String, String> map : disputeMaps) {
			String id = map.get("DispNum3");
			int dispNum3 = Integer.valueOf(map.get("DispNum3"));
			Integer dispNum4 = (Integer.valueOf(map.get("DispNum4")) < 0) ? null : Integer.valueOf(map.get("DispNum4"));
			int stDay = Integer.valueOf(map.get("StDay"));
			int stMon = Integer.valueOf(map.get("StMon"));
			int stYear = Integer.valueOf(map.get("StYear"));
			Partial startDate = (stDay < 0) ? new Partial(new YearMonth(stYear, stMon)) 
											: new Partial(new LocalDate(stYear, stMon, stDay));
			
			int endDay = Integer.valueOf(map.get("EndDay"));
			int endMon = Integer.valueOf(map.get("EndMon"));
			int endYear = Integer.valueOf(map.get("EndYear"));
			Partial endDate = (endDay < 0) ? new Partial(new YearMonth(endYear, endMon)) 
										  : new Partial(new LocalDate(endYear, endMon, endDay));
		
			Outcome outcome = Outcome.valueOf(Integer.valueOf(map.get("Outcome")));
			Settlement settlement = Settlement.valueOf(Integer.valueOf(map.get("Settle")));
			FatalityLevel fatalityLevel = FatalityLevel.valueOf(Integer.valueOf(map.get("Fatality")));
			Integer fatalities = (Integer.valueOf(map.get("FatalPre")) < 0) ? null : Integer.valueOf(map.get("FatalPre"));
			Integer minDuration = (Integer.valueOf(map.get("MinDur")) < 0) ? null : Integer.valueOf(map.get("MinDur"));
			Integer maxDuration = (Integer.valueOf(map.get("MaxDur")) < 0) ? null : Integer.valueOf(map.get("MaxDur"));
			Action maxAction = Action.valueOf(Integer.valueOf(map.get("HiAct")));
			HostilityLevel maxHostilityLevel = HostilityLevel.valueOf(Integer.valueOf(map.get("HostLev")));
			boolean reciprocated = Boolean.valueOf(map.get("Recip"));
			Integer numA = (Integer.valueOf(map.get("NumA")) < 0) ? null : Integer.valueOf(map.get("NumA"));
			Integer numB = (Integer.valueOf(map.get("NumB")) < 0) ? null : Integer.valueOf(map.get("NumB"));
			boolean ongoing2010 = Boolean.valueOf(map.get("Ongo2010"));
			String version = map.get("Version");
		
			disputes.put(dispNum3, 
					new MIDDispute(new StoreReference(storage.getName(), properties.getMID4CollectionName(), "id", id),
										id,
									    dispNum3,
										dispNum4,
										startDate,
										endDate,
										outcome,
										settlement,
										fatalityLevel,
										fatalities,
										minDuration,
										maxDuration,
										maxAction,
										maxHostilityLevel,
										reciprocated,
										numA,
										numB,
										ongoing2010,		
										version,
										(incidents.containsKey(dispNum3)) ? incidents.get(dispNum3) : new ArrayList<MIDIncident>()));
		}
		
		return disputes;
	}
	
	@SuppressWarnings("unchecked")
	private static void outputNarratives(Map<Integer, Pair<Integer, String>> narratives, Map<Integer, MIDDispute> disputes) {
		PipelineNLPStanford pipelineStanford = new PipelineNLPStanford();
		
		NELLMentionCategorizer mentionCategorizer = new NELLMentionCategorizer(
				new CategoryList(CategoryList.Type.ALL_NELL_CATEGORIES, new CatDataTools()), 
				NELLMentionCategorizer.DEFAULT_MENTION_MODEL_THRESHOLD, NELLMentionCategorizer.DEFAULT_LABEL_TYPE, 
				1);
		PipelineNLPExtendable pipelineMicroCat = new PipelineNLPExtendable();
		pipelineMicroCat.extend(mentionCategorizer);
		
		PipelineNLP basePipeline = pipelineStanford.weld(pipelineMicroCat);
		
		if (storage.hasCollection(properties.getMID4NarrativeDocumentCollectionName())) {
			storage.deleteCollection(properties.getMID4NarrativeDocumentCollectionName());
		}
		
		StoredCollection<DocumentNLPMutable, Document> documents = (StoredCollection<DocumentNLPMutable, Document>)storage.createCollection(properties.getMID4NarrativeDocumentCollectionName(), new SerializerDocumentNLPBSON(dataTools));
		
		for (Entry<Integer, Pair<Integer, String>> entry : narratives.entrySet()) {
			int dispNum3 = entry.getKey();
			final MIDDispute dispute = disputes.get(dispNum3);
			String narrativeText = entry.getValue().getSecond();
		
			System.out.println("Constructing narrative document " + dispNum3 + "...");
			
			PipelineNLPExtendable metaDataPipeline = new PipelineNLPExtendable();
			metaDataPipeline.extend(new AnnotatorDocument<MIDDispute>() {
				public String getName() { return "MID4.01"; }
				public boolean measuresConfidence() { return false; }
				public AnnotationType<MIDDispute> produces() { return AnnotationTypeNLPMID.MID_DISPUTE; }
				public AnnotationType<?>[] requires() { return new AnnotationType<?>[0]; }
				public Pair<MIDDispute, Double> annotate(DocumentNLP document) {
					return new Pair<MIDDispute, Double>(dispute, null);
				}
			});
			
			PipelineNLP pipeline = basePipeline.weld(metaDataPipeline);
			DocumentNLPMutable document = new DocumentNLPInMemory(dataTools, String.valueOf(dispNum3), narrativeText);
			pipeline.run(document);
			documents.addItem(document);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void outputDisputes(Map<Integer, MIDDispute> disputes) throws IOException {
		System.out.println("Outputting disputes...");
		
		if (storage.hasCollection(properties.getMID4CollectionName())) {
			storage.deleteCollection(properties.getMID4CollectionName());
		}
		
		StoredCollection<MIDDispute, Document> mid4Collection = (StoredCollection<MIDDispute, Document>)storage.createCollection(properties.getMID4CollectionName(), (Serializer<MIDDispute, Document>)dataTools.getSerializers().get("JSONBSONMIDDispute"));

		for (MIDDispute dispute : disputes.values()) {
			mid4Collection.addItem(dispute);
		}
	}
}
