package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.Background;

/**
 * Created by Sergey_Mishanin on 11/16/16.
 */
public class ArmaBackground extends ArmaScenario {
    public ArmaBackground(){

    }

    public ArmaBackground(Background gherkinBackground){
        setKeyword(gherkinBackground.getKeyword());
        setName(gherkinBackground.getName());
        setDescription(gherkinBackground.getDescription());
        setGherkinSteps(gherkinBackground.getSteps());
    }
}
