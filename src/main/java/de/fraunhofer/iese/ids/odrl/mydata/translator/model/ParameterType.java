package de.fraunhofer.iese.ids.odrl.mydata.translator.model;

public enum ParameterType {

    STRING("string"),

    NUMBER("number"),

    BOOLEAN("boolean"),

    OBJECT("object"),

    LIST("list");

    private final String type;

    ParameterType(String a) {
        type = a;
    }

    public String getParameterType() {
        return type;
    }
}

