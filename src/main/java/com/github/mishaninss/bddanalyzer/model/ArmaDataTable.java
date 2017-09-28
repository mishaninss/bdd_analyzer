package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.DataTable;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Sergey_Mishanin on 11/17/16.
 */
@Data
public class ArmaDataTable {
    private List<ArmaTableRow> rows;

    public ArmaDataTable(){

    }

    public ArmaDataTable(ArmaDataTable dataTable){
        if (dataTable == null){
            return;
        }
        rows = new ArrayList<>();
        List<ArmaTableRow> originRows = dataTable.getRows();
        if (CollectionUtils.isNotEmpty(originRows)){
            originRows.forEach(originRow -> rows.add(new ArmaTableRow(originRow)));
        }
    }

    public ArmaDataTable(DataTable gherkinDataTable){
        if (gherkinDataTable == null || CollectionUtils.isEmpty(gherkinDataTable.getRows())){
            return;
        }
        rows = gherkinDataTable.getRows().stream().map(ArmaTableRow::new).collect(Collectors.toList());
    }

    public void addRow(ArmaTableRow row){
        if (rows == null){
            rows = new ArrayList<>();
        }
        rows.add(row);
    }

    public List<ArmaTableRow> getRows(){
        if (rows == null){
            rows = new ArrayList<>();
        }
        return rows;
    }

    public Map<String, Integer> getParametersUsage(){
        Map<String, Integer> paramsUsage = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(rows)){
            return paramsUsage;
        }
        rows.forEach(row -> {
            Map<String, Integer> rowParamUsage = row.getParametersUsage();
            rowParamUsage.forEach((key, value) -> {
                int paramUsage = paramsUsage.getOrDefault(key, 0) + value;
                paramsUsage.put(key, paramUsage);
            });
        });

        return paramsUsage;
    }

    public void applyParameter(String paramName, String value){
        if (CollectionUtils.isEmpty(getRows())){
            return;
        }
        getRows().forEach(row -> row.applyParameter(paramName, value));
    }

    public static ArmaDataTable applyParameter(ArmaDataTable dataTable, String paramName, String value){
        ArmaDataTable newDataTable = new ArmaDataTable(dataTable);
        newDataTable.applyParameter(paramName, value);
        return  newDataTable;
    }

    public void addRow(ArmaTableCell... cells){
        addRow(new ArmaTableRow(cells));
    }

    public void addRow(String... values){
        addRow(new ArmaTableRow(values));
    }

    public boolean isEmpty(){
        return CollectionUtils.isEmpty(rows);
    }

    @Override
    public String toString(){
        return toString(null);
    }

    public String toString(String separator){
        StringBuilder sb = new StringBuilder();

        if (CollectionUtils.isNotEmpty(rows)){
            List<Integer> width = calculateColumnsWidth(rows);
            rows.forEach(row ->
            {
                sb.append("\n")
                        .append(row.toString(width, separator));
            });
        }
        return sb.toString().trim();
    }

    private static List<Integer> calculateColumnsWidth(List<ArmaTableRow> rows){
        int cellCount = 0;
        if (CollectionUtils.isNotEmpty(rows)){
            if (CollectionUtils.isNotEmpty(rows.get(0).getCells())){
                cellCount = rows.get(0).getCells().size();
            }
        }
        List<Integer> width = new ArrayList<>(cellCount);
        for (int i=0; i<cellCount; i++){
            int maxLength = 0;
            if (CollectionUtils.isNotEmpty(rows)) {
                for (ArmaTableRow row : rows) {
                    String value = row.getCells().get(i).getValue();
                    if (value != null && value.length() > maxLength) {
                        maxLength = value.length();
                    }
                }
            }
            width.add(maxLength);
        }
        return width;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArmaDataTable)) return false;
        ArmaDataTable that = (ArmaDataTable) o;
        return Objects.equals(rows, that.rows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows);
    }
}
