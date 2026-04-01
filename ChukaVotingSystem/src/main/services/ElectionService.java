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

    public String createElection(String title, int facultyId, Timestamp startDate, Timestamp endDate, int adminId) {
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
        // Requirement 5: Candidates added directly, no need for pre-existing candidates check
        
        if (electionDAO.hasOverlappingElection(facultyId, startDate, endDate)) {
            auditService.log(null, "ELECTION_CREATE_FAILED", "Admin " + adminId + " attempted to create an overlapping election.");
            return "Another election already overlaps the selected period for this faculty.";
        }

        Election election = new Election();
        election.setTitle(title.trim());
        election.setFacultyId(facultyId);
        election.setStartDate(startDate);
        election.setEndDate(endDate);
        election.setStatus(Constants.STATUS_UPCOMING);
        election.setCreatedBy(adminId);

        boolean created = electionDAO.createElection(election);
        if (created) {
            auditService.log(null, "ELECTION_CREATED", "Admin " + adminId + " created election '" + title.trim() + "'.");
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
}