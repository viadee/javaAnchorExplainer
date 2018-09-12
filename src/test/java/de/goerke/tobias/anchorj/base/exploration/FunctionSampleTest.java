package de.goerke.tobias.anchorj.base.exploration;

import de.goerke.tobias.anchorj.base.*;
import de.goerke.tobias.anchorj.util.NoiseGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class contains multiple tests for approximating mathematical functions.
 * <p>
 * Please note that Anchor is not natively usable for non-classification problems.
 * <p>
 * The tests in here serve illustrative purposes only and are used to visualize Anchors in a 2D space.
 */
public class FunctionSampleTest {
    private final static int PERTURBATION_RANGE = 10;

    // This function calculates the actual coverage of the rule.
    // This has to be done since Anchors cannot natively output rule ranges (e.g. x in [5,10]).
    // Hence, we have to see in which area the rule is applicable by ourselves.
    private static List<Point> calculateCoveredPoints(AnchorResult<Point> result,
                                                      ClassificationFunction<Point> classificationFunction) {
        final Point resultPoint = result.getInstance();
        final int resultLabel = result.getLabel();

        final int perturbationDiameter = PERTURBATION_RANGE / 2;

        List<Point> coveredPoints = new ArrayList<>();
        coveredPoints.add(resultPoint);

        if (result.getOrderedFeatures().contains(0)) {
            for (int i = -perturbationDiameter; i <= perturbationDiameter; i++) {
                if (i == 0)
                    continue;
                final Point testPoint = new Point(resultPoint.x + i, resultPoint.y);
                if (classificationFunction.predict(testPoint) == resultLabel)
                    coveredPoints.add(testPoint);
            }
        }
        if (result.getOrderedFeatures().contains(1)) {
            for (Point point : new ArrayList<>(coveredPoints)) {
                for (int i = -perturbationDiameter; i <= perturbationDiameter; i++) {
                    if (i == 0)
                        continue;
                    final Point testPoint = new Point(point.x, point.y + i);
                    if (classificationFunction.predict(testPoint) == resultLabel)
                        coveredPoints.add(testPoint);
                }
            }
        }

        final int[] xs = coveredPoints.stream().mapToInt(p -> p.x).distinct().toArray();
        final int[] ys = coveredPoints.stream().mapToInt(p -> p.y).distinct().toArray();

        return coveredPoints;
    }

    private static PerturbationFunction<Point> createPerturbationFunction(final Point explainedInstance) {
        final Random rnd = new Random();
        return (immutableFeaturesIdx, nrPerturbations) -> {
            Point[] rawResult = new Point[nrPerturbations];
            boolean[][] featuresChanged = new boolean[nrPerturbations][];
            for (int i = 0; i < nrPerturbations; i++) {
                int x = explainedInstance.x;
                int y = explainedInstance.y;

                if (!immutableFeaturesIdx.contains(0)) {
                    x += rnd.nextInt(PERTURBATION_RANGE + 1) - PERTURBATION_RANGE / 2;
                }
                if (!immutableFeaturesIdx.contains(1)) {
                    y += rnd.nextInt(PERTURBATION_RANGE + 1) - PERTURBATION_RANGE / 2;
                }

                rawResult[i] = new Point(x, y);
                boolean featureChanged[] = new boolean[2];
                featureChanged[0] = x != explainedInstance.x;
                featureChanged[1] = y != explainedInstance.y;
                featuresChanged[i] = featureChanged;
            }
            return new PerturbationFunction.PerturbationResult<>(rawResult, featuresChanged);
        };
    }

    /**
     * This test samples from a 2D function that returns label 1 with a chance of >95% when both x and y are positive,
     * e.g. are in the upper right quadrant of the coordinate grid.
     * <p>
     * The explained instance (0, 100) lies at the border to being classified positively. In this example, the
     * x coordinate is the most important feature as changing it slightly will lead to a different classification.
     * <p>
     * Hence, this test is supposed to find this volatile feature.
     */
    @Test
    public void simpleFunctionTest() {
        final Point explainedInstance = new Point(1, 100);

        ClassificationFunction<Point> classificationFunction = point -> (point.x > 0 && point.y > 0) ? 1 : 0;
        ClassificationFunction<Point> noisedClassificationFunction = point ->
                (NoiseGenerator.addBooleanNoise(classificationFunction.predict(point), 0.05));

        PerturbationFunction<Point> perturbationFunction = createPerturbationFunction(explainedInstance);

        try {
            AnchorResult<Point> result = new AnchorConstructionBuilder<>(noisedClassificationFunction,
                    perturbationFunction, explainedInstance, classificationFunction.predict(explainedInstance))
                    .setTau(0.8)
                    .setBeamSize(1)
                    .build()
                    .constructAnchor();

            assertEquals(result.getOrderedFeatures().size(), 1);
            assertEquals((int) result.getOrderedFeatures().get(0), 0);

            calculateCoveredPoints(result, classificationFunction);
        } catch (NoAnchorFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method tests a point in which both features are close to a border.
     * Thus, the result should include both points.
     */
    @Test
    public void simpleMultiFeatureFunctionTest() {
        final Point explainedInstance = new Point(1, 1);

        ClassificationFunction<Point> classificationFunction = point -> (point.x > 0 && point.y > 0) ? 1 : 0;
        ClassificationFunction<Point> noisedClassificationFunction = point ->
                (NoiseGenerator.addBooleanNoise(classificationFunction.predict(point), 0.05));

        PerturbationFunction<Point> perturbationFunction = createPerturbationFunction(explainedInstance);

        try {
            AnchorResult<Point> result = new AnchorConstructionBuilder<>(noisedClassificationFunction,
                    perturbationFunction, explainedInstance, classificationFunction.predict(explainedInstance))
                    .setTau(0.8)
                    .setBeamSize(1)
                    .build()
                    .constructAnchor();

            assertEquals(result.getOrderedFeatures().size(), 2);

            calculateCoveredPoints(result, classificationFunction);
        } catch (NoAnchorFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method tests a point in which both features are close to a border.
     * Thus, the result should include both points.
     */
    @Test
    public void simpleMultiFeatureFunctionDifferentLabelsTest() {
        final Point explainedInstance = new Point(1, 0);

        ClassificationFunction<Point> classificationFunction = point -> (point.x > 0 && point.y > 0) ? 1 : 0;
        ClassificationFunction<Point> noisedClassificationFunction = point ->
                (NoiseGenerator.addBooleanNoise(classificationFunction.predict(point), 0.05));

        PerturbationFunction<Point> perturbationFunction = createPerturbationFunction(explainedInstance);

        try {
            AnchorResult<Point> result = new AnchorConstructionBuilder<>(noisedClassificationFunction,
                    perturbationFunction, explainedInstance, classificationFunction.predict(explainedInstance))
                    .setTau(0.8)
                    .setBeamSize(1)
                    .build()
                    .constructAnchor();

            assertEquals(result.getOrderedFeatures().size(), 1);
            assertEquals((int) result.getOrderedFeatures().get(0), 1);

            calculateCoveredPoints(result, classificationFunction);
        } catch (NoAnchorFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Point implements DataInstance<Integer[]> {
        private final int x;
        private final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public Integer[] getInstance() {
            return new Integer[]{x, y};
        }

        @Override
        public int getFeatureCount() {
            return 2;
        }
    }
}
