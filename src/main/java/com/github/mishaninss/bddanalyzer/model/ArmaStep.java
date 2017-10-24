package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.DataTable;
import gherkin.ast.DocString;
import gherkin.ast.Step;
import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sergey_Mishanin on 11/16/16.
 */
@Data
public class ArmaStep {
    private String keyword;
    private String text;
    private ArmaStepDef stepDef;
    private Object argument;
    private ArmaLocation location;

    public ArmaStep(){

    }

    public ArmaStep(String text){
        keyword = "When";
        this.text = text;
    }

    public ArmaStep(Step gherkinStep){
        setKeyword(gherkinStep.getKeyword());
        setText(gherkinStep.getText());
        setLocation(new ArmaLocation(gherkinStep.getLocation()));

        Object gherkinArgument = gherkinStep.getArgument();
        if (gherkinArgument != null) {
            if (gherkinArgument instanceof DataTable){
                argument = new ArmaDataTable((DataTable) gherkinArgument);
            } else if (gherkinArgument instanceof DocString){
                argument = new ArmaDocString((DocString) gherkinArgument);
            }
        }
    }

    public ArmaStep(ArmaStep step){
        setKeyword(step.getKeyword());
        setText(step.getText());
        setLocation(new ArmaLocation(step.getLocation()));
        setStepDef(step.getStepDef());

        Object originArgument = step.getArgument();
        if (originArgument != null) {
            if (originArgument instanceof ArmaDataTable){
                argument = new ArmaDataTable((ArmaDataTable) originArgument);
            } else if (originArgument instanceof ArmaDocString){
                argument = new ArmaDocString((ArmaDocString) originArgument);
            }
        }
    }

    public boolean containsParameter(String paramName){
        return getParametersUsage().keySet().contains(paramName);
    }

    public void setKeyword(String keyword){
        this.keyword = StringUtils.trim(keyword);
    }

    public void setText(String text){
        this.text = StringUtils.trim(text);
    }

    public boolean isImplemented(){
        return stepDef != null && stepDef.isImplemented();
    }

    public void applyExample(ArmaExamples examples, int rowIndex){
        Map<String,String> values = examples.toMap(rowIndex);
        if (MapUtils.isEmpty(values)){
            return;
        }
        applyParameters(values);
    }

    public static ArmaStep applyExample(ArmaStep step, ArmaExamples examples, int rowIndex){
        Map<String,String> values = examples.toMap(rowIndex);
        return applyParameters(step, values);
    }

    public void applyParameter(String paramName, String value){
        setText(getText().replaceAll(Pattern.quote("<" + paramName + ">"), Matcher.quoteReplacement(value)));

        if (argument != null && argument instanceof ArmaDataTable){
            ((ArmaDataTable)argument).applyParameter(paramName, value);
        }
    }

    public void applyParameters(Map<String, String> values){
        if (MapUtils.isEmpty(values)){
            return;
        }
        values.forEach(this::applyParameter);
    }

    public static ArmaStep applyParameters(ArmaStep step, Map<String, String> values){
        ArmaStep newStep = new ArmaStep(step);
        newStep.applyParameters(values);
        return newStep;
    }

    public static ArmaStep applyParameter(ArmaStep step, String paramName, String value){
        ArmaStep newStep = new ArmaStep(step);
        newStep.applyParameter(paramName, value);
        return newStep;
    }

    public boolean isFullyEqualTo(ArmaStep anotherStep){
        return text.equals(anotherStep.getText());
    }

    public boolean isEqualToIgnoreParameters(ArmaStep anotherStep){
        if (stepDef != null && anotherStep.getStepDef() != null) {
            return stepDef.getText().equals(anotherStep.getStepDef().getText());
        } else
            return stepDef == null && anotherStep.getStepDef() == null && isFullyEqualTo(anotherStep);
    }

    public Map<String, Integer> getParametersUsage(){
        List<Map<String, Integer>> paramsData = new LinkedList<>();
        if (StringUtils.isNoneBlank(text)){
            paramsData.add(ArmaScenarioOutline.getParametersUsage(text));
        }
        if (argument != null && argument instanceof ArmaDataTable){
            paramsData.add(((ArmaDataTable)argument).getParametersUsage());
        }
        return ArmaScenarioOutline.mergeParametersUsage(paramsData);
    }

    @Override
    public String toString(){
        return toString(false);
    }

    public String toString(boolean fullInfo){
        StringBuilder sb = new StringBuilder();
        sb.append(keyword).append(" ").append(text);
        if (fullInfo) {
            if (stepDef == null) {
                sb.append(" # NOT IMPLEMENTED");
            } else {
                sb.append(" # IMPLEMENTED IN ").append(stepDef.getLocation());
            }
        }

        if (argument != null){
            if (argument instanceof ArmaDataTable){
                sb.append("\n")
                        .append(ArmaFeature.addPad(argument.toString()));
            } else if (argument instanceof ArmaDocString){
                sb.append("\n")
                        .append(ArmaFeature.addPad(argument.toString()));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArmaStep)) return false;
        ArmaStep armaStep = (ArmaStep) o;
        return Objects.equals(keyword, armaStep.keyword) &&
                Objects.equals(text, armaStep.text) &&
                Objects.equals(argument, armaStep.argument) &&
                Objects.equals(location, armaStep.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyword, text, argument, location);
    }
}
