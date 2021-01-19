package de.fraunhofer.iese.ids.odrl.mydata.translator.model;

import lombok.Data;

@Data
public class Constant implements Operand {

    ParameterType type;
    String value;

    public Constant(ParameterType type, String value) {
        this.type = type;
        this.value = value;
    }

    public Constant() {
    }

    @Override
    public String toString() {
        return  "        <constant:" + type.getParameterType() + " value='" + value + "'/>  \r\n";
    }
}
