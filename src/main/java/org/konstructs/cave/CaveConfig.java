package org.konstructs.cave;

import com.typesafe.config.Config;

public class CaveConfig {
    private final int maxGenerations;
    private final int minGenerations;
    private final int startPositionRadius;
    private final int minRadius;
    private final int probability;

    public CaveConfig(Config config) {
        this.maxGenerations = config.getInt("max-generations");
        this.minGenerations = config.getInt("min-generations");
        this.startPositionRadius = config.getInt("start-position-radius");
        this.minRadius = config.getInt("min-radius");
        this.probability = config.getInt("probability");
    }

    public int getMaxGenerations() {
        return maxGenerations;
    }

    public int getMinGenerations() {
        return minGenerations;
    }

    public int getStartPositionRadius() {
        return startPositionRadius;
    }

    public int getMinRadius() {
        return minRadius;
    }

    public int getProbability() {
        return probability;
    }

}
