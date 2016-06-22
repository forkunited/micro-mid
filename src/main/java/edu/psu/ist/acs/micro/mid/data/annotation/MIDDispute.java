package edu.psu.ist.acs.micro.mid.data.annotation;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.Partial;
import org.joda.time.YearMonth;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.Storable;
import edu.cmu.ml.rtw.generic.util.StoredJSONSerializable;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident.Action;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident.FatalityLevel;
import edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident.HostilityLevel;

/**
 * MIDDispute represents a MID dispute from the data at
 * http://www.correlatesofwar.org/data-sets/MIDs
 * 
 * MIDDispute consist of miltary disputes between at
 * least two states.  Each dispute can consist of several
 * incidents (edu.psu.ist.acs.micro.mid.data.annotation.MIDIncident)
 * 
 * @author Bill McDowell
 *
 */
public class MIDDispute implements StoredJSONSerializable {
	private static final int NULL_ID = -9;
	
	public static enum Outcome {
		VICTORY_FOR_SIDE_A(1),
		VICTORY_FOR_SIDE_B(2),
		YIELD_BY_SIDE_A(3),
		YIELD_BY_SIDE_B(4),
		STALEMATE(5),
		COMPROMISE(6),
		RELEASED(7),
		UNCLEAR(8),
		JOINS_ONGOING_WAR(9);
		
		private int id;
		
		Outcome(int id) {
			this.id = id;
		}
		
		public static Outcome valueOf(int id) {
			if (id == NULL_ID)
				return null;
			
			Outcome[] outcomes = Outcome.values();
			for (Outcome outcome : outcomes) {
				if (outcome.id == id)
					return outcome;
			}
			
			return null;
		}
	}
	
	public static enum Settlement {
		NEGOTIATED(1),
		IMPOSED(2),
		NONE(3),
		UNCLEAR(4);
	
		private int id;
		
		Settlement(int id) {
			this.id = id;
		}
		
		public static Settlement valueOf(int id) {
			if (id == NULL_ID)
				return null;
			
			Settlement[] settlements = Settlement.values();
			for (Settlement settlement: settlements) {
				if (settlement.id == id)
					return settlement;
			}
			
			return null;
		}
	}
	
	private String id;
	private StoreReference reference;
	private Integer dispNum3;
	private Integer dispNum4;
	private Partial startDate;
	private Partial endDate;
	private Outcome outcome;
	private Settlement settlement;
	private FatalityLevel fatalityLevel;
	private Integer fatalities;
	private Integer minDuration;
	private Integer maxDuration;
	private Action maxAction;
	private HostilityLevel maxHostilityLevel;
	private boolean reciprocated;
	private Integer numA;
	private Integer numB;
	private boolean ongoing2010;	
	private String version;
	
	private List<MIDIncident> incidents;

	public MIDDispute() {
		
	}
	
	public MIDDispute(StoreReference reference) {
		this.reference = reference;
	}
	
	public MIDDispute(StoreReference reference,
					  String id,
					  Integer dispNum3,
					  Integer dispNum4,
					  Partial startDate,
					  Partial endDate,
					  Outcome outcome,
					  Settlement settlement,
					  FatalityLevel fatalityLevel,
					  Integer fatalities,
					  Integer minDuration,
					  Integer maxDuration,
					  Action maxAction,
					  HostilityLevel maxHostilityLevel,
					  boolean reciprocated,
					  Integer numA,
					  Integer numB,
					  boolean ongoing2010,	
					  String version,
					  List<MIDIncident> incidents) {
		this.reference = reference;
		this.id = id;
		this.dispNum3 = dispNum3;
		this.dispNum4 = dispNum4;
		this.startDate = startDate;
		this.endDate = endDate;
		this.outcome = outcome;
		this.settlement = settlement;
		this.fatalityLevel = fatalityLevel;
		this.fatalities = fatalities;
		this.minDuration = minDuration;
		this.maxDuration = maxDuration;
		this.maxAction = maxAction;
		this.maxHostilityLevel = maxHostilityLevel;
		this.reciprocated = reciprocated;
		this.numA = numA;
		this.numB = numB;
		this.ongoing2010 = ongoing2010;	
		this.version = version;
		this.incidents = incidents;
	}
	
	public Integer getDispNum3() {
		return this.dispNum3;
	}
	
	public Integer getDispNum4() {
		return this.dispNum4;
	}
	
	public Partial getStartDate() {
		return this.startDate;
	}
	
	public Partial getEndDate() {
		return this.endDate;
	}
	
	public Outcome getOutcome() {
		return this.outcome;
	}
	
	public Settlement getSettlement() {
		return this.settlement;
	}
	
	public FatalityLevel getFatalityLevel() {
		return this.fatalityLevel;
	}
	
	public Integer getFatalities() {
		return this.fatalities;
	}
	
	public Integer getMinDuration() {
		return this.minDuration;
	}
	
	public Integer getMaxDuration() {
		return this.maxDuration;
	}
	
	public Action getMaxAction() {
		return this.maxAction;
	}
	
	public HostilityLevel getMaxHostilityLevel() {
		return this.maxHostilityLevel;
	}
	
	public boolean getReciprocated() {
		return this.reciprocated;
	}
	
	public Integer getNumA() {
		return this.numA;
	}
	
	public Integer getNumB() {
		return this.numB;
	}
	
	public boolean getOngoing2010() {
		return this.ongoing2010;
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public List<MIDIncident> getIncidents() {
		return this.incidents;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		try {
			json.put("id", this.id);
			json.put("dispNum3", this.dispNum3);
			json.put("dispNum4", this.dispNum4);
			json.put("startDate", this.startDate.toString());
			json.put("endDate", this.endDate.toString());
			if (this.outcome != null)
				json.put("outcome", this.outcome.toString());
			if (this.settlement != null)
				json.put("settlement", this.settlement.toString());
			if (this.fatalityLevel != null)
				json.put("fatalityLevel", this.fatalityLevel.toString());
			json.put("fatalities", this.fatalities);
			json.put("minDuration", this.minDuration);
			json.put("maxDuration", this.maxDuration);
			if (this.maxAction != null)
				json.put("maxAction", this.maxAction.toString());
			if (this.maxHostilityLevel != null)
				json.put("maxHostilityLevel", this.maxHostilityLevel.toString());
			json.put("reciprocated", this.reciprocated);
			json.put("numA", this.numA);
			json.put("numB", this.numB);
			json.put("ongoing2010", this.ongoing2010);
			json.put("version", this.version);
			
			JSONArray jsonIncidents = new JSONArray();
			for (MIDIncident incident : this.incidents) {
				JSONObject jsonIncident = incident.toJSON();
				if (jsonIncident == null)
					return null;
				jsonIncidents.put(jsonIncident);
			}
			json.put("incidents", jsonIncidents);
		} catch (JSONException e) {
			return null;
		}
		
		return json;
	}

	@Override
	public boolean fromJSON(JSONObject json) {
		try {
			if (json.has("id"))
				this.id = json.getString("id");
			if (json.has("dispNum3"))
				this.dispNum3 = json.getInt("dispNum3");
			if (json.has("dispNum4"))
				this.dispNum4 = json.getInt("dispNum4");
			
			if (YearMonth.parse(json.getString("startDate")).toString().equals(json.getString("startDate")))
				this.startDate = new Partial(YearMonth.parse(json.getString("startDate")));
			else 
				this.startDate = new Partial(LocalDate.parse(json.getString("startDate")));
		
			if (YearMonth.parse(json.getString("endDate")).toString().equals(json.getString("endDate")))
				this.endDate = new Partial(YearMonth.parse(json.getString("endDate")));
			else 
				this.endDate = new Partial(LocalDate.parse(json.getString("endDate")));
			
			if (json.has("outcome"))
				this.outcome = Outcome.valueOf(json.getString("outcome"));
			if (json.has("settlement"))
				this.settlement = Settlement.valueOf(json.getString("settlement"));
			if (json.has("fatalityLevel"))
				this.fatalityLevel = FatalityLevel.valueOf(json.getString("fatalityLevel"));
			if (json.has("fatalities"))
				this.fatalities = json.getInt("fatalities");
			if (json.has("minDuration"))
				this.minDuration = json.getInt("minDuration");
			if (json.has("maxDuration"))
				this.maxDuration = json.getInt("maxDuration");
			if (json.has("maxAction"))
				this.maxAction = Action.valueOf(json.getString("maxAction"));
			if (json.has("maxHostilityLevel"))
				this.maxHostilityLevel = HostilityLevel.valueOf(json.getString("maxHostilityLevel"));
			this.reciprocated = json.getBoolean("reciprocated");
			if (json.has("numA"))
				this.numA = json.getInt("numA");
			if (json.has("numB"))
				this.numB = json.getInt("numB");
			this.ongoing2010 = json.getBoolean("ongoing2010");
			this.version = json.getString("version");
			
			JSONArray jsonIncidents = json.getJSONArray("incidents");
			this.incidents = new ArrayList<MIDIncident>();
			for (int i = 0; i < jsonIncidents.length(); i++) {
				MIDIncident incident = new MIDIncident(this);
				if (!incident.fromJSON(jsonIncidents.getJSONObject(i)))
					return false;
				this.incidents.add(incident);
			}
			
		} catch (JSONException e) {
			return false;
		}
		
		return true;
	}

	@Override
	public Storable makeInstance(StoreReference reference) {
		return new MIDDispute(reference);
	}

	@Override
	public StoreReference getStoreReference() {
		return this.reference;
	}

	@Override
	public String getId() {
		return this.id;
	}
}
