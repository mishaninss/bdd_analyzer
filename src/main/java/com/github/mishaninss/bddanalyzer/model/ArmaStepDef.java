package com.github.mishaninss.bddanalyzer.model;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

import static com.github.mishaninss.bddanalyzer.model.ArmaProject.GSON;

/**
 * Created by Sergey_Mishanin on 11/15/16.
 */
@Data
public class ArmaStepDef {
    private String text;
    private ArmaStepDefLocation location;
    private String description;
    private boolean implemented;

    @Override
    public String toString() {
        return GSON.toJson(this);
    }

    public ArmaTableRow toTableRow(){
        return new ArmaTableRow(text, location.toShortString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArmaStepDef)) return false;
        ArmaStepDef that = (ArmaStepDef) o;
        return implemented == that.implemented &&
                Objects.equals(text, that.text) &&
                Objects.equals(location, that.location) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text, location, description, implemented);
    }
}