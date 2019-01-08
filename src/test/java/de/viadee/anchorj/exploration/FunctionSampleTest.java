package de.viadee.anchorj.exploration;

import de.viadee.anchorj.*;
import de.viadee.anchorj.coverage.CoverageIdentification;
import de.viadee.anchorj.util.NoiseGenerator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * This class contains multiple tests for approximating mathematical functions.
 * <p>
 * Please note that Anchor is not natively usable for non-classification problems.
 * <p>
 * The tests in here serve illustrative purposes only and are used to visualize Anchors in a 2D space.
 */
public class FunctionSampleTest {
    private final static int PERTURBATION_RANGE = 40;
    private final static double STEPS = 0.5;
    private static final Random RND = new Random();
    private final static Function<Point, Integer> CLASSIFIER = (point) -> {
        final double functionY = 0.01 * Math.pow(point.x, 3) - 0.09 * Math.pow(point.x, 2) - point.x + 5;
        return (point.y >= functionY) ? 1 : 0;
    };

    private static List<Point> PERTURBED_POINTS;

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

        final double[] xs = coveredPoints.stream().mapToDouble(p -> p.x).distinct().toArray();
        final double[] ys = coveredPoints.stream().mapToDouble(p -> p.y).distinct().toArray();

        return coveredPoints;
    }

    private static Point perturbCircular(Point explainedInstance, boolean perturbX, boolean perturbY) {
        final double a = 2 * Math.PI * RND.nextDouble();
        final double r = PERTURBATION_RANGE / 2D * Math.sqrt(RND.nextDouble());
        double x = explainedInstance.x;
        double y = explainedInstance.y;
        if (perturbX) {
            x += r * Math.cos(a);
        }
        if (perturbY) {
            y += r * Math.sin(a);
        }
        final Point point = new Point(x, y);
        PERTURBED_POINTS.add(point);
        return point;
    }

    private static PerturbationFunction<Point> createPerturbationFunction(final Point explainedInstance) {
        return (immutableFeaturesIdx, nrPerturbations) -> {
            Point[] rawResult = new Point[nrPerturbations];
            boolean[][] featuresChanged = new boolean[nrPerturbations][];
            for (int i = 0; i < nrPerturbations; i++) {
                rawResult[i] = perturbCircular(explainedInstance,
                        !immutableFeaturesIdx.contains(0), !immutableFeaturesIdx.contains(1));
                boolean[] featureChanged = new boolean[2];
                featureChanged[0] = rawResult[i].x != explainedInstance.x;
                featureChanged[1] = rawResult[i].y != explainedInstance.y;
                featuresChanged[i] = featureChanged;
            }
            return new PerturbationFunction.PerturbationResultImpl<>(rawResult, featuresChanged);
        };
    }

    private static boolean equalsApprox(double a, double b, double eps) {
        return Math.abs(a - b) < eps;
    }

    private static List<Point> getCoveredPoints(Point explainedInstance, int explainedInstanceLabel,
                                                Set<Integer> featureSet, int coverageCount) {
        List<Point> result = new ArrayList<>();
        for (int i = 0; i < coverageCount; i++) {
            // get a random point of perturbation space
            final Point point = perturbCircular(explainedInstance, true, true);
            if (CLASSIFIER.apply(point) != explainedInstanceLabel)
                continue;
            if (featureSet.contains(0) && !equalsApprox(point.x, explainedInstance.x, STEPS))
                continue;
            if (featureSet.contains(1) && !equalsApprox(point.y, explainedInstance.y, STEPS))
                continue;

            result.add(point);
        }
        return result;
    }

    @BeforeEach
    void beforeEach() {
        PERTURBED_POINTS = new ArrayList<>();
    }

    @AfterEach
    void afterEach() {
        System.out.println();
    }

    @Test
    public void complexFunctionTest() {
        final Point explainedInstance = new Point(10, 10);
        final int explainedInstanceLabel = CLASSIFIER.apply(explainedInstance);

        ClassificationFunction<Point> noisedClassificationFunction = point ->
                (NoiseGenerator.addBooleanNoise(CLASSIFIER.apply(point), 0.05));

        PerturbationFunction<Point> perturbationFunction = createPerturbationFunction(explainedInstance);

        CoverageIdentification coverageIdentification = featureSet -> {
            final int coverageCount = 1000;
            final List<Point> coveredPoints = getCoveredPoints(explainedInstance, explainedInstanceLabel, featureSet, coverageCount);
            return coveredPoints.size() / (double) coverageCount;
        };

        AnchorResult<Point> result = new AnchorConstructionBuilder<>(noisedClassificationFunction,
                perturbationFunction, explainedInstance, explainedInstanceLabel)
                .setCoverageIdentification(coverageIdentification)
                .setInitSampleCount(200)
                .setTau(0.8)
                .setBeamSize(1)
                .build()
                .constructAnchor();

        //assertEquals(result.getOrderedFeatures().size(), 1);
        //assertEquals((int) result.getOrderedFeatures().get(0), 0);

        final List<Point> coveredPoints = getCoveredPoints(explainedInstance, explainedInstanceLabel, result.getCanonicalFeatures(), 10000);
        final String x = "explainedX = " + String.valueOf(explainedInstance.x);
        final String y = "explainedY = " + String.valueOf(explainedInstance.y);
        final String perturbationRange = "perturbationRange = " + String.valueOf(PERTURBATION_RANGE / 2);
        final String xs = "xCoverage = c(" + coveredPoints.stream().map(Point::getX).map(String::valueOf).collect(Collectors.joining(", ")) + ")";
        final String ys = "yCoverage = c(" + coveredPoints.stream().map(Point::getY).map(String::valueOf).collect(Collectors.joining(", ")) + ")";

        final String rVariables = String.join(System.lineSeparator(), x, y, xs, ys, perturbationRange);

        coveredPoints.size();
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

        AnchorResult<Point> result = new AnchorConstructionBuilder<>(noisedClassificationFunction,
                perturbationFunction, explainedInstance, classificationFunction.predict(explainedInstance))
                .setTau(0.8)
                .setBeamSize(1)
                .build()
                .constructAnchor();

        assertEquals(result.getOrderedFeatures().size(), 1);
        assertEquals((int) result.getOrderedFeatures().get(0), 0);

        calculateCoveredPoints(result, classificationFunction);

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

        AnchorResult<Point> result = new AnchorConstructionBuilder<>(noisedClassificationFunction,
                perturbationFunction, explainedInstance, classificationFunction.predict(explainedInstance))
                .setTau(0.8)
                .setBeamSize(1)
                .build()
                .constructAnchor();

        assertEquals(result.getOrderedFeatures().size(), 2);

        calculateCoveredPoints(result, classificationFunction);

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

        AnchorResult<Point> result = new AnchorConstructionBuilder<>(noisedClassificationFunction,
                perturbationFunction, explainedInstance, classificationFunction.predict(explainedInstance))
                .setTau(0.8)
                .setBeamSize(1)
                .build()
                .constructAnchor();

        assertEquals(result.getOrderedFeatures().size(), 1);
        assertEquals((int) result.getOrderedFeatures().get(0), 1);

        calculateCoveredPoints(result, classificationFunction);

    }

    private static class Point implements DataInstance<Double[]> {
        private static final long serialVersionUID = 8921163081817881426L;

        final double x;
        final double y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public Double[] getInstance() {
            return new Double[]{x, y};
        }

        @Override
        public int getFeatureCount() {
            return 2;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

}
