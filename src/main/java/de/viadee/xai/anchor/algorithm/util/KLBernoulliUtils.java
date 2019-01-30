package de.viadee.xai.anchor.algorithm.util;

/**
 * Providing functions for calculating Kullback-Leibler (KL) divergences
 * <p>
 * Used for bandits using KL divergences
 */
public final class KLBernoulliUtils {

    private KLBernoulliUtils() {
    }

    static double klBernoulli(double p, double q) {
        p = Math.min(0.9999999999999999, Math.max(0.0000001, p));
        q = Math.min(0.9999999999999999, Math.max(0.0000001, q));
        return (p * Math.log(p / q) + (1 - p) * Math.log((1 - p) / (1 - q)));
    }

    public static double dupBernoulli(final double p, final double level) {
        double lm = p;
        double um = Math.min(Math.min(1, p + Math.sqrt(level / 2)), 1);
        for (int i = 1; i < 17; i++) {
            final double qm = (um + lm) / 2;
            if (klBernoulli(p, qm) > level)
                um = qm;
            else
                lm = qm;
        }
        return um;
    }

    public static double dlowBernoulli(final double p, final double level) {
        double um = p;
        double lm = Math.max(Math.min(1, p - Math.sqrt(level / 2)), 0);
        for (int i = 1; i < 17; i++) {
            final double qm = (um + lm) / 2;
            if (klBernoulli(p, qm) > level)
                lm = qm;
            else
                um = qm;
        }
        return lm;
    }

    public static double computeBeta(final int nFeatures, final int t, final double delta) {
        final double alpha = 1.1;
        final double k = 405.5;
        final double temp = Math.log(k * nFeatures * Math.pow(t, alpha) / delta);
        return temp + Math.log(temp);
    }
}
