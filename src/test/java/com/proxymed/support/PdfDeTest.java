package com.proxymed.support;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Fabrique des PDF pour les tests d'import : pages portant un QR code arbitraire, et
 * "numerisation" d'un PDF (chaque page rendue en image puis remise dans un nouveau PDF, comme
 * le ferait un scanner).
 */
public final class PdfDeTest {

    private PdfDeTest() {
    }

    /**
     * Un PDF A4 d'une page par texte : la page porte un QR code encodant ce texte, ou seulement
     * une ligne de texte si le texte est null.
     */
    public static byte[] pdfAvecQr(String... textesParPage) {
        try (PDDocument document = new PDDocument()) {
            for (String texte : textesParPage) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream contenu = new PDPageContentStream(document, page)) {
                    contenu.beginText();
                    contenu.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contenu.newLineAtOffset(50, 780);
                    contenu.showText("Fiche de test");
                    contenu.endText();
                    if (texte != null) {
                        BufferedImage qr = MatrixToImageWriter.toBufferedImage(new QRCodeWriter().encode(
                                texte, BarcodeFormat.QR_CODE, 300, 300, Map.of(EncodeHintType.MARGIN, 1)));
                        PDImageXObject image = LosslessFactory.createFromImage(document, qr);
                        contenu.drawImage(image, 420, 700, 100, 100);
                    }
                }
            }
            return enregistrer(document);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Simule le passage au scanner : chaque page est rendue en niveaux de gris a la resolution
     * donnee, compressee en JPEG, puis replacee seule sur une page de meme taille. Le PDF obtenu
     * ne contient plus que des images (aucun texte ni dessin vectoriel).
     */
    public static byte[] numeriser(byte[] pdf, int dpi) {
        try (PDDocument original = Loader.loadPDF(pdf); PDDocument scan = new PDDocument()) {
            PDFRenderer rendu = new PDFRenderer(original);
            for (int i = 0; i < original.getNumberOfPages(); i++) {
                PDRectangle taille = original.getPage(i).getMediaBox();
                BufferedImage image = rendu.renderImageWithDPI(i, dpi, ImageType.GRAY);
                PDPage page = new PDPage(taille);
                scan.addPage(page);
                try (PDPageContentStream contenu = new PDPageContentStream(scan, page)) {
                    contenu.drawImage(JPEGFactory.createFromImage(scan, image, 0.75f),
                            0, 0, taille.getWidth(), taille.getHeight());
                }
            }
            return enregistrer(scan);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] enregistrer(PDDocument document) throws Exception {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        document.save(sortie);
        return sortie.toByteArray();
    }
}
