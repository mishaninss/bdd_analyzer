package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.Location;
import gherkin.ast.Scenario;
import gherkin.ast.Tag;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Sergey_Mishanin on 11/16/16.
 */
@Data
public class ArmaScenario extends ArmaNode{
    protected ArmaLocation location;
    protected List<ArmaStep> steps = new LinkedList<>();
    protected List<ArmaTag> tags = new LinkedList<>();

    public ArmaScenario(){
        setKeyword("Scenario");
    }

    public ArmaScenario(Scenario gherkinScenario){
        setKeyword(gherkinScenario.getKeyword());
        setName(gherkinScenario.getName());
        setDescription(gherkinScenario.getDescription());
        setGherkinSteps(gherkinScenario.getSteps());
        setGherkinTags(gherkinScenario.getTags());
        setLocation(gherkinScenario.getLocation());
    }

    public void setGherkinSteps(List<gherkin.ast.Step> gherkinSteps){
        if (CollectionUtils.isEmpty(gherkinSteps)){
            return;
        }
        steps = gherkinSteps.stream().map(ArmaStep::new).collect(Collectors.toList());
    }

    public void setGherkinTags(List<Tag> gherkinTags){
        if (CollectionUtils.isEmpty(gherkinTags)){
            return;
        }
        tags = gherkinTags.stream().map(ArmaTag::new).collect(Collectors.toList());
    }

    public void setLocation(Location gherkinLocation){
        location = new ArmaLocation(gherkinLocation);
    }

    public void setLocation(ArmaLocation location){
        this.location = location;
    }

    public void setLocation(File file){
        if (location == null){
            location = new ArmaLocation();
        }
        location.setFile(file.getPath());
    }

    public boolean isImplemented(){
        if (CollectionUtils.isEmpty(steps)){
            return true;
        }

        for (ArmaStep step: steps){
            if (!step.isImplemented()){
                return false;
            }
        }

        return true;
    }

    public boolean isFullyEqualTo(ArmaScenario anotherScenario){
        if (steps.size() != anotherScenario.getSteps().size()){
            return false;
        } else {
            for (int i=0; i<steps.size(); i++){
                if (!steps.get(i).isFullyEqualTo(anotherScenario.getSteps().get(i))){
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isEqualToIgnoreParameters(ArmaScenario anotherScenario){
        if (steps.size() != anotherScenario.getSteps().size()){
            return false;
        } else {
            for (int i=0; i<steps.size(); i++){
                if (!steps.get(i).isEqualToIgnoreParameters(anotherScenario.getSteps().get(i))){
                    return false;
                }
            }
        }
        return true;
    }

    public void replaceStep(ArmaStep oldStep, ArmaStep newStep){
        if (oldStep.equals(newStep) || CollectionUtils.isEmpty(steps)){
            return;
        } else {
            steps.replaceAll(step -> {
                if (step.equals(oldStep)){
                    return newStep;
                } else {
                    return step;
                }
            });
        }
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        if (tags != null) {
            sb.append(StringUtils.join(tags, " ")).append("\n");
        }
        sb.append(keyword).append(": ").append(name);
        if (StringUtils.isNoneBlank(description)){
            sb.append("\n")
                .append(ArmaFeature.addPad(description));
        }
        if (steps != null) {
            steps.forEach(step -> sb.append("\n").append(ArmaFeature.addPad(step.toString())));
        }
        return sb.toString();
    }
}
