package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.Background;

/**
 * Objective representation of a Gherkin Background
 * Created by Sergey_Mishanin on 11/16/16.
 * @see Background
 */
public class ArmaBackground extends ArmaScenario {
    public ArmaBackground(){
        setKeyword("Background");
    }

    public ArmaBackground(Background gherkinBackground){
        setKeyword(gherkinBackground.getKeyword());
        setName(gherkinBackground.getName());
        setDescription(gherkinBackground.getDescription());
        setGherkinSteps(gherkinBackground.getSteps());
    }

    public ArmaBackground(ArmaBackground background){
        super(background);
    }
}
