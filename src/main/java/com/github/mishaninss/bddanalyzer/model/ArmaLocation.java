package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.Location;
import lombok.Data;
import org.apache.commons.io.FilenameUtils;

/**
 * Created by Sergey_Mishanin on 11/17/16.
 */
@Data
public class ArmaLocation {
    private String file;
    private int line;
    private int column;

    public ArmaLocation(){

    }

    public ArmaLocation(Location gherkinLocation){
        if (gherkinLocation == null){
            return;
        }
        line = gherkinLocation.getLine();
        column = gherkinLocation.getColumn();
    }

    public ArmaLocation(ArmaLocation location){
        if (location == null){
            return;
        }
        file = location.getFile();
        line = location.getLine();
        column = location.getColumn();
    }

    @Override
    public String toString(){
        return file + " " + line + ":" + column;
    }

    public String toShortString(){
        return FilenameUtils.getName(file) + " " + line + ":" + column;
    }
}
