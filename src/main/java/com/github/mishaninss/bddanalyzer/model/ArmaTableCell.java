package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.TableCell;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sergey_Mishanin on 11/16/16.
 */
@Data
public class ArmaTableCell {
    private String value = "";

    public ArmaTableCell() {

    }

    public ArmaTableCell(ArmaTableCell cell) {
        if (cell == null) {
            return;
        }
        value = cell.getValue();
    }

    public ArmaTableCell(TableCell cell) {
        value = cell.getValue();
    }

    public ArmaTableCell(String value) {
        this.value = value;
    }

    public Map<String, Integer> getParametersUsage() {
        return ArmaScenarioOutline.getParametersUsage(value);
    }

    public void applyParameter(String paramName, String value) {
        if (StringUtils.isNoneBlank(getValue())) {
            try {
                setValue(getValue().replaceAll(Pattern.quote("<" + paramName + ">"), Matcher.quoteReplacement(value)));
            }catch (Exception ex){
                System.out.println(paramName);
                System.out.println(value);
                System.out.println(getValue());
                ex.printStackTrace();
            }
        }
    }

    public static ArmaTableCell applyParameter(ArmaTableCell cell, String paramName, String value) {
        ArmaTableCell newCell = new ArmaTableCell(cell);
        newCell.applyParameter(paramName, value);
        return newCell;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArmaTableCell)) return false;
        ArmaTableCell that = (ArmaTableCell) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
