package de.fraunhofer.iese.ids.odrl.mydata.translator.model;

import java.util.List;

import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.LeftOperand;
import lombok.Data;

@Data
public class PIPBoolean {
	String solution;
	LeftOperand leftOperand;
	List<Parameter> parameters;

	public PIPBoolean(String solution, LeftOperand leftOperand, List<Parameter> parameters) {
		this.solution = solution;
		this.leftOperand = leftOperand;
		this.parameters = parameters;
	}

	public PIPBoolean() {
	}

	@Override
	public String toString() {
		return "          <pip:boolean method='urn:info:" + solution + ":" + leftOperand.getMydataLeftOperand()
				+ "' default='false'>" + System.lineSeparator() + getParameters() + "          </pip:boolean> "
				+ System.lineSeparator();
	}

	private String getParameters() {
		return Parameter.getParameters(parameters);
	}
}
