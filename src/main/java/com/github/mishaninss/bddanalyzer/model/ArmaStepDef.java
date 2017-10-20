package com.github.mishaninss.bddanalyzer.model;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

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
        return new ToStringBuilder(this)
                .append("text", text)
                .append("description", description)
                .append("location", location)
                .append("implemented", implemented)
                .toString();
    }

    public ArmaTableRow toTableRow(){
        return new ArmaTableRow(text, location.toShortString());
    }
}