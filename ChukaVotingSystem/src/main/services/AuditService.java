package main.services;

import main.dao.AuditDAO;
import main.models.Student;

public class AuditService {
    private static AuditService instance;
    private AuditDAO auditDAO;

    private AuditService() {
        this.auditDAO = new AuditDAO();
    }

    public static AuditService getInstance() {
        if (instance == null) {
            instance = new AuditService();
        }
        return instance;
    }

    public void log(Integer studentId, String action, String description) {
        auditDAO.log(studentId, action, description);
    }

    public void logStudentAction(Student student, String action, String description) {
        if (student != null) {
            log(student.getStudentId(), action, description);
        } else {
            log(null, action, description);
        }
    }
}
