package main.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PositionRules {

    public enum PositionCategory {
        FACULTY_CHAIRMAN,
        MALE_RESIDENT_REPRESENTATIVE,
        MALE_NON_RESIDENT_REPRESENTATIVE,
        FEMALE_RESIDENT_REPRESENTATIVE,
        FEMALE_NON_RESIDENT_REPRESENTATIVE,
        UNKNOWN
    }

    public static final List<PositionCategory> CANONICAL_ORDER = Collections.unmodifiableList(Arrays.asList(
            PositionCategory.FACULTY_CHAIRMAN,
            PositionCategory.MALE_RESIDENT_REPRESENTATIVE,
            PositionCategory.MALE_NON_RESIDENT_REPRESENTATIVE,
            PositionCategory.FEMALE_RESIDENT_REPRESENTATIVE,
            PositionCategory.FEMALE_NON_RESIDENT_REPRESENTATIVE
    ));

    private PositionRules() {
    }

    public static PositionCategory classify(String rawPositionName) {
        String normalized = normalizePositionName(rawPositionName);
        if (normalized.isEmpty()) {
            return PositionCategory.UNKNOWN;
        }

        boolean hasFemale = normalized.contains("female") || normalized.contains("woman") || normalized.contains("women");
        boolean hasMale = (normalized.contains("male") || normalized.contains("man") || normalized.contains("men")) && !hasFemale;
        boolean nonResident = normalized.contains("non resident") || normalized.contains("nonresident");
        boolean resident = normalized.contains("resident") && !nonResident;
        boolean facultyRole = normalized.contains("faculty") &&
                (normalized.contains("chair") || normalized.contains("determin") || normalized.contains("represent"));

        if (facultyRole) {
            return PositionCategory.FACULTY_CHAIRMAN;
        }
        if (hasMale && resident) {
            return PositionCategory.MALE_RESIDENT_REPRESENTATIVE;
        }
        if (hasMale && nonResident) {
            return PositionCategory.MALE_NON_RESIDENT_REPRESENTATIVE;
        }
        if (hasFemale && resident) {
            return PositionCategory.FEMALE_RESIDENT_REPRESENTATIVE;
        }
        if (hasFemale && nonResident) {
            return PositionCategory.FEMALE_NON_RESIDENT_REPRESENTATIVE;
        }

        return PositionCategory.UNKNOWN;
    }

    public static boolean isCanonical(PositionCategory category) {
        return category != null && category != PositionCategory.UNKNOWN;
    }

    public static String canonicalLabel(PositionCategory category) {
        if (category == null) {
            return "Unknown Position";
        }

        switch (category) {
            case FACULTY_CHAIRMAN:
                return "Faculty Chairman";
            case MALE_RESIDENT_REPRESENTATIVE:
                return "Male Resident Representative";
            case MALE_NON_RESIDENT_REPRESENTATIVE:
                return "Male Non-Resident Representative";
            case FEMALE_RESIDENT_REPRESENTATIVE:
                return "Female Resident Representative";
            case FEMALE_NON_RESIDENT_REPRESENTATIVE:
                return "Female Non-Resident Representative";
            default:
                return "Unknown Position";
        }
    }

    public static String normalizeGender(String rawGender) {
        if (rawGender == null) {
            return "";
        }

        String normalized = rawGender.trim().toUpperCase(Locale.ROOT);
        if ("M".equals(normalized)) {
            return "MALE";
        }
        if ("F".equals(normalized)) {
            return "FEMALE";
        }
        return normalized;
    }

    public static String displayGender(String rawGender) {
        String normalized = normalizeGender(rawGender);
        if (normalized.isEmpty()) {
            return "NOT SET";
        }
        return normalized;
    }

    public static boolean hasGenderRequirement(PositionCategory category) {
        return !requiredGender(category).isEmpty();
    }

    public static String requiredGender(PositionCategory category) {
        if (category == null) {
            return "";
        }

        switch (category) {
            case MALE_RESIDENT_REPRESENTATIVE:
            case MALE_NON_RESIDENT_REPRESENTATIVE:
                return "MALE";
            case FEMALE_RESIDENT_REPRESENTATIVE:
            case FEMALE_NON_RESIDENT_REPRESENTATIVE:
                return "FEMALE";
            default:
                return "";
        }
    }

    public static boolean isGenderEligible(String rawGender, PositionCategory category) {
        String required = requiredGender(category);
        if (required.isEmpty()) {
            return true;
        }
        return required.equals(normalizeGender(rawGender));
    }

    public static boolean hasResidencyRequirement(PositionCategory category) {
        if (category == null) {
            return false;
        }
        return category == PositionCategory.MALE_RESIDENT_REPRESENTATIVE
                || category == PositionCategory.MALE_NON_RESIDENT_REPRESENTATIVE
                || category == PositionCategory.FEMALE_RESIDENT_REPRESENTATIVE
                || category == PositionCategory.FEMALE_NON_RESIDENT_REPRESENTATIVE;
    }

    public static boolean requiresResident(PositionCategory category) {
        if (category == null) {
            return false;
        }
        return category == PositionCategory.MALE_RESIDENT_REPRESENTATIVE
                || category == PositionCategory.FEMALE_RESIDENT_REPRESENTATIVE;
    }

    public static boolean isResidencyEligible(boolean resident, PositionCategory category) {
        if (!hasResidencyRequirement(category)) {
            return true;
        }
        return resident == requiresResident(category);
    }

    public static String requiredResidencyLabel(PositionCategory category) {
        if (!hasResidencyRequirement(category)) {
            return "ANY";
        }
        return requiresResident(category) ? "RESIDENT" : "NON-RESIDENT";
    }

    public static String profileResidencyLabel(boolean resident) {
        return resident ? "RESIDENT" : "NON-RESIDENT";
    }

    private static String normalizePositionName(String rawPositionName) {
        if (rawPositionName == null) {
            return "";
        }

        String normalized = rawPositionName.toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replace('/', ' ');
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }
}
