package de.viadee.anchorj;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.viadee.xai.anchor.AnchorCandidate;
import de.viadee.xai.anchor.AnchorConstruction;
import de.viadee.xai.anchor.AnchorConstructionBuilder;
import de.viadee.xai.anchor.DataInstance;
import de.viadee.xai.anchor.PerturbationFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnchorConstructionTest {

    @SuppressWarnings("unchecked")
    private static List<AnchorCandidate> generateCandidateSet(final List<AnchorCandidate> previousBest,
                                                              final int featureCount,
                                                              final double minCoverage) throws Exception {
        final AnchorConstruction constructor = new AnchorConstructionBuilder<>(t -> 0,
                (s, t) -> new PerturbationFunction.PerturbationResultImpl(new Object[0], new boolean[0][0]),
                new IntegerDataInstance()).build();

        Method method = AnchorConstruction.class.getDeclaredMethod("generateCandidateSet", List.class, int.class,
                double.class);
        method.setAccessible(true);
        return (List<AnchorCandidate>) method.invoke(constructor, previousBest, featureCount, minCoverage);
    }

    private static AnchorCandidate candidate(int... features) {
        return new AnchorCandidate(IntStream.of(features).boxed().collect(Collectors.toSet()));
    }

    @Test
    public void testGenerateCandidates() throws Exception {
        assertEquals(generateCandidateSet(Collections.emptyList(), 10, 0).size(), 10);
        assertEquals(generateCandidateSet(Collections.singletonList(candidate(-1)), 10, 0).size(), 10);
        assertEquals(generateCandidateSet(Collections.singletonList(candidate(0)), 10, 0).size(), 9);
        assertEquals(generateCandidateSet(Collections.singletonList(candidate(1, 2, 3)), 10, 0).size(), 7);


        assertEquals(generateCandidateSet(Arrays.asList(candidate(1, 2, 3), candidate(1, 2, 3)), 10, 0).size(), 7);
        assertEquals(generateCandidateSet(Arrays.asList(candidate(1, 2, 3), candidate(1, 2, 3, 4)), 10, 0).size(), 13);
    }

    private static class IntegerDataInstance implements DataInstance<int[]> {
        private static final long serialVersionUID = -8505052889913449802L;

        final int[] wrapped;


        public IntegerDataInstance(int... wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int[] getInstance() {
            return wrapped;
        }

        @Override
        public int getFeatureCount() {
            return wrapped.length;
        }
    }
}
