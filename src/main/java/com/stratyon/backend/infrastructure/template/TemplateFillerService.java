package com.stratyon.backend.infrastructure.template;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TemplateFillerService {

    private static final String TEMPLATE_PATH = "templates/report-template.docx";

    public byte[] generatePdf(Map<String, Object> data) {
        try {
            InputStream templateStream = loadTemplate();
            byte[] filledDocx = fillDocxTemplate(templateStream, data);
            return convertToPdf(filledDocx);
        } catch (Exception e) {
            log.warn("Template generation failed, using fallback: {}", e.getMessage());
            return generateFallbackPdf(data);
        }
    }

    private InputStream loadTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        if (resource.exists()) {
            return resource.getInputStream();
        }
        // Return a minimal blank DOCX if no template exists yet
        return createBlankDocx();
    }

    private byte[] fillDocxTemplate(InputStream templateStream, Map<String, Object> data) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(templateStream)) {
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                replacePlaceholdersInParagraph(paragraph, data);
            }
            // Also process tables
            doc.getTables().forEach(table ->
                    table.getRows().forEach(row ->
                            row.getTableCells().forEach(cell ->
                                    cell.getParagraphs().forEach(p ->
                                            replacePlaceholdersInParagraph(p, data)))));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    private void replacePlaceholdersInParagraph(XWPFParagraph paragraph, Map<String, Object> data) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null) return;

        // Merge all runs into one string, replace, then rewrite
        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) fullText.append(text);
        }

        String merged = fullText.toString();
        boolean changed = false;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (merged.contains(placeholder)) {
                merged = merged.replace(placeholder, entry.getValue() != null ? entry.getValue().toString() : "");
                changed = true;
            }
        }

        if (changed && !runs.isEmpty()) {
            runs.get(0).setText(merged, 0);
            for (int i = 1; i < runs.size(); i++) {
                runs.get(i).setText("", 0);
            }
        }
    }

    private byte[] convertToPdf(byte[] docxBytes) throws IOException {
        // POI XWPF to PDF conversion via basic text extraction + PDFBox
        // For production-quality conversion, use LibreOffice headless or a cloud API.
        // This produces a simple text-based PDF as a fallback.
        return generateSimplePdfFromDocx(docxBytes);
    }

    private byte[] generateSimplePdfFromDocx(byte[] docxBytes) throws IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument pdf = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            pdf.addPage(page);

            try (XWPFDocument doc = new XWPFDocument(new java.io.ByteArrayInputStream(docxBytes));
                 org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {

                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 11);
                cs.setLeading(14.5f);
                cs.newLineAtOffset(50, 750);

                for (XWPFParagraph paragraph : doc.getParagraphs()) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        // Wrap long lines
                        for (String line : wrapText(text, 90)) {
                            cs.showText(line);
                            cs.newLine();
                        }
                    }
                    cs.newLine();
                }
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        }
    }

    private byte[] generateFallbackPdf(Map<String, Object> data) {
        try (org.apache.pdfbox.pdmodel.PDDocument pdf = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            pdf.addPage(page);

            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(50, 750);
                cs.showText("Stratyon Report");
                cs.newLine();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 11);
                cs.setLeading(16f);
                cs.newLine();

                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String line = entry.getKey() + ": " + (entry.getValue() != null ? entry.getValue() : "");
                    cs.showText(line);
                    cs.newLine();
                }
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Fallback PDF generation failed: {}", e.getMessage());
            return new byte[0];
        }
    }

    private InputStream createBlankDocx() throws IOException {
        XWPFDocument doc = new XWPFDocument();
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText("{{REPORT_TITLE}}");
        doc.createParagraph().createRun().setText("Company: {{COMPANY_NAME}}");
        doc.createParagraph().createRun().setText("Date: {{REPORT_DATE}}");
        doc.createParagraph().createRun().setText("Score: {{OVERALL_SCORE}}");
        doc.createParagraph().createRun().setText("GA4 Sessions: {{GA4_SESSIONS}}");
        doc.createParagraph().createRun().setText("GA4 Users: {{GA4_USERS}}");
        doc.createParagraph().createRun().setText("GA4 Conversion Rate: {{GA4_CONVERSION_RATE}}%");
        doc.createParagraph().createRun().setText("Google Ads Spend: ${{GADS_SPEND}}");
        doc.createParagraph().createRun().setText("Google Ads ROAS: {{GADS_ROAS}}");
        doc.createParagraph().createRun().setText("Meta Reach: {{META_REACH}}");
        doc.createParagraph().createRun().setText("Meta Spend: ${{META_SPEND}}");
        doc.createParagraph().createRun().setText("Meta CPR: ${{META_CPR}}");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        doc.close();
        return new java.io.ByteArrayInputStream(out.toByteArray());
    }

    private List<String> wrapText(String text, int maxChars) {
        List<String> lines = new java.util.ArrayList<>();
        while (text.length() > maxChars) {
            int breakAt = text.lastIndexOf(' ', maxChars);
            if (breakAt < 0) breakAt = maxChars;
            lines.add(text.substring(0, breakAt));
            text = text.substring(breakAt).trim();
        }
        if (!text.isEmpty()) lines.add(text);
        return lines;
    }
}
