package de.goerke.tobias.anchorj.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Some basic tests to ensure correct Bernoulli functioning
 */
class KLBernoulliUtilsTest {

    @Test
    void k1_bernoulli() {
        Assertions.assertEquals(KLBernoulliUtils.klBernoulli(1.0, 0.5), 0.6931471805599411);
        assertEquals(KLBernoulliUtils.klBernoulli(1.0, 0.25), 1.3862943611198864);
        assertEquals(KLBernoulliUtils.klBernoulli(1.0, 0.125), 2.0794415416798313);
        assertEquals(KLBernoulliUtils.klBernoulli(1.0, 0.0625), 2.7725887222397767);
        assertEquals(KLBernoulliUtils.klBernoulli(0.5490196078431373, 0.37898193030656757), 0.05920671257767904);
    }

    @Test
    void dup_bernoulli() {
        assertEquals(KLBernoulliUtils.dupBernoulli(0.2, 1.3926867786435662), 0.9037841796874999);
        assertEquals(KLBernoulliUtils.dupBernoulli(0.391304347826087, 0.6055159907145939), 0.8648085989678865);
        assertEquals(KLBernoulliUtils.dupBernoulli(0.391304347826087, 0.6414609762718735), 0.8733629578679287);
        assertEquals(KLBernoulliUtils.dupBernoulli(0.4, 0.2458933742375515), 0.7343833192766789);
        assertEquals(KLBernoulliUtils.dupBernoulli(0.4, 0.25392301891743857), 0.7390107435128799);
    }

    @Test
    void dlow_bernoulli() {
        assertEquals(KLBernoulliUtils.dlowBernoulli(1.0, 2.995732273553991), 0.04998779296875);
        assertEquals(KLBernoulliUtils.dlowBernoulli(0.5490196078431373, 0.05873984850105864), 0.3796278349933222);
        assertEquals(KLBernoulliUtils.dlowBernoulli(0.45161290322580644, 0.44925379956244066), 0.09032809349798387);
        assertEquals(KLBernoulliUtils.dlowBernoulli(0.5, 2.32114463107261), 0.002410888671875);
        assertEquals(KLBernoulliUtils.dlowBernoulli(0.5106382978723404, 0.2963163358816098), 0.17283491607411444);
    }

    @Test
    void compute_beta() {
        assertEquals(KLBernoulliUtils.computeBeta(12, 1, 0.05), 13.926867786435661);
        assertEquals(KLBernoulliUtils.computeBeta(12, 2, 0.05), 14.753602454253091);
        assertEquals(KLBernoulliUtils.computeBeta(12, 3, 0.05), 15.235381135046314);
        assertEquals(KLBernoulliUtils.computeBeta(12, 4, 0.05), 15.576454429018908);
        assertEquals(KLBernoulliUtils.computeBeta(12, 5, 0.05), 15.84060245509722);
    }

}