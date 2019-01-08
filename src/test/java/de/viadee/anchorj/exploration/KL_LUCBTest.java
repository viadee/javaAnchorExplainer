package de.viadee.anchorj.exploration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import de.viadee.anchorj.AnchorCandidate;
import de.viadee.anchorj.execution.SamplingService;
import de.viadee.anchorj.execution.SamplingSession;
import de.viadee.anchorj.execution.sampling.SamplingFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test compares results with these of the original Anchor implementation
 */
public class KL_LUCBTest {
    private static SamplingService mockIdentify(int[] predictionsToReturn) {
        AtomicInteger currentIndex = new AtomicInteger(0);
        BiFunction<AnchorCandidate, Integer, Double> function = (a, b) -> {
            int prediction = predictionsToReturn[currentIndex.getAndIncrement()];

            a.registerSamples(b, prediction);

            return 0D;
        };

        return new SamplingService() {
            private static final long serialVersionUID = -1015790616477178369L;

            @Override
            public double getTimeSpentSampling() {
                return 0;
            }

            @Override
            public int getSamplesTakenCount() {
                return 0;
            }

            @Override
            public SamplingSession createSession(int explainedInstanceLabel) {
                return new SamplingSession() {
                    private static final long serialVersionUID = -2995382802085107059L;

                    @Override
                    public SamplingSession registerCandidateEvaluation(AnchorCandidate candidate, int count) {
                        function.apply(candidate, count);
                        return this;
                    }

                    @Override
                    public void run() {

                    }
                };
            }

            @Override
            public SamplingService notifySamplingFunctionChange(SamplingFunction samplingFunction) {
                return null;
            }
        };
    }

    @Test
    @SuppressWarnings("deprecation")
    void updateBoundsTest() {
        int[] result = mockUpdateBounds(1, 5, new int[]{1, 1, 1, 1, 1},
                new double[]{0, 1, 1, 1, 0}, 1, new double[]{0, 0, 0, 0, 0},
                new double[]{0, 0, 0, 0, 0});
        assertEquals(result[0], 0);
        assertEquals(result[1], 3);

        result = mockUpdateBounds(2, 6, new int[]{101, 1, 1, 101, 1, 1},
                new double[]{0.45544554, 1, 1, 0.82178218, 0, 0}, 1, new double[]{1, 1, 1, 0, 1, 1},
                new double[]{0, 0, 0, 0, 0, 0});
        assertEquals(result[0], 4);
        assertEquals(result[1], 2);

        result = mockUpdateBounds(5, 5, new int[]{101, 101, 101, 301, 201},
                new double[]{0.45544554, 0.4950495, 0.4950495, 0.8372093, 0.66666667}, 1,
                new double[]{0.70931609, 0.74302866, 0.74302866, 0.95871969, 0.82283715},
                new double[]{0, 0, 0, 0.70518634, 0});
        assertEquals(result[0], 4);
        assertEquals(result[1], 3);

        result = mockUpdateBounds(2, 6, new int[]{101, 1, 1, 101, 1, 1},
                new double[]{0.11881188, 1, 1, 0.76237624, 0, 0}, 1,
                new double[]{1, 1, 1, 0, 1, 1},
                new double[]{0, 0, 0, 0, 0, 0});
        assertEquals(result[0], 4);
        assertEquals(result[1], 2);
    }

    private int[] mockUpdateBounds(int t, int nFeatures, int[] nSamples, double[] means, int topN, double[] ub, double[] lb) {
        KL_LUCB kl_lucb = new KL_LUCB(100);
        List<AnchorCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < nSamples.length; i++) {
            int positiveSamples = (int) (means[i] * nSamples[i]);
            AnchorCandidate anchorCandidate = new AnchorCandidate(Collections.singletonList(1), null);
            anchorCandidate.registerSamples(nSamples[i], positiveSamples);
            candidates.add(anchorCandidate);
        }
        return kl_lucb.updateBounds(t, candidates, 0.1, topN, ub, lb);
    }

    // Due to refactoring not applicable anymore
    private List<AnchorCandidate> mockCandidates(List<Integer[]> features, int[] nCandidates, int[] positives) {
        List<AnchorCandidate> result = new ArrayList<>();
        for (int i = 0; i < features.size(); i++) {
            AnchorCandidate candidate = createFakeParentCandidate(Arrays.asList(features.get(i)));
            candidate.registerSamples(nCandidates[i], positives[i]);
            result.add(candidate);
        }
        return result;
    }

    private AnchorCandidate createFakeParentCandidate(List<Integer> features) {
        List<Integer> previousFeatures = new ArrayList<>(features);
        // We don't really care if this is the actual parent. We just need it to pass validation
        if (previousFeatures.size() > 0)
            previousFeatures.remove(0);
        else
            return null;
        return new AnchorCandidate(features, createFakeParentCandidate(previousFeatures));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testBestCandidate() {
        List<AnchorCandidate> candidates = mockCandidates(Arrays.asList(
                new Integer[]{0},
                new Integer[]{1},
                new Integer[]{2},
                new Integer[]{3},
                new Integer[]{4},
                new Integer[]{5}),
                new int[]{0, 0, 0, 0, 0, 1},
                new int[]{0, 0, 0, 0, 0, 0});
        List<AnchorCandidate> result = new KL_LUCB(100).identify(candidates, mockIdentify(new int[]{0, 1, 1, 1, 0, 12, 76, 17, 18, 17, 33}), 0, 0.1, 0.15, 1);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), candidates.get(3));
        /***/
        candidates = mockCandidates(Arrays.asList(
                new Integer[]{0},
                new Integer[]{1},
                new Integer[]{2},
                new Integer[]{3},
                new Integer[]{4},
                new Integer[]{5}),
                new int[]{13, 10, 24, 5, 6, 101}, new int[]{10, 7, 22, 2, 5, 85});
        result = new KL_LUCB(100).identify(candidates, mockIdentify(new int[]{90, 83, 92, 83, 64, 93, 38, 89, 82, 91, 83, 85}), 0, 0.1, 0.15, 1);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), candidates.get(0));
        /***/
        candidates = mockCandidates(Arrays.asList(
                new Integer[]{0, 1},
                new Integer[]{0, 3},
                new Integer[]{0, 5},
                new Integer[]{0, 2},
                new Integer[]{0, 4}),
                new int[]{35, 14, 513, 139, 30}, new int[]{29, 10, 460, 128, 29});
        result = new KL_LUCB(100).identify(candidates, mockIdentify(new int[]{51, 92, 77, 90, 95, 88}), 0, 0.1, 0.15, 1);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), candidates.get(4));
        /***/
        candidates = mockCandidates(Arrays.asList(
                new Integer[]{0, 4, 5},
                new Integer[]{0, 3, 4},
                new Integer[]{0, 2, 4},
                new Integer[]{0, 1, 4}),
                new int[]{330, 9, 97, 25}, new int[]{306, 8, 92, 24});
        result = new KL_LUCB(100).identify(candidates, mockIdentify(new int[]{76, 81, 87, 87, 88, 91}), 0, 0.1, 0.15, 1);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), candidates.get(0));
        /***/
        candidates = mockCandidates(Arrays.asList(
                new Integer[]{0, 2, 4, 5},
                new Integer[]{0, 3, 4, 5},
                new Integer[]{0, 1, 4, 5}),
                new int[]{150, 15, 41}, new int[]{138, 11, 38});
        result = new KL_LUCB(100).identify(candidates, mockIdentify(new int[]{74, 83, 88, 92}), 0, 0.1, 0.15, 1);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), candidates.get(0));
        /***/
        candidates = mockCandidates(Arrays.asList(
                new Integer[]{0, 2, 3, 4, 5},
                new Integer[]{0, 1, 2, 4, 5}),
                new int[]{8, 17}, new int[]{5, 16});
        result = new KL_LUCB(100).identify(candidates, mockIdentify(new int[]{81, 86, 81, 84, 86, 92}), 0, 0.1, 0.15, 1);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), candidates.get(1));
        /***/
        List<Integer[]> list = new ArrayList<>();
        list.add(new Integer[]{0, 1, 2, 3, 4, 5});
        candidates = mockCandidates(list,
                new int[]{9}, new int[]{9});
        result = new KL_LUCB(100).identify(candidates, mockIdentify(new int[]{}), 0, 0.1, 0.15, 1);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0), candidates.get(0));
    }
}