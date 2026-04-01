package main.services;

import main.models.Candidate;
import main.models.Student;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailService {

    private static EmailService instance;
    private final Properties props;
    private final String username;
    private final String password;
    private final ExecutorService executor;

    private EmailService() {
        this.props = new Properties();
        this.executor = Executors.newFixedThreadPool(2);

        loadConfig();
        this.username = props.getProperty("mail.smtp.username");
        this.password = props.getProperty("mail.smtp.password");
    }

    public static synchronized EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    private void loadConfig() {
        InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");
        if (input == null) {
            input = getClass().getResourceAsStream("/config.properties");
        }

        if (input != null) {
            try {
                props.load(input);
            } catch (IOException e) {
                System.err.println("Error loading email config: " + e.getMessage());
            } finally {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore close failure.
                }
            }
        } else {
            System.err.println("config.properties not found for EmailService.");
        }
    }

    private void sendEmail(String to, String subject, String body) {
        executor.submit(() -> {
            if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
                System.out.println("SMTP credentials detected, but JavaMail is not bundled in this build. Falling back to simulation.");
            }

            System.out.println("========================================");
            System.out.println("EMAIL SIMULATION");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + body);
            System.out.println("========================================");
        });
    }

    public void sendWelcomeEmail(Student student) {
        String subject = "Welcome to Chuka University Voting System";
        String body = String.format(
                "Hello %s,\n\n"
                        + "Your account has been successfully created.\n"
                        + "Registration Number: %s\n"
                        + "Faculty ID: %d\n\n"
                        + "Please log in and verify your phone number via OTP to start participating in elections.\n\n"
                        + "Regards,\n"
                        + "The Election Committee",
                student.getFullName(), student.getRegNumber(), student.getFacultyId()
        );
        sendEmail(student.getEmail(), subject, body);
    }

    public void sendCandidateStatusEmail(Candidate candidate, boolean approved, String reason) {
        String status = approved ? "APPROVED" : "REJECTED";
        StringBuilder body = new StringBuilder();
        body.append(String.format("Hello %s,\n\n", candidate.getStudentName()));
        body.append(String.format("Your application for the position of %s has been %s.\n",
                candidate.getPositionName(), status));

        if (!approved && reason != null && !reason.isBlank()) {
            body.append("\nReason for rejection: ").append(reason).append("\n");
        } else if (approved) {
            body.append("\nCongratulations! You are now eligible to seek peer nominations and appear on the ballot.\n");
        }

        body.append("\nRegards,\nChuka University Election Board");
    }

    public void sendVoteConfirmationEmail(Student student, String electionName, String positionName) {
        String subject = "Vote Confirmation - " + electionName;
        String body = String.format(
                "Hello %s,\n\n"
                        + "This is to confirm that you have successfully cast your vote in the '%s' election "
                        + "for the position of '%s'.\n\n"
                        + "Thank you for participating in the democratic process at Chuka University.\n\n"
                        + "Regards,\n"
                        + "Chuka University Voting System",
                student.getFullName(), electionName, positionName
        );
        sendEmail(student.getEmail(), subject, body);
    }

    public void sendCandidateStatusEmail(String email, String name, String position, boolean approved, String reason) {
        String status = approved ? "APPROVED" : "REJECTED";
        String subject = "Candidate Application Status: " + status;

        String body = String.format(
                "Hello %s,\n\n"
                        + "Your application for the position of %s has been %s.\n",
                name, position, status
        );

        if (!approved && reason != null) {
            body += "\nReason: " + reason + "\n";
        }

        body += "\nRegards,\nChuka University Election Board";
        sendEmail(email, subject, body);
    }
}
