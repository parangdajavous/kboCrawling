package com.example.crawling_sampling.utils;


import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

public class util {


    /* 숫자 파싱 */
    public static int parseI(String s) {
        try {
            return parseInt(s.replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public static double parseD(String s) {
        try {
            return parseDouble(s.replace(",", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }


}
