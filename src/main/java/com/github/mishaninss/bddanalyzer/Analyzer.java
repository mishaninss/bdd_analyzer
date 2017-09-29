package com.github.mishaninss.bddanalyzer;


import com.github.mishaninss.bddanalyzer.model.ArmaProject;

public class Analyzer {

    private static final String STEP_DEF_ROOT = "";
    private static final String FEATURES_ROOT = "";

    public static void main(String[] args){
        ArmaProject project = new ArmaProject(STEP_DEF_ROOT, FEATURES_ROOT);
        project.scan();
        project.printStatistics();

        project.getNotImplementedSteps().forEach(System.out::println);
    }


}
