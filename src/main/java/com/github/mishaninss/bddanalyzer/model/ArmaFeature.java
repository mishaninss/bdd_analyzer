package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.*;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Sergey_Mishanin on 11/16/16.
 */
@Data
public class ArmaFeature extends ArmaNode{
    private ArmaLocation location;
    private List<ArmaTag> tags = new LinkedList<>();
    private ArmaBackground background;
    private List<ArmaScenario> scenarios = new LinkedList<>();

    public ArmaFeature(){
        setKeyword("Feature");
    }

    public ArmaFeature(Feature gherkinFeature){
        setGherkinTags(gherkinFeature.getTags());
        setKeyword(gherkinFeature.getKeyword());
        setName(gherkinFeature.getName());
        setDescription(gherkinFeature.getDescription());
        setGherkinChildren(gherkinFeature.getChildren());
        setLocation(gherkinFeature.getLocation());
    }

    public void setGherkinTags(List<Tag> gherkinTags){
        if (CollectionUtils.isEmpty(gherkinTags)){
            return;
        }
        tags = gherkinTags.stream().map(ArmaTag::new).collect(Collectors.toList());
    }

    public void setGherkinChildren(List<ScenarioDefinition> gherkinScenarios){
        if (CollectionUtils.isEmpty(gherkinScenarios)){
            return;
        }
        scenarios = new ArrayList<>();
        gherkinScenarios.forEach(gherkinScenario ->
            {
                if (gherkinScenario instanceof Scenario){
                    scenarios.add(new ArmaScenario((Scenario) gherkinScenario));
                } else if (gherkinScenario instanceof ScenarioOutline){
                    scenarios.add(new ArmaScenarioOutline((ScenarioOutline) gherkinScenario));
                } else if (gherkinScenario instanceof Background){
                    setBackground(new ArmaBackground((Background) gherkinScenario));
                }
            });
    }

    public void setLocation(Location gherkinLocation){
        location = new ArmaLocation(gherkinLocation);
    }

    public void setLocation(File file){
        if (location == null){
            location = new ArmaLocation();
        }
        location.setFile(file.getPath());
        scenarios.forEach(scenario -> scenario.setLocation(file));
    }

    public static String addPad(String string){
        StringBuilder outter = new StringBuilder();

        Pattern pattern = Pattern.compile("(?m)(^)");
        Matcher matcher = pattern.matcher(string);
        outter.append(matcher.replaceAll("  "));
        return outter.toString();
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
                .append(addPad(description));
        }

        if (background != null){
            sb.append("\n\n")
                .append(addPad(background.toString()));
        }

        if (scenarios != null) {
            scenarios.forEach(scenario ->
            {
                String scenarioText = scenario.toString();
                sb.append("\n\n")
                        .append(addPad(scenarioText));
            });
        }
        return sb.toString();
    }
}
