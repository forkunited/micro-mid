package edu.psu.ist.acs.micro.mid.scratch;

import java.io.File;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.psu.ist.acs.micro.mid.data.MIDDataTools;
import edu.psu.ist.acs.micro.mid.util.MIDProperties;


/**
 * RunMIDContext runs ctx scripts from the
 * src/main/resources/contexts directory.  These
 * scripts are mainly used to train MID relevance
 * and attribute models.  See README.md for details
 * about the ctx scripts.
 * 
 * @author Bill McDowell
 *
 */
public class RunMIDContext {
	public static void main(String[] args) {
		Context.run(new MIDDataTools(new OutputWriter(), new MIDProperties()), new File(args[0]));
	}
}
