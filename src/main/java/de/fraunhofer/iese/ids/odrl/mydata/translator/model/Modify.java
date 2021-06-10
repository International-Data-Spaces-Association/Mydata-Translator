package de.fraunhofer.iese.ids.odrl.mydata.translator.model;

import java.util.List;

import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ActionType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ModificationMethod;
import lombok.Data;

@Data
public class Modify implements Operand {

    String eventParameter;
    ActionType modifyAction;
    String jsonPath;
    List<Parameter> parameters;

    public Modify(String eventParameter, ActionType modifyAction, String jsonPath, List<Parameter> parameters) {

        this.eventParameter = eventParameter;
        this.modifyAction = modifyAction;
        this.jsonPath = jsonPath;
        this.parameters = parameters;
    }

    public Modify() {
    }

    @Override
    public String toString() {
        return  "        <modify eventParameter='" + eventParameter + "' method='"+ modifyAction.name().toLowerCase() +"' jsonPathQuery='" + jsonPath + "'> " + System.lineSeparator() +
                getParameters() +
                "        </modify> " + System.lineSeparator();
    }

	private String getParameters() {
		return Parameter.getParameters(parameters);
	}
}
