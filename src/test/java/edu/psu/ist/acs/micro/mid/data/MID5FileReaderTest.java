package edu.psu.ist.acs.micro.mid.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;


public class MID5FileReaderTest {
	@Test
	public void testReader() {
		String text = "\nHeadline: A story\n" +
					  "Key: id\n" +
					  "Date: 20160213\n" +
					  "Source: A source\n" +
					  "Dateline: A dateline\n" +
					  "Byline: A byline\n" +
					  "\n" +
					  ">>>>>>>>>>>>>>>>>>>>>>\n" +
					  "President clinton ate soup for lunch, and then he fortified the border.\n" +
					  "<<<<<<<<<<<<<<<<<<<<<<\n" +
					  "\n" +
					  "---------------------------------------------------------------\n" +
					  "\n" +
					  "Headline: Another story\n" +
					  "Key: id2\n" +
					  "Date: 20160213\n" +
					  "Source: A second source\n" +
					  "Dateline: A second dateline\n" +
					  "Byline: A byline\n" +
					  "\n" +
					  ">>>>>>>>>>>>>>>>>>>>>>\n" +
					  "President clinton ate soup for lunch, and then he fortified the border.  Iraq was attacked.  There were airstrikes.  Soldiers ded.  Ground troops.  Alarms.  Raid.  Prime minister.  Defended.\n" +
					  "<<<<<<<<<<<<<<<<<<<<<<\n" +
					  "\n" +
					  "---------------------------------------------------------------\n" +
					  "\n" +
					  "Headline: Another story\n" +
					  "Key: id3\n" +
					  "Date: 20160213\n" +
					  "Source: A second source\n" +
					  "Dateline: A second dateline\n" +
					  "Byline: A byline\n" +
					  "\n" +
					  ">>>>>>>>>>>>>>>>>>>>>>\n" +
					  "I ate soup for lunch.\n" +
					  "<<<<<<<<<<<<<<<<<<<<<<\n" +
					  "\n" +
					  "---------------------------------------------------------------";
		
		MID5FileReader r = new MID5FileReader(new BufferedReader(new StringReader(text)));
		try {
			r.readOne();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
