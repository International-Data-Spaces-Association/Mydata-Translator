/**
 * 
 */
package de.fraunhofer.iese.ids.odrl.mydata.translator.util;

import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.RuleType;

/**
 * @author Robin Brandstaedter <Robin.Brandstaedter@iese.fraunhofer.de>
 *
 */
public class MyDataUtil {
	
	public static final String ALLOW = "allow";
	public static final String INHIBIT = "inhibit";
	

	public static String getMyDataDecision(RuleType ruleType) {
		
		switch (ruleType) {
			case PERMISSION :
			case OBLIGATION	:
			case PRE_DUTY :
			case POST_DUTY:
				return ALLOW;
			case PROHIBITION :
				return INHIBIT;
			default :
				return null; // TODO: throw exception?!
		}
	}
}
