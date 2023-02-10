package de.fraunhofer.iese.ids.odrl.mydata.translator.util;

import de.fraunhofer.iese.ids.odrl.mydata.translator.model.MydataPolicy;
import de.fraunhofer.iese.ids.odrl.policy.library.model.Duration;
import de.fraunhofer.iese.ids.odrl.policy.library.model.OdrlPolicy;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.TimeUnit;

public class BuildMydataPolicyUtils {

	//static class
	private BuildMydataPolicyUtils() {
	}
	
	public static MydataPolicy buildPolicy(OdrlPolicy odrlPolicy, String solution) {
		//set mydataPolicy id
		String pid = "";
		if (null != odrlPolicy.getPolicyId()) {
			pid = getName(odrlPolicy.getPolicyId().toString());
		}

		return new MydataPolicy(pid, solution);
	}


	public static String getSolution(OdrlPolicy odrlPolicy)
	{
		return "ids";
	}

	// get Duration(2,TimeUnit.H) from "PT2H"
	public static Duration getDurationFromPeriodValue(String value)
	{
		String valueWithoutP = value.substring(1);
		String lastChar = valueWithoutP.substring(valueWithoutP.length()-1);

		if(lastChar.equals("H"))
		{
			valueWithoutP = valueWithoutP.substring(1);
		}
		// valueWithoutP is like : 2H
		int n = Integer.parseInt(valueWithoutP.substring(0,valueWithoutP.length()-1));
		TimeUnit t = TimeUnit.HOURS;
		switch(lastChar) {
			case "H":
				t = TimeUnit.HOURS;
				break;

			case "D":
				t = TimeUnit.DAYS;
				break;

			case "M":
				t = TimeUnit.MONTHS;
				break;

			case "Y":
				t = TimeUnit.YEARS;
				break;

		}

		return new Duration(n,t);

	}

	public static String getName(String uri) {
		// sample: http://example.com/policy/notify
		String[] termSplit = uri.toString().split("/");
		return termSplit[termSplit.length-1];
	}

	public static boolean isNull(Object o) {
		return null == o;
	}

	public static boolean isNotNull(Object o) {
		return null != o;
	}

	public static boolean isNullOrEmpty(String value) {
		return (null == value || value.isEmpty());
	}
}


