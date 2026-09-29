package com.proxymed.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.proxymed.service.model.ConsultationModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Generation du PDF de la Fiche 1 (section 5.2 du cahier des charges) :
 * remplissage du template Thymeleaf "fiche-consultation" a partir du ConsultationModel complet,
 * generation du QR code (ZXing) puis conversion HTML -> PDF (openhtmltopdf).
 *
 * Le QR code encode uniquement l'UUID de la consultation (pas d'URL) : il est insere dans
 * le template sous forme d'image PNG en data URI base64.
 *
 * Ne connait que le Model : l'appelant (ConsultationService) est responsable du chargement
 * de la fiche et des regles metier.
 */
@Component
public class PdfGenerationService {

    static final String TEMPLATE_FICHE_CONSULTATION = "fiche-consultation";
    private static final int TAILLE_QR_CODE_PX = 300;

    private final ITemplateEngine templateEngine;
    private final ZoneId fuseauHoraire;

    /**
     * @param fuseauHoraire fuseau d'affichage des horodatages de signature (stockes en Instant/UTC),
     *                      independant du fuseau du serveur.
     */
    public PdfGenerationService(ITemplateEngine templateEngine,
                                @Value("${proxymed.pdf.fuseau-horaire:Africa/Dakar}") ZoneId fuseauHoraire) {
        this.templateEngine = templateEngine;
        this.fuseauHoraire = fuseauHoraire;
    }

    public byte[] genererFicheConsultation(ConsultationModel consultation) {
        String html = remplirTemplate(consultation);
        return convertirEnPdf(html);
    }

    String remplirTemplate(ConsultationModel consultation) {
        Context context = new Context(Locale.FRENCH, Map.of(
                "c", consultation,
                "qrCode", genererQrCodeBase64(consultation.id()),
                "fuseauHoraire", fuseauHoraire));
        return templateEngine.process(TEMPLATE_FICHE_CONSULTATION, context);
    }

    String genererQrCodeBase64(UUID consultationId) {
        try {
            BitMatrix matrice = new QRCodeWriter().encode(
                    consultationId.toString(), BarcodeFormat.QR_CODE, TAILLE_QR_CODE_PX, TAILLE_QR_CODE_PX,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrice, "PNG", png);
            return Base64.getEncoder().encodeToString(png.toByteArray());
        } catch (WriterException e) {
            throw new IllegalStateException("Impossible de generer le QR code de la consultation " + consultationId, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] convertirEnPdf(String html) {
        try (ByteArrayOutputStream pdf = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(pdf);
            builder.run();
            return pdf.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Echec de la conversion HTML -> PDF de la fiche", e);
        }
    }
}
