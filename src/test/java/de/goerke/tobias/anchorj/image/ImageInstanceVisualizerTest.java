package de.goerke.tobias.anchorj.image;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.*;
import java.net.URL;
import java.rmi.server.ExportException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class ImageInstanceVisualizerTest {

    @Test
    public void drawImportance() throws Exception {
        URL resource = Thread.currentThread().getContextClassLoader().getResource("dog.jpg");
        BufferedImageRepresentation imageRepresentation = new BufferedImageRepresentation(ImageIO.read(resource));
        ImageInstance imageInstance = Superpixel.calculateClusters(imageRepresentation, 100, 150);
        ImageInstanceVisualizer visualizer = new ImageInstanceVisualizer(imageInstance);

        Map<Integer, Double> importanceMap = new HashMap<>();
        for (int i = 0; i < imageInstance.getFeatureCount(); i++) {
            importanceMap.put(i, 0D);
        }
        ImageRepresentation expectedEmpty = visualizer.drawImportance(importanceMap);
        for (int i = 0; i < expectedEmpty.getWidth(); i++)
            for (int j = 0; j < expectedEmpty.getHeight(); j++)
                assertEquals(expectedEmpty.getPixel(i, j), Color.HSBtoRGB(0, 0, 0));

        double stepSize = 1D / imageInstance.getFeatureCount();
        importanceMap = new HashMap<>();
        for (int i = 0; i < imageInstance.getFeatureCount(); i++) {
            importanceMap.put(i, 1 - stepSize * i);
        }
        // TODO test result
    }
}