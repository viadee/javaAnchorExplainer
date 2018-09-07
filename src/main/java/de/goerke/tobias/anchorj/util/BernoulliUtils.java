package de.goerke.tobias.anchorj.util;

/**
 * Providing functions for calculating UCB bounds.
 * <p>
 * Used to balance exploration vs. exploitation.
 */
public final class BernoulliUtils {

    private BernoulliUtils() {
    }

    static double klBernoulli(double p, double q) {
        p = Math.min(0.9999999999999999, Math.max(0.0000001, p));
        q = Math.min(0.9999999999999999, Math.max(0.0000001, q));
        return (p * Math.log((p) / q) + (1 - p) * Math.log((1 - p) / (1 - q)));
    }

    public static double dupBernoulli(double p, double level) {
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
