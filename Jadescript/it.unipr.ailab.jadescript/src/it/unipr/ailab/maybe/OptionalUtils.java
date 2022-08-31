package it.unipr.ailab.maybe;

import java.util.Optional;

/**
 * Created on 2019-06-12.
 */
public final class OptionalUtils {
    private OptionalUtils(){}

    public static boolean areAllPresent(Optional<?>... opts){
        for (Optional<?> opt : opts) {
            if (opt.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
