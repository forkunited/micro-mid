package edu.psu.ist.acs.micro.mid.scratch;

/**
 * Scratch isn't for anything in particular.  It's just
 * for running short pieces of code when trying new things.
 * 
 * @author Bill McDowell
 *
 */
public class Scratch {
	public static void main(String[] args) {
		/*YearMonth ym = new YearMonth(1999, 12);
		LocalDate ld = new LocalDate(1999, 12, 10);
		System.out.println(ym.toString());
		Partial p = new Partial(ym);
		System.out.println(p.toString());
		System.out.println(ld.toString());
		System.out.println(YearMonth.parse(ld.toString()).toString());
		System.out.println(LocalDate.parse(ym.toString()).toString());*/
		/*
		DateTimeFormatter dateParser = DateTimeFormat.forPattern("MMMM dd, yyyy E");
		DateTimeFormatter dateOutputFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
		DateTime date = dateParser.parseDateTime("january 27, 2016 wednesday");
		System.out.println(date.toString(dateOutputFormat));*/
		/*
		String s = "DATELINE: Cairo";
		System.out.println(ConstructMID4NewsDocumentSet.isText(s) + " " + ConstructMID4NewsDocumentSet.isGarbageMetaData(s));
		*/
		
		System.out.println(">>>>>>".matches(">+"));
		/*
		String s = "asdf\nalskdjf";
		System.out.println(s.contains("df a"));
		System.out.println(s);
		
		DateTimeFormatter partialDateParser2 = DateTimeFormat.forPattern("MMMM yyyy");
		try {
			System.out.println(partialDateParser2.parseDateTime("March 1995"));
		} catch (Exception e) {
			System.out.println("No");
		}*/
	}
}
