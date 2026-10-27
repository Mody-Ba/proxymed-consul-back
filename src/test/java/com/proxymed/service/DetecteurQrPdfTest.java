package com.proxymed.service;

import com.proxymed.exception.RegleGestionException;
import com.proxymed.support.PdfDeTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetecteurQrPdfTest {

    private final DetecteurQrPdf detecteur = new DetecteurQrPdf();

    @Test
    void detecter_chercheSurToutesLesPages_dansLOrdre() {
        byte[] pdf = PdfDeTest.pdfAvecQr(null, "page 2", null, "page 4");

        assertThat(detecteur.detecter(pdf)).containsExactly("page 2", "page 4");
    }

    @Test
    void detecter_litLeQrDUnPdfScanne() {
        byte[] scan = PdfDeTest.numeriser(PdfDeTest.pdfAvecQr("{\"type\":\"FICHE_1_CONSULTATION_INITIALE\"}"), 200);

        assertThat(detecteur.detecter(scan)).containsExactly("{\"type\":\"FICHE_1_CONSULTATION_INITIALE\"}");
    }

    @Test
    void detecter_renvoieUneListeVide_sansQrCode() {
        assertThat(detecteur.detecter(PdfDeTest.pdfAvecQr(null, null))).isEmpty();
    }

    @Test
    void detecter_rejetteUnPdfEndommage() {
        byte[] endommage = "%PDF-1.7\nfiche tronquee".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> detecteur.detecter(endommage))
                .isInstanceOf(RegleGestionException.class)
                .hasMessage("Le fichier PDF est illisible ou endommage");
    }
}
