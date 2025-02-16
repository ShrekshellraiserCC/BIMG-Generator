package com.shrekshellraiser.modes;

import com.shrekshellraiser.palettes.Color;

import java.awt.image.BufferedImage;

public class KMeans {
    static private int rgb2hsv(int rgb) {
        int r = (rgb & 0xFF0000) >> 16;
        int g = (rgb & 0x00FF00) >> 8;
        int b = (rgb & 0xFF);
        float [] hsv = new float[3];
        java.awt.Color.RGBtoHSB(r,g,b,hsv);
        return ((int)(hsv[0] * 255) << 16) | ((int)(hsv[1] * 255) << 8) | (int)(hsv[2] * 255);
    }
    static private int hsv2rgb(int hsv) {
        int h = (hsv & 0xFF0000) >> 16;
        int s = (hsv & 0x00FF00) >> 8;
        int v = (hsv & 0xFF);
        float H = (float) h / 255;
        float S = (float) s / 255;
        float V = (float) v / 255;
        java.awt.Color color = java.awt.Color.getHSBColor(H,S,V);
        return color.getRGB();
    }
    public static Color[] applyKMeans(BufferedImage image, int K, Color[] start) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixelArray = new int[width * height];
        Record[] recordPoints = new Record[width * height];
        Centroid[] centroidPoints = new Centroid[K];
        image.getRGB(0, 0, width, height, pixelArray, 0, width);
        for (int i = 0; i < width * height; i++) {
            recordPoints[i] = new Record(new Color(rgb2hsv(pixelArray[i])), centroidPoints);
        }
        for (int i = 0; i < K; i++) {
            centroidPoints[i] = new Centroid(start[i]);
        }
        // First point is one of the data points chosen at random
        for (int i = 1; i < K; i++) {
            // Compute the total weight of all items together.
            // This can be skipped of course if sum is already 1.
            double totalWeight = 0.0;
            for (Record record : recordPoints) {
                totalWeight += record.getWeight(i);
            }

            // Now choose a random item.
            int idx = 0;
            for (double r = Math.random() * totalWeight; idx < recordPoints.length - 1; ++idx) {
                r -= recordPoints[idx].getWeight(i);
                if (r <= 0.0) break;
            }
            centroidPoints[i] = new Centroid(recordPoints[idx].location);
            // https://stackoverflow.com/questions/6737283/weighted-randomness-in-java
        }
        // end choosing centroids
        int iterations = 0;
        boolean centroidsStayedPut = false;
        while (!centroidsStayedPut) {
            centroidsStayedPut = true;
            Color[] averageCentroidLocation = new Color[K];
            int[] centroidCount = new int[K];
            for (int i = 0; i < K; i++) {
                averageCentroidLocation[i] = new Color(0, 0, 0);
                centroidCount[i] = 0;
            }
            for (Record recordPoint : recordPoints) {
                int centroidIndex = recordPoint.determineCentroid();
                averageCentroidLocation[centroidIndex] =
                        averageCentroidLocation[centroidIndex].add(recordPoint.location);
                centroidCount[centroidIndex]++;
            }
            for (int i = 0; i < K; i++) {
                averageCentroidLocation[i] = averageCentroidLocation[i].multiply(1.0f / centroidCount[i]);
                centroidsStayedPut = centroidsStayedPut && centroidPoints[i].setLocation(averageCentroidLocation[i]);
            }
            iterations++;
            if (iterations > 1000) break;
        }
        Color[] returnValue = new Color[K];
        for (int i = 0; i < K; i++) {
            returnValue[i] = new Color(hsv2rgb(centroidPoints[i].location.getColor()));
        }
        return returnValue;
    }
}

class Centroid {
    public Color location;

    Centroid(Color location) {
        this.location = location;
    }

    public boolean setLocation(Color location) {
        if (!this.location.equals(location)) {
            this.location = location;
            return false;
        }
        return true;
    }
}

class Record {
    public Color location; // location in RGB space
    private int closestCentroid = 0;
    private final Centroid[] centroids;

    Record(Color color, Centroid[] centroids) {
        this.centroids = centroids;
        location = color;
    }

    public int determineCentroid(int maxCentroid) {
        double diff = centroids[closestCentroid].location.diff(location);
        for (int index = 0; index < maxCentroid; index++) {
            double lDiff = centroids[index].location.diff(this.location);
            if (lDiff < diff) {
                diff = lDiff;
                closestCentroid = index;
            }
        }
        return closestCentroid;
    }

    public int determineCentroid() {
        return determineCentroid(centroids.length);
    }

    public double getWeight(int maxCentroid) {
        determineCentroid(maxCentroid);
        return Math.pow(location.diff(centroids[closestCentroid].location), 2) /
                Math.pow(0xFFFFFF, 2); // Normalize so max distance = 1.0
    }
}