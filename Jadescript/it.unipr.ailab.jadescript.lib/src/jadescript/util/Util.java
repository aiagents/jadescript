package jadescript.util;

public class Util {

    public static String quoteIfString(Object x) {
        if (x instanceof String) {
            return "\"" + x + "\"";
        } else {
            return String.valueOf(x);
        }
    }

}
