package de.fraunhofer.iese.ids.odrl.mydata.translator.util;

import java.util.Arrays;
import java.util.List;

import de.fraunhofer.iese.ids.odrl.mydata.translator.model.Constant;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.Event;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.EventParameter;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.MydataCondition;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.MydataPolicy;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.ParameterType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.Duration;
import de.fraunhofer.iese.ids.odrl.policy.library.model.OdrlPolicy;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ActionType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.Operator;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.RuleType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.TimeUnit;

@SuppressWarnings("rawtypes")
public class BuildMydataPolicyUtils {

	private BuildMydataPolicyUtils() {

	}

	public static MydataPolicy buildPolicy(OdrlPolicy odrlPolicy, ActionType actionType, RuleType ruleType, String solution) {

		//set mydataPolicy action
		ActionType action = actionType;

		//set mydataPolicy decision
		RuleType decision = ruleType;

		//set mydataPolicy target
		String target = "";
		if (null != odrlPolicy.getTarget()) {
			target = odrlPolicy.getTarget().toString();
		}

		//set mydataPolicy id
		String pid = "";
		if (null != odrlPolicy.getPolicyId()) {
			pid = getName(odrlPolicy.getPolicyId().toString());
		}

		//set mydataPolicy consumer : if type is agreement or request, it will have an consumer condition
//		String consumer = "";
//		if (null != odrlPolicy.getConsumer()) {
//			consumer = odrlPolicy.getConsumer().getName();
//		}

		// get conditions
		Event targetFirstOperand = new Event(ParameterType.STRING, EventParameter.TARGET.getEventParameter(), null);
		Constant targetSecondOperand = new Constant(ParameterType.STRING, target);
		MydataCondition targetCondition = new MydataCondition(targetFirstOperand, Operator.EQ, targetSecondOperand);

//		MydataCondition consumerCondition = new MydataCondition();
//		if (!consumer.isEmpty() && odrlPolicy.isProviderSide()) {
//			Event consumerFirstOperand = new Event(ParameterType.STRING, EventParameter.CONSUMER.getEventParameter(), "name");
//			Constant consumerSecondOperand = new Constant(ParameterType.STRING, consumer);
//			consumerCondition = new MydataCondition(consumerFirstOperand, Operator.EQUALS, consumerSecondOperand);
//
//		}

		// create MYDATA MydataPolicy
		MydataPolicy mydataPolicy = new MydataPolicy(solution, pid, action, decision, false, null);
		List<MydataCondition> cons = mydataPolicy.getConditions();
		cons.add(targetCondition);
//		cons.add(consumerCondition);
		mydataPolicy.setConditions(cons);
		mydataPolicy.setTarget(target);

		return mydataPolicy;
	}

	public static String getSolution(OdrlPolicy odrlPolicy)
	{
//		if (odrlPolicy.isProviderSide()) {
//			// when it is not a provider side policy, set the solution to consumer
//			return odrlPolicy.getProvider().getName();
//		}else
//		{
//			return odrlPolicy.getConsumer().getName();
//		}
		return "ids";
	}

	// get Duration(2,TimeUnit.H) from "PT2H"
	public static Duration getDurationFromPeriodValue(String value)
	{
		String valueWithoutP = value.substring(1);
		String lastChar = valueWithoutP.substring(valueWithoutP.length()-1);

		String d = "";
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

	private static String getLastSplitElement(String url) {
		String value;
		String[] bits = url.split(":");
		value = bits[bits.length-1];
		return value;
	}

	public static String getName(String uri) {
		// sample: http://example.com/policy/notify
		String[] termSplit = uri.toString().split("/");
		return termSplit[6];
	}

	public static Object[] addElement(Object[] a, Object e) {
		a  = Arrays.copyOf(a, a.length + 1);
		a[a.length - 1] = e;
		return a;
	}
}


