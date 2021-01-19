package de.fraunhofer.iese.ids.odrl.mydata.translator.model;

import java.util.List;

import lombok.Data;

@Data
public class Parameter {

    ParameterType type;
    String name;
    String value;
    Event event;

    public Parameter(ParameterType type, String name, String value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }
    
    public Parameter(ParameterType type, String name, Event event) {
        this.type = type;
        this.name = name;
        this.event = event;
    }

    public Parameter() {
    }

    @Override
    public String toString() {
    	StringBuilder stringBuilder = new StringBuilder();
    	stringBuilder.append("<parameter:").append(type.getParameterType()).append(" name='").append(name).append("'");
    	if(null != event) {
    		stringBuilder.append(">").append(System.lineSeparator())
    		.append("      ").append(event.toString())
    		.append("            </parameter:").append(type.getParameterType()).append(">");
    	}
    	else {
    		stringBuilder.append(" value='").append(value).append("'/>");
    	}
        return  stringBuilder.toString();
    }
    
    public static String getParameters(List<Parameter> parameters) {
		if (null == parameters || parameters.isEmpty()) {
			return "";
		} else {
			StringBuilder params = new StringBuilder();
			for (Parameter parameter : parameters) {
				params.append("            ").append(parameter.toString()).append(System.lineSeparator());
			}
			return params.toString();
		}
    }
}
