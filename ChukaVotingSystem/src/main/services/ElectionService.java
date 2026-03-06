package main.services;

import main.dao.ElectionDAO;
import main.models.Election;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class ElectionService {

    private ElectionDAO electionDAO;

    public ElectionService() {
        this.electionDAO = new ElectionDAO();
    }

    public String createElection(String title, int facultyId, Timestamp startDate,
                                 Timestamp endDate, int adminId) {
        if (title == null || title.trim().isEmpty()) return "❌ Election title is required.";
        if (startDate.after(endDate))                return "❌ Start date must be before end date.";

        Election e = new Election();
        e.setTitle(title);
        e.setFacultyId(facultyId);
        e.setStartDate(startDate);
        e.setEndDate(endDate);
        e.setCreatedBy(adminId);

        boolean success = electionDAO.createElection(e);
        return success ? "✅ Election created successfully!" : "❌ Failed to create election.";
    }

    public boolean activateElection(int electionId) {
        return electionDAO.updateStatus(electionId, "ACTIVE");
    }

    public boolean closeElection(int electionId) {
        return electionDAO.updateStatus(electionId, "CLOSED");
    }

    public boolean addCandidateToElection(int electionId, int applicationId) {
        return electionDAO.addCandidateToElection(electionId, applicationId);
    }

    public List<Election> getElectionsByFaculty(int facultyId) {
        return electionDAO.getElectionsByFaculty(facultyId);
    }

    public List<Election> getActiveElectionsForFaculty(int facultyId) {
        return electionDAO.getActiveElectionsByFaculty(facultyId);
    }

    public List<Election> getAllElections() {
        return electionDAO.getAllElections();
    }

    public Election getElectionById(int electionId) {
        return electionDAO.findById(electionId);
    }

    // Auto-update statuses based on current time
    public void syncElectionStatuses() {
        List<Election> elections = electionDAO.getAllElections();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        for (Election e : elections) {
            if ("UPCOMING".equals(e.getStatus()) && now.after(e.getStartDate())) {
                electionDAO.updateStatus(e.getElectionId(), "ACTIVE");
            } else if ("ACTIVE".equals(e.getStatus()) && now.after(e.getEndDate())) {
                electionDAO.updateStatus(e.getElectionId(), "CLOSED");
            }
        }
    }
}
