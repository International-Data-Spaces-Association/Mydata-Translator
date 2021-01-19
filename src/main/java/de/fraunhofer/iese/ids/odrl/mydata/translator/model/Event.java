package de.fraunhofer.iese.ids.odrl.mydata.translator.model;

import lombok.Data;

@Data
public class Event implements Operand {

    ParameterType type;
    String eventParameter;
    String jsonPath;

    public Event(ParameterType type, String eventParameter, String jsonPath) {
        this.type = type;
        this.eventParameter = eventParameter;
        this.jsonPath = jsonPath;
    }
    
    public Event(ParameterType type, String eventParameter) {
        this.type = type;
        this.eventParameter = eventParameter;
    }

    @Override
    public String toString() {
    	if(null != jsonPath) {
    		return  "        <event:" + type.getParameterType() + " eventParameter='" + eventParameter + "' jsonPathQuery='$." + jsonPath + "' default=''/>  \r\n";
    	}
    	else {
    		return  "        <event:" + type.getParameterType() + " eventParameter='" + eventParameter + "' default=''/>  \r\n";
    	}
        
    }
}
