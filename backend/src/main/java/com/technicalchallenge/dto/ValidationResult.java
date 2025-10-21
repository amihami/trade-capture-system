package com.technicalchallenge.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {

    private final List<String> errors = new ArrayList<>();

    public static ValidationResult ok(){
        return new ValidationResult();
    }

    public boolean okStatus(){
        return errors.isEmpty();
    }

    public boolean failed(){
        return !okStatus();
    }

    public void addError(String message){
        if(message != null && !message.isBlank()){
            errors.add(message);
        }
    }

    public List<String> getErrors(){
        return Collections.unmodifiableList(errors);
    }
}
