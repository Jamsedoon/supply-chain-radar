package com.zisti.radar.matching;

import java.util.Locale;

/**
 * Reduces npm package names to a comparable form.
 *
 * <p>Comparison happens on normalized names so that cosmetic differences —
 * casing, scope prefixes, punctuation — do not hide a real similarity.
 * Callers keep the original name for display, and for the delimiter
 * comparator, which needs punctuation to still be there.
 */
public final class Normalizer {

    /** This class is never instantiated — it only holds a static method. */
    private Normalizer() {
        throw new AssertionError("Normalizer is a utility class");
    }

    /**
     * Normalizes a package name for comparison.
     *
     * <p>In order: lowercase, remove the scope, remove every character that
     * is not a lowercase letter or a digit.
     *
     * <pre>
     *   "@types/Node-Fetch"  -> "nodefetch"
     *   "Cross-Env"          -> "crossenv"
     *   "lodash.merge"       -> "lodashmerge"
     * </pre>
     *
     * @param raw the package name as published; must not be null
     * @return the normalized form; may be empty if the name had no letters or digits
     * @throws IllegalArgumentException if {@code raw} is null
     */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("package name must not be null");
        }

        String lowered = raw.toLowerCase(Locale.ROOT);
        String unscoped = stripScope(lowered);
        return unscoped.replaceAll("[^a-z0-9]", "");
    }

    /**
     * Removes an npm scope prefix, so {@code @babel/core} becomes {@code core}.
     *
     * <p>Only a leading {@code @} followed later by a slash counts as a scope.
     */
    private static String stripScope(String name) {
        if (!name.startsWith("@")) {
            return name;
        }
        int slash = name.indexOf('/');
        return slash > 0 ? name.substring(slash + 1) : name;
    }
}