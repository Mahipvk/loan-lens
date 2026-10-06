package com.loanlens.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Step 1 of the pipeline: turn the PDF into plain text (Apache PDFBox). No AI involved. */
@Component
public class PdfTextExtractor {

    public String extract(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read PDF: " + e.getMessage(), e);
        }
    }
}
