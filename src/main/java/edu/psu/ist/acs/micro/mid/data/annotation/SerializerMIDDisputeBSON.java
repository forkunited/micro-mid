package edu.psu.ist.acs.micro.mid.data.annotation;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.Serializer;

public class SerializerMIDDisputeBSON extends Serializer<MIDDispute, Document>{
	private List<Index<MIDDispute>> indices;
	
	public SerializerMIDDisputeBSON() {
		this.indices = new ArrayList<Index<MIDDispute>>();
		this.indices.add(new Index<MIDDispute>() {
			@Override
			public String getField() {
				return "dispNum3";
			}

			@Override
			public Object getValue(MIDDispute item) {
				return item.getDispNum3();
			}
		});
	
	}

	@Override
	public String getName() {
		return "MIDDisputeBSON";
	}

	@Override
	public Document serialize(MIDDispute item) {
		return Document.parse(item.toJSON().toString()); // FIXME: This is dumb, but not worth fixing right now
	}

	@Override
	public MIDDispute deserialize(Document object) {
		MIDDispute dispute = new MIDDispute();
		
		try {
			if (!dispute.fromJSON(new JSONObject(object.toJson()))) // FIXME This is also dumb, but not worth fixing now
				return null;
		} catch (JSONException e) {
			return null;
		}
		
		return dispute;
	}

	@Override
	public String serializeToString(MIDDispute item) {
		return serialize(item).toJson();
	}

	@Override
	public MIDDispute deserializeFromString(String str) {
		return deserialize(Document.parse(str));
	}

	@Override
	public List<edu.cmu.ml.rtw.generic.data.Serializer.Index<MIDDispute>> getIndices() {
		return this.indices;
	}
}
