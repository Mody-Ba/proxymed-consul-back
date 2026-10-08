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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


@Component
public class PdfGenerationService {

    static final String TEMPLATE_FICHE_CONSULTATION = "fiche-consultation";
    private static final int TAILLE_QR_CODE_PX = 300;

    private final ITemplateEngine templateEngine;
    private final ZoneId fuseauHoraire;


    public PdfGenerationService(ITemplateEngine templateEngine,
                                @Value("${proxymed.pdf.fuseau-horaire:Africa/Dakar}") ZoneId fuseauHoraire) {
        this.templateEngine = templateEngine;
        this.fuseauHoraire = fuseauHoraire;
    }

    /**
     * Le PDF est ecrit dans un fichier temporaire, renvoye sous forme de Resource : il est
     * supprime des que son contenu a ete lu (cf. {@link FichePdfTemporaire}).
     */
    public Resource genererFicheConsultation(ConsultationModel consultation) {
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

    private Resource convertirEnPdf(String html) {
        try {
            var file = File.createTempFile("fiche-consultation-", ".pdf");
            try (var pdf = new FileOutputStream(file)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);
                builder.toStream(pdf);
                builder.run();
            } catch (IOException | RuntimeException e) {
                file.delete();
                throw e;
            }
            return new FichePdfTemporaire(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Echec de la conversion HTML -> PDF de la fiche", e);
        }
    }

    /**
     * Fichier PDF a usage unique : supprime du disque a la fermeture du flux de lecture,
     * pour ne pas accumuler un fichier temporaire par telechargement.
     */
    private static final class FichePdfTemporaire extends FileSystemResource {

        private FichePdfTemporaire(File file) {
            super(file);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(getFile().toPath(), StandardOpenOption.DELETE_ON_CLOSE);
        }
    }
}
