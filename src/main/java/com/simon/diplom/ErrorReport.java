package com.simon.diplom;

public class ErrorReport {
    private Integer lineNumber;
    private Integer inlineNumber;
    private ErrorType status;

    public ErrorReport(Integer line, Integer inlineNumber, ErrorType errorType){
        this.lineNumber = line;
        this.inlineNumber = inlineNumber;
        this.status = errorType;
    }

    @Override
    public String toString() {
        String errorDisc = "";
        switch (status){
            case EMPTY_NAME -> errorDisc = "пустое имя узла";
            case NO_SEPARATOR -> errorDisc = "отсутствует разделитель между входными связями и вероятностями";
            case UNKNOWN_CONNECTION -> errorDisc = "попытка создания связи с неизвестным узлом";
            case TYPE_MISMATCH_LINK -> errorDisc = "несоответствие типа данных в поле связей";
            case TYPE_MISMATCH_PROB -> errorDisc = "несоответствие типа данных в поле вероятностей";
            case PROB_NUM_MISMATCH -> errorDisc = "несоответствие размера ТУВ количеству входных узлов";
            case TO_BIG_PROB -> errorDisc = "слишком большая вероятность";
            case NEGATIVE_PROB -> errorDisc = "отрицательная вероятность";
            case NOT_ENOUGH_PARENTS_LN -> errorDisc = "у логического узла меньше 2 родительских узлов";
            case NOT_ENOUGH_CHILDREN_LN -> errorDisc = "у логического узла нет дочерних узлов";
            case LN_HAS_PROB_TABLE -> errorDisc = "задана таблица вероятностей для логического узла";
        }
        return "Ошибка: " + errorDisc + " в строке " + (lineNumber+1) + (inlineNumber != null ? " на позиции " + (inlineNumber+1) : "");
    }
}
