package main.utils;

import main.services.AuthService;
import main.ui.RoleSelectionFrame;
import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;

public class SessionManager {
    private static SessionManager instance;
    private Timer inactivityTimer;
    private Timer warningTimer;
    private JFrame currentFrame;
    private JDialog currentWarningDialog;
    private AWTEventListener globalListener;

    private static final int INACTIVITY_TIMEOUT = 10 * 60 * 1000; // 10 Minutes
    private static final int GRACE_PERIOD       = 2 * 60 * 1000; // 2 Minutes

    private SessionManager() {
        inactivityTimer = new Timer(INACTIVITY_TIMEOUT, e -> showWarning());
        inactivityTimer.setRepeats(false);

        warningTimer = new Timer(GRACE_PERIOD, e -> forceLogout());
        warningTimer.setRepeats(false);

        // Define the global listener once
        globalListener = event -> resetTimer();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void startSession(JFrame frame) {
        this.currentFrame = frame;
        inactivityTimer.restart();
        
        // Attach global listener to track mouse and keyboard application-wide
        Toolkit.getDefaultToolkit().addAWTEventListener(globalListener, 
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    public void resetTimer() {
        if (warningTimer != null && !warningTimer.isRunning()) {
            inactivityTimer.restart();
        }
    }

    public void stopSession() {
        inactivityTimer.stop();
        warningTimer.stop();
        if (currentWarningDialog != null) currentWarningDialog.dispose();
        
        // Remove the listener to prevent memory leaks or dual-sessions
        Toolkit.getDefaultToolkit().removeAWTEventListener(globalListener);
        currentFrame = null;
    }

    private void showWarning() {
        warningTimer.start();
        
        SwingUtilities.invokeLater(() -> {
            Object[] options = {"Stay Logged In"};
            JOptionPane pane = new JOptionPane(
                "You will be logged out in 2 minutes due to inactivity.\nClick 'Stay Logged In' to continue.",
                JOptionPane.WARNING_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null, options, options[0]
            );

            currentWarningDialog = pane.createDialog(currentFrame, "Inactivity Warning");
            currentWarningDialog.setModal(false); 
            currentWarningDialog.setVisible(true);

            pane.addPropertyChangeListener(e -> {
                if (JOptionPane.VALUE_PROPERTY.equals(e.getPropertyName())) {
                    warningTimer.stop();
                    inactivityTimer.restart();
                    currentWarningDialog.dispose();
                }
            });
        });
    }

    private void forceLogout() {
        if (currentWarningDialog != null) currentWarningDialog.dispose();
        if (currentFrame != null) {
            AuthService.logout();
            currentFrame.dispose();
            new RoleSelectionFrame();
        }
        stopSession();
    }
}
