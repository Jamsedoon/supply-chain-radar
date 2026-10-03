package com.zisti.radar.matching;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The configured Fellegi-Sunter parameters, loaded from a properties file.
 *
 * <p>Deliberately uses {@link Properties} from the standard library rather than
 * a YAML parser. The only YAML parser on the classpath arrives as part of Spring
 * Boot, and this package must not depend on the framework — a dependency that
 * happens to be present is not the same as a dependency you declared.
 *
 * <p>Missing or malformed entries fail at load time rather than silently
 * defaulting. A quietly missing weight would skew every score in the system
 * while looking like an algorithm problem.
 */
public final class MatchWeights {

    private static final String DEFAULT_PATH = "/matching-weights.properties";

    private final Properties properties;
    private final double strongBoundary;
    private final double weakBoundary;
    private final double threshold;

    private MatchWeights(Properties properties) {
        this.properties = properties;
        this.strongBoundary = readDouble("level.strong");
        this.weakBoundary = readDouble("level.weak");
        this.threshold = readDouble("threshold");

        if (weakBoundary >= strongBoundary) {
            throw new IllegalStateException(
                "level.weak (" + weakBoundary + ") must be below level.strong ("
                    + strongBoundary + ")");
        }
    }

    /** Loads the weights shipped with the application. */
    public static MatchWeights loadDefault() {
        return loadFrom(DEFAULT_PATH);
    }

    /** Loads weights from any classpath resource. Useful for tests. */
    public static MatchWeights loadFrom(String classpathResource) {
        Properties loaded = new Properties();

        try (InputStream in = MatchWeights.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException(
                    "weights file not found on classpath: " + classpathResource);
            }
            loaded.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                "could not read weights file: " + classpathResource, e);
        }

        return new MatchWeights(loaded);
    }

    /**
     * Builds weights from an in-memory property set.
     *
     * <p>Used to run the engine with parameters estimated from labelled data
     * without replacing the shipped configuration, so both can be measured in
     * one pass.
     */
    public static MatchWeights fromProperties(Properties properties) {
        return new MatchWeights(properties);
    }

    /** The total evidence, in bits, required to raise an alert. */
    public double threshold() {
        return threshold;
    }

    /** Sorts a raw comparator score into its evidence bucket. */
    public Level bucket(double rawScore) {
        if (rawScore >= strongBoundary) {
            return Level.STRONG;
        }
        if (rawScore >= weakBoundary) {
            return Level.WEAK;
        }
        return Level.NONE;
    }

    /**
     * The evidence, in bits, that a comparator contributes at a given level.
     *
     * <p>This is {@code log2(m / u)}: how much more often the comparator agrees
     * for a real squat than it does by chance. {@link Level#NONE} contributes
     * nothing, and so does any level configured with a zero rate.
     */
    public double weightFor(String comparatorName, Level level) {
        if (level == Level.NONE) {
            return 0.0;
        }

        double m = readDouble(comparatorName + "." + level.name() + ".m");
        double u = readDouble(comparatorName + "." + level.name() + ".u");

        // A level that never fires carries no evidence, and log2(0) is undefined.
        if (m <= 0.0 || u <= 0.0) {
            return 0.0;
        }

        return log2(m / u);
    }

    private double readDouble(String key) {
        String value = properties.getProperty(key);

        if (value == null) {
            throw new IllegalStateException("missing weights entry: " + key);
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                "weights entry '" + key + "' is not a number: " + value, e);
        }
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2);
    }
}