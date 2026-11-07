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
import com.proxymed.service.model.RapportFicheModel;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Genere le PDF de la Fiche 1 : template Thymeleaf -> XHTML -> openhtmltopdf, avec un QR code
 * qui encode le type de fiche, l'id du patient et l'id de la consultation (cf.
 * {@link GenerateurContenuQr}) : c'est lui qui permet de ranger automatiquement la fiche scannee a l'import.
 */
@Component
public class PdfGenerationService {

    static final String TEMPLATE_FICHE_CONSULTATION = "fiche-consultation";
    static final String TEMPLATE_RAPPORT_FICHE = "rapport-fiche";
    private static final byte[] SIGNATURE_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int TAILLE_QR_CODE_PX = 300;

    private final ITemplateEngine templateEngine;
    private final GenerateurContenuQr generateurContenuQr;
    private final ZoneId fuseauHoraire;


    public PdfGenerationService(ITemplateEngine templateEngine,
                                GenerateurContenuQr generateurContenuQr,
                                @Value("${proxymed.pdf.fuseau-horaire:Africa/Dakar}") ZoneId fuseauHoraire) {
        this.templateEngine = templateEngine;
        this.generateurContenuQr = generateurContenuQr;
        this.fuseauHoraire = fuseauHoraire;
    }

    /**
     * Le PDF est ecrit dans un fichier temporaire, renvoye sous forme de Resource : il est
     * supprime des que son contenu a ete lu (cf. {@link FichePdfTemporaire}).
     */
    public Resource genererFicheConsultation(ConsultationModel consultation) {
        String html = remplirTemplate(consultation);
        return ecrireDansFichierTemporaire(pdf -> convertirEnPdf(html, pdf));
    }

    /**
     * Un seul PDF pour plusieurs fiches, dans l'ordre de la liste : chaque fiche est rendue a part
     * (meme template, meme QR), puis les documents sont mis bout a bout. Chaque fiche commence donc
     * sur une nouvelle page et garde sa propre numerotation "Page x / y". Meme Resource a usage unique.
     */
    public Resource genererFichesCombinees(List<ConsultationModel> fiches) {
        if (fiches.isEmpty()) {
            throw new IllegalArgumentException("Aucune fiche a generer");
        }
        List<byte[]> pdfs = fiches.stream().map(fiche -> {
            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            convertirEnPdf(remplirTemplate(fiche), pdf);
            return pdf.toByteArray();
        }).toList();
        return ecrireDansFichierTemporaire(destination -> {
            PDFMergerUtility fusion = new PDFMergerUtility();
            for (byte[] pdf : pdfs) {
                fusion.addSource(new RandomAccessReadBuffer(pdf));
            }
            fusion.setDestinationStream(destination);
            fusion.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
        });
    }

    /**
     * Rapport d'une fiche visee : page de garde (template rapport-fiche, sans donnee medicale) suivie
     * du scan signe, ajoute tel quel. L'import n'accepte que des PDF ; une image (JPEG, PNG...) est
     * tout de meme acceptee ici et placee seule sur une page A4, a l'echelle. Meme Resource a usage unique.
     */
    public Resource genererRapport(RapportFicheModel rapport) {
        Context context = new Context(Locale.FRENCH, Map.of("r", rapport, "fuseauHoraire", fuseauHoraire));
        ByteArrayOutputStream pageDeGarde = new ByteArrayOutputStream();
        convertirEnPdf(templateEngine.process(TEMPLATE_RAPPORT_FICHE, context), pageDeGarde);
        byte[] scan = estPdf(rapport.scan()) ? rapport.scan() : imageEnPdf(rapport.scan());
        return ecrireDansFichierTemporaire(destination -> {
            PDFMergerUtility fusion = new PDFMergerUtility();
            fusion.addSource(new RandomAccessReadBuffer(pageDeGarde.toByteArray()));
            fusion.addSource(new RandomAccessReadBuffer(scan));
            fusion.setDestinationStream(destination);
            fusion.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
        });
    }

    private static boolean estPdf(byte[] contenu) {
        return contenu != null && contenu.length >= SIGNATURE_PDF.length
                && Arrays.equals(contenu, 0, SIGNATURE_PDF.length, SIGNATURE_PDF, 0, SIGNATURE_PDF.length);
    }

    /** Une page A4 portant l'image, reduite pour tenir dans la page en gardant ses proportions. */
    private static byte[] imageEnPdf(byte[] image) {
        try (PDDocument document = new PDDocument()) {
            PDImageXObject img = PDImageXObject.createFromByteArray(document, image, "scan");
            PDRectangle a4 = PDRectangle.A4;
            float echelle = Math.min(1f, Math.min(a4.getWidth() / img.getWidth(), a4.getHeight() / img.getHeight()));
            float largeur = img.getWidth() * echelle;
            float hauteur = img.getHeight() * echelle;
            PDPage page = new PDPage(a4);
            document.addPage(page);
            try (PDPageContentStream contenu = new PDPageContentStream(document, page)) {
                contenu.drawImage(img, (a4.getWidth() - largeur) / 2, (a4.getHeight() - hauteur) / 2, largeur, hauteur);
            }
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Le scan de la fiche n'est ni un PDF ni une image lisible", e);
        }
    }

    String remplirTemplate(ConsultationModel consultation) {
        Context context = new Context(Locale.FRENCH, Map.of(
                "c", consultation,
                "qrCode", genererQrCodeBase64(generateurContenuQr.construire(consultation)),
                "fuseauHoraire", fuseauHoraire));
        return templateEngine.process(TEMPLATE_FICHE_CONSULTATION, context);
    }

    String genererQrCodeBase64(String contenu) {
        try {
            BitMatrix matrice = new QRCodeWriter().encode(
                    contenu, BarcodeFormat.QR_CODE, TAILLE_QR_CODE_PX, TAILLE_QR_CODE_PX,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrice, "PNG", png);
            return Base64.getEncoder().encodeToString(png.toByteArray());
        } catch (WriterException e) {
            throw new IllegalStateException("Impossible de generer le QR code : " + contenu, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void convertirEnPdf(String html, OutputStream pdf) {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(pdf);
        try {
            builder.run();
        } catch (IOException e) {
            throw new UncheckedIOException("Echec de la conversion HTML -> PDF de la fiche", e);
        }
    }

    private Resource ecrireDansFichierTemporaire(EcriturePdf ecriture) {
        try {
            var file = File.createTempFile("fiche-consultation-", ".pdf");
            try (var pdf = new FileOutputStream(file)) {
                ecriture.ecrire(pdf);
            } catch (IOException | RuntimeException e) {
                file.delete();
                throw e;
            }
            return new FichePdfTemporaire(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Echec de l'ecriture du PDF de la fiche", e);
        }
    }

    @FunctionalInterface
    private interface EcriturePdf {
        void ecrire(OutputStream destination) throws IOException;
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
