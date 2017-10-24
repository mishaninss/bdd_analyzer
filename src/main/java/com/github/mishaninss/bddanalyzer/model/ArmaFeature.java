package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.*;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Objective representation of a Gherkin Feature
 * Created by Sergey_Mishanin on 11/16/16.
 * @see Feature
 */
@Data
public class ArmaFeature extends ArmaNode implements HasTags{
    private ArmaLocation location;
    private Set<ArmaTag> tags = new LinkedHashSet<>();
    private ArmaBackground background;
    private List<ArmaScenario> scenarios = new LinkedList<>();

    public ArmaFeature(){
        setKeyword("Feature");
    }

    public ArmaFeature(@NonNull Feature gherkinFeature){
        setGherkinTags(gherkinFeature.getTags());
        setKeyword(gherkinFeature.getKeyword());
        setName(gherkinFeature.getName());
        setDescription(gherkinFeature.getDescription());
        setGherkinChildren(gherkinFeature.getChildren());
        setLocation(gherkinFeature.getLocation());
    }

    public ArmaFeature(@NonNull ArmaFeature feature){
        setKeyword(feature.getKeyword());
        setName(feature.getName());
        setDescription(feature.getDescription());
        setLocation(new ArmaLocation(feature.getLocation()));

        if (feature.hasTags()){
            feature.getTags().forEach(originTag -> addTag(new ArmaTag(originTag)));
        }

        if (feature.hasBackground()) {
            setBackground(new ArmaBackground(feature.getBackground()));
        }

        if (feature.hasScenarios()){
            feature.getScenarios().forEach(originScenario -> {
                if (originScenario instanceof ArmaScenarioOutline){
                    scenarios.add(new ArmaScenarioOutline((ArmaScenarioOutline) originScenario));
                } else {
                    scenarios.add(new ArmaScenario(originScenario));
                }
            });
        }
    }

    public void setGherkinTags(List<Tag> gherkinTags){
        if (CollectionUtils.isEmpty(gherkinTags)){
            return;
        }
        tags = gherkinTags.stream().map(ArmaTag::new).collect(Collectors.toSet());
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

    public void setLocation(ArmaLocation location){
        this.location = location;
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

    public void pullTags(){
        if (hasBackground()) {
            background.addTags(getTags());
        }
        scenarios.forEach(scenario -> scenario.addTags(getTags()));
    }

    public void optimizeTags(){
        if (hasBackground()) {
            background.removeTags(getTags());
        }
        scenarios.forEach(scenario -> scenario.removeTags(getTags()));
    }

    public ArmaFeature applyTagFilters(String... tagFilters){
        ArmaFeature newFeature = new ArmaFeature(this);
        newFeature.pullTags();

        if (newFeature.hasBackground() && !newFeature.getBackground().acceptTagFilters(tagFilters)){
            newFeature.setBackground(null);
        }

        newFeature.setScenarios(newFeature.getScenarios().stream()
                .filter(scenario -> scenario.acceptTagFilters(tagFilters))
                .collect(Collectors.toList()));

        newFeature. optimizeTags();
        return newFeature;
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

    public boolean hasBackground(){
        return background != null;
    }

    public boolean hasScenarios(){
        return CollectionUtils.isNotEmpty(scenarios);
    }

    public List<ArmaScenarioOutline> getScenarioOutlines(){
        return scenarios.stream()
            .filter(scenario -> scenario instanceof ArmaScenarioOutline)
            .map(scenario -> (ArmaScenarioOutline) scenario)
            .collect(Collectors.toList());
    }
}
