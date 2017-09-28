package com.github.mishaninss.bddanalyzer.model;

import lombok.Data;

/**
 * Created by Sergey_Mishanin on 11/17/16.
 */
@Data
public class ArmaStepDefLocation {
    private String file;
    private String methodName;
    private String declaration;
    private int line;
    private int column;

    @Override
    public String toString(){
        return file + " : " + line + " > " + methodName;
    }
}
