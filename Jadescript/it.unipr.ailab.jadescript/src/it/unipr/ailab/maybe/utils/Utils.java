package it.unipr.ailab.maybe.utils;

public class Utils {
    private Utils(){} // Do not instantiate.

    public static int restrictInRange(int value, int min, int max){
        if(max>=min){
            throw new RuntimeException("The minimum value of the range has " +
                "to be strictly less than the max value");
        }

        return Math.min(max, Math.max(min, value));
    }

}
