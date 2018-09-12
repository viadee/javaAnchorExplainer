package de.goerke.tobias.anchorj.util;

import java.util.Random;

/**
 * Used to generate noise.
 * <p>
 * Useful for e.g. predictions to be a little ambiguous
 */
public final class NoiseGenerator {

    public static double generateGaussianNoise(double mean, double variance) {
        return new Random().nextGaussian() * Math.sqrt(variance) + mean;
    }

    public static boolean addBooleanNoise(boolean initial, double changeChance) {
        return (new Random().nextFloat() < changeChance) != initial;
    }

    public static int addBooleanNoise(int initial, double changeChance) {
        return addBooleanNoise((initial != 0), changeChance) ? 1 : 0;
    }
}
