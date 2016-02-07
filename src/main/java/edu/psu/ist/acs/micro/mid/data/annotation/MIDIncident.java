package edu.psu.ist.acs.micro.mid.data.annotation;


import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.Partial;
import org.joda.time.YearMonth;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.util.JSONSerializable;

public class MIDIncident implements JSONSerializable {
	private static final int NULL_ID = -9;
	
	public static enum HostilityLevel {
		NO_MILITARIZED_ACTION(1),
		THREAT_TO_USE_FORCE(2),
		DISPLAY_OF_FORCE(3),
		USE_OF_FORCE(4),
		WAR(5);
		
		private int id;
		
		HostilityLevel(int id) {
			this.id = id;
		}

		public static HostilityLevel valueOf(int id) {
			if (id == NULL_ID)
				return null;
			
			HostilityLevel[] levels = HostilityLevel.values();
			for (HostilityLevel level : levels) {
				if (level.id == id)
					return level;
			}
			return null;
		}
	}
	
	public static enum Action {
		NO_MILITARIZED_ACTION(0, HostilityLevel.NO_MILITARIZED_ACTION),
		THREAT_TO_USE_FORCE(1, HostilityLevel.THREAT_TO_USE_FORCE),
		THREAT_TO_BLOCKADE(2, HostilityLevel.THREAT_TO_USE_FORCE),
		THREAT_TO_OCCUPY_TERRITORY(3, HostilityLevel.THREAT_TO_USE_FORCE),
		THREAT_TO_DECLARE_WAR(4, HostilityLevel.THREAT_TO_USE_FORCE),
		THREAT_TO_USE_CBR_WEAPONS(5, HostilityLevel.THREAT_TO_USE_FORCE),
		THREAT_TO_JOIN_WAR(6, HostilityLevel.THREAT_TO_USE_FORCE),
		SHOW_OF_FORCE(7, HostilityLevel.DISPLAY_OF_FORCE),
		ALERT(8, HostilityLevel.DISPLAY_OF_FORCE),
		NUCLEAR_ALERT(9, HostilityLevel.DISPLAY_OF_FORCE),
		MOBILIZATION(10, HostilityLevel.DISPLAY_OF_FORCE),
		FORTIFY_BORDER(11, HostilityLevel.DISPLAY_OF_FORCE),
		BORDER_VIOLATION(12, HostilityLevel.DISPLAY_OF_FORCE),
		BLOCKADE(13, HostilityLevel.USE_OF_FORCE),
		OCCUPATION_OF_TERRITORY(14, HostilityLevel.USE_OF_FORCE),
		SEIZURE(15, HostilityLevel.USE_OF_FORCE),
		ATTACK(16, HostilityLevel.USE_OF_FORCE),
		CLASH(17, HostilityLevel.USE_OF_FORCE),
		DECLARATION_OF_WAR(18, HostilityLevel.USE_OF_FORCE),
		USE_OF_CBR_WEAPONS(19, HostilityLevel.USE_OF_FORCE),
		BEGIN_INTERSTATE_WAR(20, HostilityLevel.WAR),
		JOIN_INTERSTATE_WAR(21, HostilityLevel.WAR);
		
		private int id;
		private HostilityLevel hostilityLevel;
		
		Action(int id, HostilityLevel hostilityLevel) {
			this.id = id;
			this.hostilityLevel = hostilityLevel;
		}
		
		public HostilityLevel getHostilityLevel() {
			return this.hostilityLevel;
		}
		
		public static Action valueOf(int id) {
			if (id == NULL_ID)
				return null;
			
			Action[] actions = Action.values();
			for (Action action : actions) {
				if (action.id == id)
					return action;
			}
			return null;
		}
	}
	
	public static enum FatalityLevel {
		NONE(0, 0, 0),
		FIRST(1, 1, 25),
		SECOND(2, 26, 100),
		THIRD(3, 101, 250),
		FOURTH(4, 251, 500),
		FIFTH(5, 501, 999),
		SIXTH(6, 999, Integer.MAX_VALUE);
		
		private int id;
		private int minDeaths;
		private int maxDeaths;
		
		FatalityLevel(int id, int minDeaths, int maxDeaths) {
			this.id = id;
			this.minDeaths = minDeaths;
			this.maxDeaths = maxDeaths;
		}
		
		public int getMinDeaths() {
			return this.minDeaths;
		}
		
		public int getMaxDeaths() {
			return this.maxDeaths;
		}
		
		public static FatalityLevel valueOf(int id) {
			if (id == NULL_ID)
				return null;
			
			FatalityLevel[] levels = FatalityLevel.values();
			for (FatalityLevel level : levels) {
				if (level.id == id)
					return level;
			}
			return null;
		}
	}
	
	public static enum RevisionType {
		NOT_APPLICABLE(0),
		TERRITORY(1),
		POLICY(2),
		REGIME_OR_GOVERNMENT(3),
		OTHER(4);
		
		private int id;
		
		RevisionType(int id) {
			this.id = id;
		}
		
		public static RevisionType valueOf(int id) {
			if (id == NULL_ID)
				return null;
			
			RevisionType[] types = RevisionType.values();
			for (RevisionType type : types) {
				if (type.id == id)
					return type;
			}
			return null;
		}
	}
	
	public static class Participant implements JSONSerializable {
		private MIDIncident incident;
		
		private String country;
		private Partial startDate;
		private Partial endDate;
		private boolean inSideA;
		private boolean sideA;
		private FatalityLevel fatalityLevel;
		private Integer fatalities;
		private Action action;
		private HostilityLevel hostilityLevel;
		private RevisionType revType1;
		private RevisionType revType2;
		private String version;
		
		public Participant(MIDIncident incident) {
			this.incident = incident;
		}

		public Participant(MIDIncident incident,
						   String country,
						   Partial startDate,
						   Partial endDate,
						   boolean inSideA,
						   boolean sideA,
						   FatalityLevel fatalityLevel,
						   Integer fatalities,
						   Action action,
						   HostilityLevel hostilityLevel,
						   RevisionType revType1,
						   RevisionType revType2,
						   String version) {
			this.incident = incident;
			this.country = country;
			this.startDate = startDate;
			this.endDate = endDate;
			this.inSideA = inSideA;
			this.sideA = sideA;
			this.fatalityLevel = fatalityLevel;
			this.fatalities = fatalities;
			this.action = action;
			this.hostilityLevel = hostilityLevel;
			this.revType1 = revType1;
			this.revType2 = revType2;
			this.version = version;
		}
		
		
		public MIDIncident getIncident() {
			return this.incident;
		}
		
		public String getCountry() {
			return this.country;
		}
		
		public Partial getStartDate() {
			return this.startDate;
		}

		public Partial getEndDate() {
			return this.endDate;
		}
		
		public boolean getInSideA() {
			return this.inSideA;
		}
		
		public boolean getSideA() {
			return this.sideA;
		}
		
		public FatalityLevel getFatalityLevel() {
			return this.fatalityLevel;
		}
		
		public int getFatalities() {
			return this.fatalities;
		}
		
		public Action getAction() {
			return this.action;
		}
		
		public HostilityLevel getHostilityLevel() {
			return this.hostilityLevel;
		}
		
		public RevisionType getRevType1() {
			return this.revType1;
		}
		
		public RevisionType getRevType2() {
			return this.revType2;
		}
		
		@Override
		public JSONObject toJSON() {
			JSONObject json = new JSONObject();
			
			try {
				json.put("country", this.country);
				json.put("startDate", this.startDate.toString());
				json.put("endDate", this.endDate.toString());
				json.put("inSideA", this.inSideA);
				json.put("sideA", this.sideA);
				json.put("version", this.version);
				if (this.fatalityLevel != null)
					json.put("fatalityLevel", this.fatalityLevel.toString());
				if (this.fatalities != null)
					json.put("fatalities", this.fatalities);
				if (this.action != null)
					json.put("action", this.action.toString());
				if (this.hostilityLevel != null)
					json.put("hostilityLevel", this.hostilityLevel.toString());
				if (this.revType1 != null)
					json.put("revType1", this.revType1.toString());
				if (this.revType2 != null)
					json.put("revType2", this.revType2.toString());
				
			} catch (JSONException e) {
				return null;
			}
			
			return json;
		}

		@Override
		public boolean fromJSON(JSONObject json) { 
			try {
				this.country = json.getString("country");
				
				if (YearMonth.parse(json.getString("startDate")).toString().equals(json.getString("startDate")))
					this.startDate = new Partial(YearMonth.parse(json.getString("startDate")));
				else 
					this.startDate = new Partial(LocalDate.parse(json.getString("startDate")));
			
				if (YearMonth.parse(json.getString("endDate")).toString().equals(json.getString("endDate")))
					this.endDate = new Partial(YearMonth.parse(json.getString("endDate")));
				else 
					this.endDate = new Partial(LocalDate.parse(json.getString("endDate")));
				
				this.inSideA = json.getBoolean("inSideA");
				this.sideA = json.getBoolean("sideA");
				this.version = json.getString("version");
				
				if (json.has("fatalityLevel"))
					this.fatalityLevel = FatalityLevel.valueOf(json.getString("fatalityLevel"));
				if (json.has("fatalities"))
					this.fatalities = json.getInt("fatalities");
				if (json.has("action"))
					this.action = Action.valueOf(json.getString("action"));
				if (json.has("hostilityLevel"))
					this.hostilityLevel = HostilityLevel.valueOf(json.getString("hostilityLevel"));
				if (json.has("revType1"))
					this.revType1 = RevisionType.valueOf(json.getString("revType1"));
				if (json.has("revType2"))
					this.revType2 = RevisionType.valueOf(json.getString("revType2"));
			} catch (JSONException e) {
				return false;
			}
			
			return true;
		}
	}
	
	private MIDDispute dispute;
	
	private Integer incidNum3;
	private Integer incidNum4;
	private Partial startDate;
	private Partial endDate;
	private Integer duration;
	private Integer TBI;
	private FatalityLevel fatalityLevel;
	private Integer fatalities;
	private Action action;
	private HostilityLevel hostilityLevel;
	private Integer numA;
	private RevisionType revType1;
	private RevisionType revType2;
	private String version;
	
	private List<Participant> participants;

	public MIDIncident(MIDDispute dispute) {
		this.dispute = dispute;
	}
	
	public MIDIncident(MIDDispute dispute,
					   Integer incidNum3,
					   Integer incidNum4,
					   Partial startDate,
					   Partial endDate,
					   Integer duration,
					   Integer TBI,
					   FatalityLevel fatalityLevel,
					   Integer fatalities,
					   Action action,
					   HostilityLevel hostilityLevel,
					   Integer numA,
					   RevisionType revType1,
					   RevisionType revType2,
					   String version,
					   List<Participant> participants) {
		this.dispute = dispute;
		this.incidNum3 = incidNum3;
		this.incidNum4 = incidNum4;
		this.startDate = startDate;
		this.endDate = endDate;
		this.duration = duration;
		this.TBI = TBI;
		this.fatalityLevel = fatalityLevel;
		this.fatalities = fatalities;
		this.action = action;
		this.hostilityLevel = hostilityLevel;
		this.numA = numA;
		this.revType1 = revType1;
		this.revType2 = revType2;
		this.version = version;
		this.participants = participants;
	}
	
	public MIDDispute getDispute() {
		return this.dispute;
	}
	
	public Integer getIncidNum3() {
		return this.incidNum3;
	}
	
	public Integer getIncidNum4() {
		return this.incidNum4;
	}

	public Partial getStartDate() {
		return this.startDate;
	}
	
	public Partial getEndDate() {
		return this.endDate;
	}
	
	public Integer getDuration() {
		return this.duration;
	}
	
	public Integer getTBI() {
		return this.TBI;
	}
	
	public FatalityLevel getFatalityLevel() {
		return this.fatalityLevel;
	}
	
	public Integer getFatalities() {
		return this.fatalities;
	}
	
	public Action getAction() {
		return this.action;
	}
	
	public HostilityLevel getHostilityLevel() {
		return this.hostilityLevel;
	}
	
	public Integer getNumA() {
		return this.numA;
	}
	
	public RevisionType getRevType1() {
		return this.revType1;
	}
	
	public RevisionType getRevType2() {
		return this.revType2;
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public List<Participant> getParticipants() {
		return this.participants;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		try {
			json.put("incidNum3", this.incidNum3);
			json.put("incidNum4", this.incidNum4);
			json.put("startDate", this.startDate.toString());
			json.put("endDate", this.endDate.toString());
			json.put("duration", this.duration);
			json.put("TBI", this.TBI);
			if (this.fatalityLevel != null)
				json.put("fatalityLevel", this.fatalityLevel.toString());
			json.put("fatalities", this.fatalities);
			if (this.action != null)
				json.put("action", this.action.toString());
			if (this.hostilityLevel != null)
				json.put("hostilityLevel", this.hostilityLevel.toString());
			json.put("numA", this.numA);
			if (this.revType1 != null)
				json.put("revType1", this.revType1.toString());
			if (this.revType2 != null)
				json.put("revType2", this.revType2.toString());
			json.put("version", this.version);	
		
			JSONArray jsonParticipants = new JSONArray();
			for (Participant participant : this.participants) {
				JSONObject jsonParticipant = participant.toJSON();
				if (jsonParticipant == null)
					return null;
				jsonParticipants.put(jsonParticipant);
			}
			json.put("participants", jsonParticipants);
		} catch (JSONException e) {
			return null;
		}
		
		return json;
	}

	@Override
	public boolean fromJSON(JSONObject json) {
		try {
			if (json.has("incidNum3"))
				this.incidNum3 = json.getInt("incidNum3");
			if (json.has("incidNum4"))
				this.incidNum4 = json.getInt("incidNum4");
			
			if (YearMonth.parse(json.getString("startDate")).toString().equals(json.getString("startDate")))
				this.startDate = new Partial(YearMonth.parse(json.getString("startDate")));
			else 
				this.startDate = new Partial(LocalDate.parse(json.getString("startDate")));
		
			if (YearMonth.parse(json.getString("endDate")).toString().equals(json.getString("endDate")))
				this.endDate = new Partial(YearMonth.parse(json.getString("endDate")));
			else 
				this.endDate = new Partial(LocalDate.parse(json.getString("endDate")));
			
			if (json.has("duration"))
				this.duration = json.getInt("duration");
			if (json.has("TBI"))
				this.TBI = json.getInt("TBI");
			if (json.has("fatalityLevel"))
				this.fatalityLevel = FatalityLevel.valueOf(json.getString("fatalityLevel"));
			if (json.has("fatalities"))
				this.fatalities = json.getInt("fatalities");
			if (json.has("action"))
				this.action = Action.valueOf(json.getString("action"));
			if (json.has("hostilityLevel"))
				this.hostilityLevel = HostilityLevel.valueOf(json.getString("hostilityLevel"));
			if (json.has("numA"))
				this.numA = json.getInt("numA");
			if (json.has("revType1"))
				this.revType1 = RevisionType.valueOf(json.getString("revType1"));
			if (json.has("revType2"))
				this.revType2 = RevisionType.valueOf(json.getString("revType2"));
			if (json.has("version"))
				this.version = json.getString("version");
			
			JSONArray jsonParticipants = json.getJSONArray("participants");
			this.participants = new ArrayList<Participant>();
			for (int i = 0; i < jsonParticipants.length(); i++) {
				Participant participant = new Participant(this);
				if (!participant.fromJSON(jsonParticipants.getJSONObject(i)))
						return false;
				this.participants.add(participant);
			}
		} catch (JSONException e) {
			return false;
		}
		
		return true;
	}
}
