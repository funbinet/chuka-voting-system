package main.utils;

import main.models.Candidate;
import main.models.Election;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResultsPDFExporter {

    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    public static void exportElectionResults(Election election, List<Candidate> candidates,
                                             Map<Integer, Integer> voteData, int totalEligible,
                                             int totalVotesCast, File file) throws IOException {

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = 750;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                contentStream.beginText();
                contentStream.setFont(FONT_BOLD, 18);
                contentStream.newLineAtOffset(200, y);
                contentStream.showText("CHUKA UNIVERSITY");
                contentStream.endText();

                y -= 25;
                contentStream.beginText();
                contentStream.setFont(FONT_BOLD, 14);
                contentStream.newLineAtOffset(180, y);
                contentStream.showText("Electoral Commission Official Results");
                contentStream.endText();

                y -= 40;
                contentStream.beginText();
                contentStream.setFont(FONT_BOLD, 12);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("Election: " + election.getTitle());
                y -= 15;
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Faculty: " + election.getFacultyName());
                y -= 15;
                contentStream.newLineAtOffset(0, -15);
                contentStream.setFont(FONT_REGULAR, 10);
                contentStream.showText("Period: " + sdf.format(election.getStartDate()) + " to " + sdf.format(election.getEndDate()));
                contentStream.endText();

                y -= 30;

                Map<String, List<Candidate>> byPosition = new LinkedHashMap<>();
                for (Candidate c : candidates) {
                    byPosition.computeIfAbsent(c.getPositionName(), k -> new ArrayList<>()).add(c);
                }

                for (Map.Entry<String, List<Candidate>> entry : byPosition.entrySet()) {
                    y -= 20;
                    contentStream.beginText();
                    contentStream.setFont(FONT_BOLD, 11);
                    contentStream.newLineAtOffset(50, y);
                    contentStream.showText("POSITION: " + entry.getKey().toUpperCase());
                    contentStream.endText();

                    y -= 15;
                    drawTableRow(contentStream, 50, y, "Candidate Name", "Reg Number", "Votes", "%");
                    y -= 12;

                    int posTotal = 0;
                    for (Candidate c : entry.getValue()) {
                        posTotal += voteData.getOrDefault(c.getApplicationId(), 0);
                    }

                    for (Candidate c : entry.getValue()) {
                        int votes = voteData.getOrDefault(c.getApplicationId(), 0);
                        double pct = posTotal == 0 ? 0 : (votes * 100.0 / posTotal);

                        contentStream.setFont(FONT_REGULAR, 10);
                        drawTableRow(contentStream, 50, y, c.getStudentName(), c.getRegNumber(),
                                String.valueOf(votes), String.format("%.1f%%", pct));
                        y -= 12;
                    }
                    y -= 10;
                }

                y -= 30;
                contentStream.setLineWidth(1f);
                contentStream.moveTo(50, y + 20);
                contentStream.lineTo(550, y + 20);
                contentStream.stroke();

                contentStream.beginText();
                contentStream.setFont(FONT_BOLD, 12);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("VOTER TURNOUT SUMMARY");
                y -= 18;
                contentStream.newLineAtOffset(0, -18);
                contentStream.setFont(FONT_REGULAR, 11);

                double turnoutPct = totalEligible == 0 ? 0 : (totalVotesCast * 100.0 / totalEligible);
                contentStream.showText("Total Votes Cast: " + totalVotesCast);
                y -= 15;
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Total Eligible Voters: " + totalEligible);
                y -= 15;
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Turnout Percentage: " + String.format("%.2f%%", turnoutPct));
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(FONT_ITALIC, 9);
                contentStream.newLineAtOffset(180, 50);
                contentStream.showText("Official Results - Chuka University Electoral Commission");
                contentStream.endText();
            }

            document.save(file);
        }
    }

    private static void drawTableRow(PDPageContentStream stream, float x, float y,
                                     String col1, String col2, String col3, String col4) throws IOException {
        stream.beginText();
        stream.newLineAtOffset(x, y);
        stream.showText(truncate(col1, 25));
        stream.newLineAtOffset(180, 0);
        stream.showText(col2);
        stream.newLineAtOffset(120, 0);
        stream.showText(col3);
        stream.newLineAtOffset(60, 0);
        stream.showText(col4);
        stream.endText();
    }

    private static String truncate(String text, int limit) {
        if (text == null) {
            return "";
        }
        return text.length() > limit ? text.substring(0, limit - 2) + ".." : text;
    }
}
