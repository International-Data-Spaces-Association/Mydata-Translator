package de.fraunhofer.iese.ids.odrl.mydata.translator.model;


import java.util.List;

import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ActionType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.LeftOperand;
import lombok.Data;

@Data
public class Count implements Operand {
 String solution;
 LeftOperand leftOperand;
 List<Parameter> parameters;
 FixedTime fixedTime;
 ActionType action;

 public Count(String solution, LeftOperand leftOperand, ActionType action, List<Parameter> parameters, FixedTime fixedTime) {
  this.solution = solution;
  this.leftOperand = leftOperand;
  this.action = action;
  this.parameters = parameters;
  this.fixedTime = fixedTime;
 }

 public Count() {
 }


 @Override
 public String toString() {
  if(null != leftOperand)
  {
   return  "        <count> " + System.lineSeparator() +
		   "          <eventOccurrence event='urn:action:"+ solution +":"+ leftOperand.getMydataLeftOperand() +"'> " + System.lineSeparator() +
           getParameters() +
           "          </eventOccurrence> " + System.lineSeparator() +
           "          <when fixedTime='"+ fixedTime.getFixedTime() +"'/> " + System.lineSeparator() +
           "        </count> "  + System.lineSeparator();
  }else if(null != action){
   return  "        <count> " + System.lineSeparator() +
           "          <eventOccurrence event='urn:action:"+ solution +":"+ action.name().toLowerCase() +"'> " + System.lineSeparator() +
           getParameters() +
           "          </eventOccurrence> "  + System.lineSeparator()+
           "          <when fixedTime='"+ fixedTime.getFixedTime() +"'/> " + System.lineSeparator() +
           "        </count> " + System.lineSeparator();
  }
  return "";
 }

	private String getParameters() {
		return Parameter.getParameters(parameters);
	}
}
