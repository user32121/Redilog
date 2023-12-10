package redilog.utils;

public class MathUtil {
    /**
     * @return the item with a larger magnitude, preserving sign
     */
    public static double signMax(double a, double b) {
        return (Math.abs(a) >= Math.abs(b)) ? a : b;
    }

    /**
     * @return the item with a smaller magnitude, preserving sign
     */
    public static double signMin(double a, double b) {
        return (Math.abs(a) <= Math.abs(b)) ? a : b;
    }
}
