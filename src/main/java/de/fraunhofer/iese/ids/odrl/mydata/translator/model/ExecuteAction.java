package de.fraunhofer.iese.ids.odrl.mydata.translator.model;


import java.util.List;

import de.fraunhofer.iese.ids.odrl.mydata.translator.interfaces.IDuty;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ActionType;
import lombok.Data;

@Data
public class ExecuteAction implements IDuty {

 String solution;
 ActionType action;
 List<Parameter> parameters;

 public ExecuteAction(String solution, ActionType action, List<Parameter> parameters) {
  this.solution = solution;
  this.action = action;
  this.parameters = parameters;
 }

 public ExecuteAction() {
 }


 @Override
 public String toString() {
  return  "          <execute action='urn:action:"+ solution +":"+ action.name().toLowerCase() +"'> " + System.lineSeparator() +
          getParameters() +
          "          </execute> " + System.lineSeparator();
 }

	private String getParameters() {
		return Parameter.getParameters(parameters);
	}
}
