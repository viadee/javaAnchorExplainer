package de.goerke.tobias.anchorj.image;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Vector;

/**
 * Adapted from: http://popscan.blogspot.com/2014/12/superpixel-algorithm-implemented-in-java.html
 * <p>
 * Changed some minor things only
 */
public class Superpixel {
    // in case of instable clusters, max number of loops
    private static final int maxClusteringLoops = 50;

    // arrays to store values during process
    private double[] distances;
    private int[] labels;
    private int[] reds;
    private int[] greens;
    private int[] blues;
    private Cluster[] clusters;


    private Superpixel() {
    }

    public static ImageInstance calculateClusters(ImageRepresentation imageInstance, int superpixelWidth, int proximityModifier) {
        Superpixel sp = new Superpixel();
        return sp.calculate(imageInstance, superpixelWidth, proximityModifier);
    }

    private ImageInstance calculate(ImageRepresentation imageRepresentation, double S, double m) {
        BufferedImage image = imageRepresentation.asBufferedImage();
        int width = image.getWidth();
        int height = image.getHeight();


        // get the image pixels
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);

        // create and fill lookup tables
        distances = new double[width * height];
        Arrays.fill(distances, Integer.MAX_VALUE);
        labels = new int[width * height];
        Arrays.fill(labels, -1);
        // split rgb-values to own arrays
        reds = new int[width * height];
        greens = new int[width * height];
        blues = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pos = x + y * width;
                int color = pixels[pos];
                reds[pos] = color >> 16 & 0x000000FF;
                greens[pos] = color >> 8 & 0x000000FF;
                blues[pos] = color & 0x000000FF;
            }
        }

        // create clusters
        createClusters(image, S, m);
        // loop until all clusters are stable!
        int loops = 0;
        boolean pixelChangedCluster = true;
        while (pixelChangedCluster && loops < maxClusteringLoops) {
            pixelChangedCluster = false;
            loops++;
            // for each cluster center C
            for (int i = 0; i < clusters.length; i++) {
                Cluster c = clusters[i];
                // for each pixel i in 2S region around
                // cluster center
                int xs = Math.max((int) (c.avg_x - S), 0);
                int ys = Math.max((int) (c.avg_y - S), 0);
                int xe = Math.min((int) (c.avg_x + S), width);
                int ye = Math.min((int) (c.avg_y + S), height);
                for (int y = ys; y < ye; y++) {
                    for (int x = xs; x < xe; x++) {
                        int pos = x + width * y;
                        double D = c.distance(x, y, reds[pos],
                                greens[pos],
                                blues[pos],
                                S, m, width, height);
                        if ((D < distances[pos]) && (labels[pos] != c.id)) {
                            distances[pos] = D;
                            labels[pos] = c.id;
                            pixelChangedCluster = true;
                        }
                    } // end for x
                } // end for y
            } // end for clusters
            // reset clusters
            for (int index = 0; index < clusters.length; index++) {
                clusters[index].reset();
            }
            // add every pixel to cluster based on label
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pos = x + y * width;
                    clusters[labels[pos]].addPixel(x, y,
                            reds[pos], greens[pos], blues[pos]);
                }
            }

            // calculate centers
            for (int index = 0; index < clusters.length; index++) {
                clusters[index].calculateCenter();
            }
        }

        return new ImageInstance(imageRepresentation, labels);
    }

    /*
     * Create initial clusters.
     */
    private void createClusters(BufferedImage image, double S, double m) {
        Vector<Cluster> temp = new Vector<>();
        int w = image.getWidth();
        int h = image.getHeight();
        boolean even = false;
        double xstart;
        int id = 0;
        for (double y = S / 2; y < h; y += S) {
            // alternate clusters x-position
            // to create nice hexagon grid
            if (even) {
                xstart = S / 2.0;
                even = false;
            } else {
                xstart = S;
                even = true;
            }
            for (double x = xstart; x < w; x += S) {
                int pos = (int) (x + y * w);
                Cluster c = new Cluster(id,
                        reds[pos], greens[pos], blues[pos],
                        (int) x, (int) y, S, m);
                temp.add(c);
                id++;
            }
        }
        clusters = new Cluster[temp.size()];
        for (int i = 0; i < temp.size(); i++) {
            clusters[i] = temp.elementAt(i);
        }
    }


    private class Cluster {
        int id;
        double inv;        // inv variable for optimization
        double pixelCount;    // pixels in this cluster
        double avg_red;     // average red value
        double avg_green;    // average green value
        double avg_blue;    // average blue value
        double sum_red;     // sum red values
        double sum_green;   // sum green values
        double sum_blue;     // sum blue values
        double sum_x;       // sum x
        double sum_y;       // sum y
        double avg_x;       // average x
        double avg_y;       // average y

        public Cluster(int id, int in_red, int in_green,
                       int in_blue, int x, int y,
                       double S, double m) {
            // inverse for distance calculation
            this.inv = 1.0 / ((S / m) * (S / m));
            this.id = id;
            addPixel(x, y, in_red, in_green, in_blue);
            // calculate center with initial one pixel
            calculateCenter();
        }

        public void reset() {
            avg_red = 0;
            avg_green = 0;
            avg_blue = 0;
            sum_red = 0;
            sum_green = 0;
            sum_blue = 0;
            pixelCount = 0;
            avg_x = 0;
            avg_y = 0;
            sum_x = 0;
            sum_y = 0;
        }

        /*
         * Add pixel color values to sum of previously added
         * color values.
         */
        void addPixel(int x, int y, int in_red,
                      int in_green, int in_blue) {
            sum_x += x;
            sum_y += y;
            sum_red += in_red;
            sum_green += in_green;
            sum_blue += in_blue;
            pixelCount++;
        }

        public void calculateCenter() {
            // Optimization: using "inverse"
            // to change divide to multiply
            double inv = 1 / pixelCount;
            avg_red = sum_red * inv;
            avg_green = sum_green * inv;
            avg_blue = sum_blue * inv;
            avg_x = sum_x * inv;
            avg_y = sum_y * inv;
        }

        double distance(int x, int y,
                        int red, int green, int blue,
                        double S, double m, int w, int h) {
            // power of color difference between
            // given pixel and cluster center
            double dx_color = (avg_red - red) * (avg_red - red)
                    + (avg_green - green) * (avg_green - green)
                    + (avg_blue - blue) * (avg_blue - blue);
            // power of spatial difference between
            // given pixel and cluster center
            double dx_spatial = (avg_x - x) * (avg_x - x) + (avg_y - y) * (avg_y - y);
            // Calculate approximate distance D
            // double D = dx_color+dx_spatial*inv;
            // Calculate squares to get more accurate results
            return Math.sqrt(dx_color) + Math.sqrt(dx_spatial * inv);
        }
    }

}
