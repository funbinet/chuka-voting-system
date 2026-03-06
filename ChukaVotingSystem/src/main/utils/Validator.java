package main.utils;

public class Validator {

    public static boolean isValidRegNumber(String regNumber) {
        // e.g. SCT/2021/001
        return regNumber != null && regNumber.matches("[A-Z]{2,4}/\\d{4}/\\d{3}");
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^(07|01)\\d{8}$");
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    public static boolean isValidPassword(String password) {
        // Min 8 chars, at least one uppercase, one digit, one special char
        return password != null &&
               password.length() >= 8 &&
               password.matches(".*[A-Z].*") &&
               password.matches(".*\\d.*") &&
               password.matches(".*[!@#$%^&*].*");
    }

    public static boolean isValidGPA(double gpa) {
        return gpa >= 0.0 && gpa <= 4.0;
    }

    public static boolean meetsMinGPA(double gpa) {
        return gpa >= Constants.MIN_GPA;
    }

    public static boolean meetsMinYear(int year) {
        return year >= Constants.MIN_YEAR_OF_STUDY;
    }

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
