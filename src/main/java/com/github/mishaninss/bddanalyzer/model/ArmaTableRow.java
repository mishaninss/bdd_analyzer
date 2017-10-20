package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.TableRow;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Sergey_Mishanin on 11/16/16.
 */
@Data
public class ArmaTableRow {

    private static final String SEPARATOR = "|";
    private List<ArmaTableCell> cells = new ArrayList<>();

    public ArmaTableRow(){

    }

    public ArmaTableRow(ArmaTableRow row){
        if (row == null){
            return;
        }
        cells = new ArrayList<>();
        List<ArmaTableCell> originCells = row.getCells();
        if (CollectionUtils.isNotEmpty(originCells)){
            originCells.forEach(originCell -> cells.add(new ArmaTableCell(originCell)));
        }
    }

    public ArmaTableRow(TableRow row){
        if (row == null || CollectionUtils.isEmpty(row.getCells())){
            return;
        }
        this.cells = row.getCells().stream().map(ArmaTableCell::new).collect(Collectors.toList());
    }

    public ArmaTableRow(ArmaTableCell... cells){
        this.cells = new ArrayList<>();
        this.cells.addAll(Arrays.asList(cells));
    }

    public ArmaTableRow(String... values){
        for (String value: values){
            addCell(value);
        }
    }

    public ArmaTableRow(int size){
        for (int i=0; i<size; i++){
            addCell("");
        }
    }

    public int addCell(ArmaTableCell cell){
        getCells().add(cell);
        return cells.size();
    }

    public void removeCell(int index){
        if (CollectionUtils.isNotEmpty(cells) && cells.size()>index){
            cells.remove(index);
        }
    }

    public void setValue(int colIndex, String value){
        if (colIndex < 0 ) {
            return;
        }
        adjustSize(colIndex);
        cells.get(colIndex).setValue(value);
    }

    public void adjustSize(int size){
        List<ArmaTableCell> thisCells = getCells();
        while (thisCells.size() < size){
            addCell("");
        }
    }

    public Map<String, Integer> getParametersUsage(){
        Map<String, Integer> paramsUsage = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(cells)){
            return paramsUsage;
        }
        cells.forEach(cell -> {
            Map<String, Integer> stepParamUsage = cell.getParametersUsage();
            stepParamUsage.forEach((key, value) -> {
                int paramUsage = paramsUsage.getOrDefault(key, 0) + value;
                paramsUsage.put(key, paramUsage);
            });
        });

        return paramsUsage;
    }

    public void applyParameter(String paramName, String value){
        if (CollectionUtils.isEmpty(cells)){
            return;
        }
        cells.forEach(cell -> cell.applyParameter(paramName, value));
    }

    public static ArmaTableRow applyParameter(ArmaTableRow tableRow, String paramName, String value){
        ArmaTableRow newTableRow = new ArmaTableRow(tableRow);
        tableRow.applyParameter(paramName, value);
        return newTableRow;
    }

    public String getValue(int colIndex){
        if (colIndex < 0 || colIndex >= CollectionUtils.size(cells)){
            return null;
        }
        return cells.get(colIndex).getValue();
    }

    public List<String> getValues(){
        List<String> values = new ArrayList<>();
        cells.forEach(cell -> values.add(cell.getValue()));
        return values;
    }

    public List<Integer> findValue(String value){
        List<Integer> colIndexes = new ArrayList<>();
        if (CollectionUtils.isEmpty(cells)){
            return colIndexes;
        }
        for (int i=0; i<cells.size(); i++){
            ArmaTableCell cell = cells.get(i);
            if (StringUtils.equals(value, cell.getValue())){
                colIndexes.add(i);
            }
        }
        return colIndexes;
    }

    public boolean containsValue(String value){
        return CollectionUtils.isNotEmpty(findValue(value));
    }

    public ArmaTableRow mergeValues(ArmaTableRow anotherRow){
        ArmaTableRow newRow = new ArmaTableRow(this);
        if (anotherRow == null || CollectionUtils.isEmpty(anotherRow.getCells())){
            return newRow;
        }
        anotherRow.getCells().stream()
                .map(ArmaTableCell::getValue)
                .forEach(value -> {
                    if (!newRow.containsValue(value)){
                        newRow.addCell(value);
                    }
                });
        return newRow;
    }

    public int addCell(String value){
        return addCell(new ArmaTableCell(value));
    }

    public int addCell(Object value){
        return addCell(new ArmaTableCell(value.toString()));
    }

    @Override
    public String toString(){
        if (CollectionUtils.isEmpty(cells)){
            return "";
        }

        List<String> values = cells.stream().map(ArmaTableCell::getValue).collect(Collectors.toList());
        return StringUtils.wrap(StringUtils.join(values, "|"), "|");
    }

    public int getSize(){
        return CollectionUtils.size(cells);
    }

    public String toString(List<Integer> width){
        return toString(width, SEPARATOR);
    }

    public String toString(List<Integer> width, String separator){
        if (separator == null) {
            separator = SEPARATOR;
        }
        if (CollectionUtils.isEmpty(cells)){
            return "";
        }

        List<String> values = new ArrayList<>();
        for (int i=0; i<cells.size(); i++){
            int w = width != null && width.size() >= i ? width.get(i) : 0;
            values.add(StringUtils.rightPad(cells.get(i).getValue(), w));
        }
        return StringUtils.wrap(StringUtils.join(values, separator), separator).trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArmaTableRow)) return false;
        ArmaTableRow that = (ArmaTableRow) o;
        return Objects.equals(cells, that.cells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cells);
    }
}
