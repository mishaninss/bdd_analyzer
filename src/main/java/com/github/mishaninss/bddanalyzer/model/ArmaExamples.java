package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.Examples;
import gherkin.ast.TableRow;
import gherkin.ast.Tag;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Objective representation of a Gherkin Examples
 * Created by Sergey_Mishanin on 11/16/16.
 * @see Examples
 */
@Data
public class ArmaExamples implements HasTags{
    private Set<ArmaTag> tags = new LinkedHashSet<>();
    private String keyword = "Examples";
    private String name = "";
    private String description;
    private ArmaTableRow tableHeader;
    private List<ArmaTableRow> tableBody;

    public ArmaExamples(){
    }

    public ArmaExamples(ArmaExamples originExamples){
        if (originExamples == null){
            return;
        }
        setKeyword(originExamples.getKeyword());
        setName(originExamples.getName());
        setDescription(originExamples.getDescription());
        setTags(originExamples.getTags().stream().map(ArmaTag::new).collect(Collectors.toList()));
        setTableHeader(new ArmaTableRow(originExamples.getTableHeader()));
        setTableBody(originExamples.getTableBody().stream().map(ArmaTableRow::new).collect(Collectors.toList()));
    }

    public ArmaExamples(Examples gherkinExamples){
        setGherkinTags(gherkinExamples.getTags());
        setKeyword(gherkinExamples.getKeyword());
        setName(gherkinExamples.getName());
        setDescription(gherkinExamples.getDescription());
        setTableHeader(gherkinExamples.getTableHeader());
        setGherkinTableBody(gherkinExamples.getTableBody());
    }

    public List<ArmaTableRow> getTableBody(){
        if (tableBody == null){
            tableBody = new ArrayList<>();
        }
        return tableBody;
    }

    public void removeColumn(int colIndex){
        tableHeader.removeCell(colIndex);
        if (CollectionUtils.isNotEmpty(tableBody)){
            tableBody.forEach(bodyRow -> bodyRow.removeCell(colIndex));
        }
    }

    public void removeColumns(int... colIndexes){
        for (int colIndex: colIndexes){
            removeColumn(colIndex);
        }
    }

    public void removeColumn(String paramName){
        int colIndex = getColumnIndex(paramName);
        if (colIndex>=0){
            removeColumn(colIndex);
        }
    }

    public void removeColumns(String... paramNames){
        for(String paramName: paramNames){
            removeColumn(paramName);
        }
    }

    public void removeColumns(Iterable<String> paramNames){
        for(String paramName: paramNames){
            removeColumn(paramName);
        }
    }

    public void removeDuplicatedRows(){
        if (CollectionUtils.isEmpty(tableBody) || tableBody.size() == 1){
            return;
        }
        Set<ArmaTableRow> rowSet = new LinkedHashSet<>(tableBody);
        tableBody.clear();
        tableBody.addAll(rowSet);
    }

    public int getColumnIndex(String paramName){
        if (StringUtils.isBlank(paramName) || tableHeader == null || CollectionUtils.isEmpty(tableHeader.getCells())){
            return -1;
        }
        List<ArmaTableCell> headerCells = tableHeader.getCells();
        for (int i=0; i<headerCells.size(); i++){
            if (paramName.equals(headerCells.get(i).getValue())){
                return i;
            }
        }
        return -1;
    }

    public String getParamName(int colIndex){
        if (colIndex < 0 || CollectionUtils.size(tableHeader.getCells()) <= colIndex){
            return null;
        }
        return tableHeader.getValue(colIndex);
    }

    public List<String> getParamNames(Iterable<Integer> colIndexes){
        List<String> paramNames = new ArrayList<>();
        if (colIndexes == null){
            return paramNames;
        }
        colIndexes.forEach(colIndex -> paramNames.add(getParamName(colIndex)));
        return paramNames;
    }

    public Map<String, Integer> getColumnIndexes(Iterable<String> paramNames){
        Map<String, Integer> colIndexes = new LinkedHashMap<>();
        if (IterableUtils.isEmpty(paramNames)){
            return colIndexes;
        }
        paramNames.forEach(paramName -> colIndexes.put(paramName, getColumnIndex(paramName)));
        return colIndexes;
    }

    public List<Integer> findConstantColumns(){
        List<Integer> colIndexes = new LinkedList<>();
        if (CollectionUtils.isEmpty(tableBody)){
            return colIndexes;
        }
        int colCount = CollectionUtils.size(tableBody.get(0).getCells());
        for (int i=0; i<colCount; i++){
            boolean constant = true;
            String value = tableBody.get(0).getValue(i);
            for (int j=1; j<tableBody.size(); j++){
                if (!value.equals(tableBody.get(j).getValue(i))) {
                    constant = false;
                    break;
                }
            }
            if (constant){
                colIndexes.add(i);
            }
        }
        return colIndexes;
    }

    public int addColumn(String paramName, String value){
        int colIndex = getColumnIndex(paramName);
        if (colIndex == -1){
            ArmaTableRow header = getTableHeader();
            colIndex = header.addCell(paramName);
            List<ArmaTableRow> body = getTableBody();
            if (CollectionUtils.isNotEmpty(body)){
                body.forEach(row -> row.addCell(value));
            } else {
                ArmaTableRow row = new ArmaTableRow(header.getSize());
                row.setValue(colIndex, value);
                body.add(row);
            }
        }
        return colIndex;
    }

    public void addBodyRow(ArmaTableRow row){
        getTableBody().add(new ArmaTableRow(row));
    }

    public void addBodyRows(List<ArmaTableRow> rows){
        rows.forEach(this::addBodyRow);
    }

    public String getValue(String paramName, int rowIndex){
        if (StringUtils.isBlank(paramName) || CollectionUtils.size(tableBody) <= rowIndex){
            return null;
        } else {
            int colIndex = getColumnIndex(paramName);
            return getValue(colIndex, rowIndex);
        }
    }

    public String getValue(int colIndex, int rowIndex){
        if (colIndex < 0 || rowIndex < 0 || colIndex >= getTableHeader().getSize() || getTableBody().size() <= rowIndex){
            return null;
        } else {
            return getTableBody().get(rowIndex).getValue(colIndex);
        }
    }

    public ArmaExamples mergeTo(ArmaExamples anotherExamples){
        ArmaExamples newExamples = new ArmaExamples(this);
        newExamples.setTableHeader(newExamples.getTableHeader().mergeValues(anotherExamples.getTableHeader()));
        if (newExamples.getTableHeader().equals(anotherExamples.getTableHeader())){
            newExamples.getTableBody().addAll(anotherExamples.getTableBody());
        } else {
            anotherExamples.getTableBody().forEach(row -> {
                ArmaTableRow newRow = new ArmaTableRow(newExamples.getTableHeader().getSize());
                for (int i=0; i<row.getSize(); i++){
                    String paramName = anotherExamples.getParamName(i);
                    String value = row.getValue(i);
                    int newColIndex = newExamples.getColumnIndex(paramName);
                    newRow.setValue(newColIndex, value);
                }
            });
        }
        newExamples.removeDuplicatedRows();
        return newExamples;
    }

    public ArmaExamples joinWith(ArmaExamples anotherExamples){
        setTableHeader(getTableHeader().mergeValues(anotherExamples.getTableHeader()));
        if (getTableHeader().equals(anotherExamples.getTableHeader())){
            addBodyRows(anotherExamples.getTableBody());
        } else {
            anotherExamples.getTableBody().forEach(row -> {
                ArmaTableRow newRow = new ArmaTableRow(getTableHeader().getSize());
                for (int i=0; i<row.getSize(); i++){
                    String paramName = anotherExamples.getParamName(i);
                    if (StringUtils.isNoneBlank(paramName)) {
                        String value = row.getValue(i);
                        int newColIndex = getColumnIndex(paramName);
                        newRow.setValue(newColIndex, value);
                    }
                }
            });
        }
        removeDuplicatedRows();
        adjustTableSize();
        addTags(anotherExamples.getTags());
        return this;
    }

    public static ArmaExamples join(ArmaExamples to, ArmaExamples from){
        if (to == null && from == null){
             return new ArmaExamples();
        } else if (to == null) {
            return new ArmaExamples(from);
        } else if (from == null){
            return new ArmaExamples(to);
        } else {
            return new ArmaExamples(to).joinWith(from);
        }
    }

    public static ArmaExamples join(Iterable<ArmaExamples> examples){
        if (IterableUtils.isEmpty(examples)){
            return new ArmaExamples();
        } else if (IterableUtils.size(examples) == 1){
            return new ArmaExamples(examples.iterator().next());
        } else {
            Iterator<ArmaExamples> iterator = examples.iterator();
            ArmaExamples to = iterator.next();
            while (iterator.hasNext()){
                ArmaExamples from = iterator.next();
                to = join(to, from);
            }
            return to;
        }
    }

    public Map<String, String> toMap(int rowIndex){
        Map<String, String> values = new LinkedHashMap<>();
        if (rowIndex >= CollectionUtils.size(tableBody)){
            return values;
        }
        ArmaTableRow row = tableBody.get(rowIndex);
        for (int i=0; i<tableHeader.getSize(); i++){
            values.put(tableHeader.getValue(i), row.getValue(i));
        }
        return values;
    }

    public Map<String, String> toMap(int rowIndex, Iterable<String> paramNames){
        Map<String, String> values = new LinkedHashMap<>();
        if (rowIndex >= CollectionUtils.size(tableBody)){
            return values;
        }
        ArmaTableRow row = tableBody.get(rowIndex);
        Map<String, Integer> columnIndexes = getColumnIndexes(paramNames);
        columnIndexes.forEach((paramName, columnIndex) -> values.put(paramName, row.getValue(columnIndex)));
        return values;
    }

    public boolean hasTheSameScopeWith(ArmaExamples anoterExamples){
        return CollectionUtils.isEqualCollection(getTags(), anoterExamples.getTags());
    }

    public void adjustTableSize(){
        getTableBody().forEach(row -> row.adjustSize(getTableHeader().getSize()));
    }

    public void setKeyword(String keyword){
        this.keyword = StringUtils.trim(keyword);
    }

    public void setName(String name){
        this.name = StringUtils.normalizeSpace(StringUtils.trim(name));
    }

    public void setDescription(String description){
        this.description = StringUtils.normalizeSpace(StringUtils.trim(description));
    }

    public void setGherkinTags(List<Tag> gherkinTags){
        if (CollectionUtils.isEmpty(gherkinTags)){
            return;
        }
        setTags(gherkinTags.stream().map(ArmaTag::new).collect(Collectors.toList()));
    }

    public void setTableHeader(TableRow gherkinTableHeader){
        this.tableHeader = new ArmaTableRow(gherkinTableHeader);
    }

    public void setTableHeader(ArmaTableRow tableHeader){
        this.tableHeader = tableHeader;
    }

    public void setGherkinTableBody(List<TableRow> gherkinTableBody){
        if (CollectionUtils.isEmpty(gherkinTableBody)){
            return;
        }
        this.tableBody = gherkinTableBody.stream().map(ArmaTableRow::new).collect(Collectors.toList());
        adjustBodyColumns();
    }

    public void adjustBodyColumns(){
        if (CollectionUtils.size(tableBody) <= 1){
            return;
        }

        int maxColumnCount = getMaxBodyColumnsCount();
        tableBody.forEach(row -> {
            int delta = maxColumnCount - row.getSize();
            for (int i=0; i<delta; i++){
                row.addCell("");
            }
        });
    }

    public int getMaxBodyColumnsCount(){
        if (CollectionUtils.isEmpty(tableBody)){
            return 0;
        }
        return tableBody.stream()
                .map(ArmaTableRow::getSize)
                .collect(Collectors.summarizingInt(Integer::valueOf)).getMax();
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

        List<Integer> width = calculateColumnsWidth(tableHeader, tableBody);
        if (tableHeader != null){
            sb.append("\n")
                .append(ArmaFeature.addPad(tableHeader.toString(width)));
        }
        if (tableBody != null){
            tableBody.forEach(row ->
            {
                sb.append("\n")
                    .append(ArmaFeature.addPad(row.toString(width)));
            });
        }
        return sb.toString();
    }

    private static List<Integer> calculateColumnsWidth(ArmaTableRow headers, List<ArmaTableRow> tableBody){
        int cellCount = 0;
        if (CollectionUtils.isNotEmpty(headers.getCells())){
            cellCount = headers.getCells().size();
        } else if (CollectionUtils.isNotEmpty(tableBody)){
            if (CollectionUtils.isNotEmpty(tableBody.get(0).getCells())){
                cellCount = tableBody.get(0).getCells().size();
            }
        }
        List<Integer> width = new ArrayList<>(cellCount);
        for (int i=0; i<cellCount; i++){
            int maxLength = 0;
            if (CollectionUtils.isNotEmpty(headers.getCells())) {
                maxLength = headers.getCells().get(i).getValue().length();
            }
            if (CollectionUtils.isNotEmpty(tableBody)) {
                for (ArmaTableRow row : tableBody) {
                    String value = row.getValue(i);
                    if (value != null && value.length() > maxLength) {
                        maxLength = value.length();
                    }
                }
            }

            width.add(maxLength);
        }
        return width;
    }
}
