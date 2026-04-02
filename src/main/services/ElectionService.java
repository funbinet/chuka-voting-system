package main.services;

import main.dao.CandidateDAO;
import main.dao.ElectionDAO;
import main.models.Election;
import main.utils.Constants;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class ElectionService {

    private final ElectionDAO electionDAO;
    private final CandidateDAO candidateDAO;
    private final AuditService auditService;

    public ElectionService() {
        this.electionDAO = new ElectionDAO();
        this.candidateDAO = new CandidateDAO();
        this.auditService = AuditService.getInstance();
    }

    public String createElection(String title, Integer facultyId, int positionId, Timestamp startDate, Timestamp endDate, int adminId) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        if (title == null || title.trim().isEmpty()) {
            return "Election title is required.";
        }
        if (startDate == null || endDate == null) {
            return "Both the start date and end date are required.";
        }
        if (!startDate.after(now) || !endDate.after(now)) {
            auditService.log(null, "ELECTION_CREATE_FAILED", "Admin " + adminId + " attempted to create an election with a past date.");
            return "Election dates must be in the future.";
        }
        if (!endDate.after(startDate)) {
            return "The end date must be later than the start date.";
        }
        
        if (candidateDAO.countApprovedCandidatesForBind(facultyId, positionId) == 0) {
            return "Cannot create election: No approved candidates found for this position" + (facultyId != null && facultyId > 0 ? " in this faculty." : ".");
        }

        Election election = new Election();
        election.setTitle(title.trim());
        election.setFacultyId(facultyId != null ? facultyId : 0);
        election.setPositionId(positionId);
        election.setStartDate(startDate);
        election.setEndDate(endDate);
        election.setStatus(Constants.STATUS_UPCOMING);
        election.setCreatedBy(adminId);

        int electionId = electionDAO.createElection(election);
        if (electionId > 0) {
            List<main.models.Candidate> candidates = candidateDAO.getApprovedCandidatesForElectionBind(facultyId, positionId);
            int count = 0;
            for (main.models.Candidate c : candidates) {
                if (candidateDAO.addCandidateToElection(electionId, c.getApplicationId())) {
                    count++;
                }
            }
            auditService.log(null, "ELECTION_CREATED", "Admin " + adminId + " created election '" + title.trim() + "' for position " + positionId + " with " + count + " candidates.");
            return "Election created successfully.";
        }
        return "The election could not be created.";
    }

    public boolean activateElection(int electionId) {
        if (!electionDAO.hasCandidates(electionId)) {
            auditService.log(null, "ELECTION_ACTIVATION_FAILED", "Election " + electionId + " has no candidates.");
            return false;
        }
        boolean ok = electionDAO.updateStatus(electionId, Constants.STATUS_ACTIVE);
        if (ok) {
            auditService.log(null, "ELECTION_ACTIVATED", "Election ID " + electionId + " set to ACTIVE.");
        }
        return ok;
    }

    public boolean closeElection(int electionId) {
        boolean ok = electionDAO.updateStatus(electionId, Constants.STATUS_CLOSED);
        if (ok) {
            auditService.log(null, "ELECTION_CLOSED", "Election ID " + electionId + " set to CLOSED.");
        }
        return ok;
    }

    public boolean addCandidateToElection(int electionId, int applicationId) {
        return electionDAO.addCandidateToElection(electionId, applicationId);
    }

    public List<Election> getElectionsByFaculty(int facultyId) {
        syncElectionStatuses();
        return electionDAO.getElectionsByFaculty(facultyId);
    }

    public List<Election> getActiveElectionsForFaculty(int facultyId) {
        syncElectionStatuses();
        return electionDAO.getActiveElectionsByFaculty(facultyId);
    }

    public List<Election> getAllActiveElections() {
        syncElectionStatuses();
        return electionDAO.getAllActiveElections();
    }

    public List<Election> getAllElections() {
        syncElectionStatuses();
        return electionDAO.getAllElections();
    }

    public Election getElectionById(int electionId) {
        syncElectionStatuses();
        return electionDAO.findById(electionId);
    }

    public void syncElectionStatuses() {
        List<Election> elections = electionDAO.getAllElections();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        for (Election election : elections) {
            if (Constants.STATUS_UPCOMING.equals(election.getStatus()) && !now.before(election.getStartDate())) {
                if (electionDAO.hasCandidates(election.getElectionId())) {
                    electionDAO.updateStatus(election.getElectionId(), Constants.STATUS_ACTIVE);
                } else {
                    // If start time reached but no candidates, we keep it upcoming or close it if end date also passed
                    if (now.after(election.getEndDate())) {
                        electionDAO.updateStatus(election.getElectionId(), Constants.STATUS_CLOSED);
                    }
                }
            }
            if (Constants.STATUS_ACTIVE.equals(election.getStatus()) && now.after(election.getEndDate())) {
                electionDAO.updateStatus(election.getElectionId(), Constants.STATUS_CLOSED);
                auditService.log(null, "ELECTION_AUTO_CLOSED",
                        "Election ID " + election.getElectionId() + " closed automatically after end date.");
            }
        }
    }

    public String updateElection(int electionId, String newTitle, Integer facultyId, int positionId, Timestamp newStart, Timestamp newEnd, int adminId) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        if (newTitle == null || newTitle.trim().isEmpty()) {
            return "Election title is required.";
        }
        if (newStart == null || newEnd == null) {
            return "Both start date and end date are required.";
        }
        if (!newEnd.after(newStart)) {
            return "The end date must be later than the start date.";
        }

        Election existing = electionDAO.findById(electionId);
        if (existing == null) {
            return "Election not found.";
        }

        boolean updated = electionDAO.updateElection(electionId, newTitle.trim(), facultyId, positionId, newStart, newEnd);
        if (updated) {
            auditService.log(null, "ELECTION_UPDATED", "Admin " + adminId + " updated election ID " + electionId);
            syncElectionStatuses();
            return "Election updated successfully.";
        }
        return "Failed to update the election.";
    }

    private boolean isSameTimeFrame(Election e, Timestamp s, Timestamp d) {
        return e.getStartDate().equals(s) && e.getEndDate().equals(d);
    }

    public String deleteElection(int electionId, int adminId) {
        Election e = electionDAO.findById(electionId);
        if (e == null) {
            return "Election not found.";
        }
        
        boolean deleted = electionDAO.deleteElection(electionId);
        if (deleted) {
            auditService.log(null, "ELECTION_DELETED", "Admin " + adminId + " deleted election ID " + electionId + " (" + e.getTitle() + ")");
            return "Election deleted successfully.";
        }
        return "Failed to delete the election (database error).";
    }
}