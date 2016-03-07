package edu.psu.ist.acs.micro.mid.scratch;

import java.io.File;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;

public class RunMIDContext {
	public static void main(String[] args) {
		Context.run(new MIDDataTools(new OutputWriter(), new MIDProperties()), new File(args[0]));
	}
}
