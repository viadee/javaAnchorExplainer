package de.goerke.tobias.anchorj.base.exploration;

import de.goerke.tobias.anchorj.base.AnchorCandidate;
import de.goerke.tobias.anchorj.base.execution.AbstractSamplingService;
import de.goerke.tobias.anchorj.base.execution.SamplingService;
import de.goerke.tobias.anchorj.util.NoiseGenerator;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Test comparing runtime of several MABs
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class ExplorationAlgorithmsPerformanceTest {

    /*
     * Meta test vars
     */
    private static final int CANDIDATE_COUNT = 100;
    private final Map<String, Map<String, Object>> testParamsMap = new HashMap<>();
    private final Random rnd = new Random();
    /**
     * Candidates under test
     */
    private Map<AnchorCandidate, Double> candidateToMeanValues;
    private Map<String, Object> currentMap;

    @AfterAll
    void outputTestResults() {
        for (Map.Entry<String, Map<String, Object>> entry : testParamsMap.entrySet()) {
            Map<String, Object> testMap = entry.getValue();
            long timeTaken = ((long) testMap.get("end") - (long) testMap.get("start"));
            long sampleSize = (long) testMap.get("sampledSize");
            System.out.println("*****************************************");
            System.out.println("Test-Results for " + entry.getKey().replace("(", "").replace(")", ""));
            System.out.println("Time taken " + timeTaken / 1000000 + " ms");
            System.out.println("Sampled size " + sampleSize);
            System.out.println("Time per sample " + timeTaken / (double) sampleSize + " ns");
            System.out.println("Total difference " + testMap.get("totalDiff"));
            System.out.println("*****************************************");
        }
    }

    @BeforeEach
    void init(TestInfo testInfo) {
        currentMap = new HashMap<>();
        testParamsMap.put(testInfo.getDisplayName(), currentMap);
        currentMap.put("start", System.nanoTime());

        Map<AnchorCandidate, Double> tmpCandidateToMeanValues = new LinkedHashMap<>();
        for (int i = 0; i < CANDIDATE_COUNT; i++) {
            AnchorCandidate candidate = new AnchorCandidate(Collections.singletonList(i), null);
            tmpCandidateToMeanValues.put(candidate, i * (1 / (double) CANDIDATE_COUNT));
        }
        List<AnchorCandidate> list = new ArrayList<>(tmpCandidateToMeanValues.keySet());
        Collections.shuffle(list);
        candidateToMeanValues = new LinkedHashMap<>();
        list.forEach(k -> candidateToMeanValues.put(k, tmpCandidateToMeanValues.get(k)));
    }

    @AfterEach
    void terminate(TestInfo testInfo) {
        currentMap.put("end", System.nanoTime());
        long sampleSize = candidateToMeanValues.keySet().stream().mapToInt(AnchorCandidate::getSampledSize).sum();
        currentMap.put("sampledSize", sampleSize);
    }

    @Test
    void sequentialBatchSAR() {
        startTest(new BatchSAR(CANDIDATE_COUNT * 100, CANDIDATE_COUNT));
    }


    @Test
    void parallelBatchSAR() {
        startTest(new BatchSAR(CANDIDATE_COUNT * 100, CANDIDATE_COUNT));
    }

    @Test
    void sequentialBatchRacing() {
        startTest(new BatchRacing(CANDIDATE_COUNT * 100));
    }

    @Test
    void parallelBatchRacing() {
        startTest(new BatchRacing(CANDIDATE_COUNT * 100));
    }

    @Test
    void medianElimination() {
        startTest(new MedianElimination());
    }

    @Test
    void KL_LUCB() {
        startTest(new KL_LUCB(100));
    }

    private void startTest(BestAnchorIdentification algorithm) {
        final List<AnchorCandidate> result = algorithm.identify(new ArrayList<>(candidateToMeanValues.keySet()),
                createSamplingService(10, true), 0.15, 0.1, 5);
        int totalDiff = 0;
        for (int i = 0; i < result.size(); i++) {
            final AnchorCandidate candidate = result.get(i);
            totalDiff += Math.abs((CANDIDATE_COUNT - 1 - i) - candidate.getOrderedFeatures().get(0));
        }
        currentMap.put("totalDiff", totalDiff);
    }


    private SamplingService createSamplingService(final int threadCount, final boolean doBalance) {
        final BiFunction<AnchorCandidate, Integer, Double> function = (candidate, count) -> {
            try {
                Thread.sleep(5);
            } catch (Exception ignored) {
            }
            double noised = Math.min(1, Math.max(0,
                    NoiseGenerator.generateGaussianNoise(candidateToMeanValues.get(candidate), 0.04)));
            candidate.registerSamples(count, (int) (count * noised));

            return 0D;
        };

        return SamplingService.createDefaultExecution(function, threadCount, true);
    }
}
