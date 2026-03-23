package com.simon.diplom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyMath {
    public static List<Integer> toBinary(int number, int size) {
        List<Integer> binaryDigits = new ArrayList<>();

        while (number > 0) {
            binaryDigits.add(number % 2);
            number /= 2;
        }

        while (binaryDigits.size()<size){
            binaryDigits.add(0);
        }

        Collections.reverse(binaryDigits);

        return binaryDigits;
    }

    public static int toDecimal(List<Integer> binaryDigits) {
        int result = 0;
        int size = binaryDigits.size();

        for (int i = 0; i < size; i++) {
            int bit = binaryDigits.get(i);
            result += bit * Math.pow(2, size - 1 - i);
        }

        return result;
    }

    public static String getStringFromBinary(List<Integer> binaryDigits, boolean addSpaces){
        StringBuilder returnString = new StringBuilder();
        for(int i = 0; i < binaryDigits.size(); i++){
            returnString.append(binaryDigits.get(i));
            if(addSpaces && i!= binaryDigits.size()-1){
                returnString.append(" ");
            }
        }
        return returnString.toString();
    }

    public static Double sumList(List<Double> list){
        Double sum = 0.0;
        for(Double i:list){
            sum+=i;
        }
        return sum;
    }

    public static void standardize(List<Double> list){
        Double sum = MyMath.sumList(list);
        if(sum!=0) {
            list.replaceAll(aDouble -> aDouble / sum);
        }
    }


    public static boolean isInteger(String line){
        try{
            Integer.parseInt(line);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }
    public static boolean isDouble(String line){
        try{
            Double.parseDouble(line);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }
}
