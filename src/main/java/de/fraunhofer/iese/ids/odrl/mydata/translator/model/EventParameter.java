package de.fraunhofer.iese.ids.odrl.mydata.translator.model;

public enum EventParameter {

    TARGET("TargetDataUri"),

    PROVIDER("provider"),

    CONSUMER("consumer"),

    DIGIT("digit");

    private final String eventParameter;

    EventParameter(String a) {
        eventParameter = a;
    }

    public String getEventParameter() {
        return eventParameter;
    }

}

