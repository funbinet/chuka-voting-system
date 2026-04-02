package main.utils;

import java.awt.Color;
import java.awt.Font;

public class Constants {

    // OTP
    public static final int    OTP_LENGTH      = 6;
    public static final int    OTP_EXPIRY_MINS = 5;
    public static final int    OTP_MAX_ATTEMPTS = 3;

    // Candidate Eligibility
    public static final double MIN_GPA           = 2.5;
    public static final int    MIN_YEAR_OF_STUDY = 2;
    public static final int    MIN_NOMINATIONS   = 10;
    public static final int    MIN_MANIFESTO_LENGTH = 50;

    // Election Status
    public static final String STATUS_UPCOMING = "UPCOMING";
    public static final String STATUS_ACTIVE   = "ACTIVE";
    public static final String STATUS_CLOSED   = "CLOSED";

    // Application Status
    public static final String APP_PENDING  = "PENDING";
    public static final String APP_APPROVED = "APPROVED";
    public static final String APP_REJECTED = "REJECTED";

    // Roles
    public static final String ROLE_STUDENT   = "STUDENT";
    public static final String ROLE_ADMIN     = "ADMIN";
    public static final String ROLE_CANDIDATE = "CANDIDATE";

    // Chuka University Theme Colors
    public static final Color COLOR_PRIMARY   = new Color(26, 82, 118);   // Dark Blue
    public static final Color COLOR_SECONDARY = new Color(46, 134, 193);  // Light Blue
    public static final Color COLOR_ACCENT    = new Color(243, 156, 18);  // Gold
    public static final Color COLOR_BG        = new Color(244, 246, 247); // Light Grey
    public static final Color COLOR_WHITE     = Color.WHITE;
    public static final Color COLOR_SUCCESS   = new Color(30, 132, 73);   // Green
    public static final Color COLOR_DANGER    = new Color(192, 57, 43);   // Red
    public static final Color COLOR_TEXT      = new Color(33, 33, 33);

    // Fonts
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BUTTON  = new Font("Segoe UI", Font.BOLD, 13);

    // App Info
    public static final String APP_NAME    = "Chuka University Voting System";
    public static final String APP_VERSION = "1.0.0";
}
