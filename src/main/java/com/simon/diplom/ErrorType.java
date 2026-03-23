package com.simon.diplom;

public enum ErrorType {
    EMPTY_NAME, //Пустое имя
    NO_SEPARATOR, //Отсутствие разделителя между входными связями и вероятностями
    TYPE_MISMATCH_LINK, //Не-int-овый тип данный в линке
    TYPE_MISMATCH_PROB, //Не-double-овый тип данных в вероятности
    UNKNOWN_CONNECTION, //Попытка добавить в предки ещё не прочитанный узел
    PROB_NUM_MISMATCH, //Несоответствие размеров ТУВ и количества входных связей
    TO_BIG_PROB, //Вероятность слишком велика
    NEGATIVE_PROB, //Отрицательная вероятность
    NOT_ENOUGH_PARENTS_LN, //У логического узла не хватает родительских узлов
    NOT_ENOUGH_CHILDREN_LN, //У логического узла не хватает дочерних узлов
    LN_HAS_PROB_TABLE //Для логического узла задана таблица вероятностей
}
