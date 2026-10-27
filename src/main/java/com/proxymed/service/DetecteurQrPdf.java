package com.proxymed.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.QRCodeReader;
import com.proxymed.exception.RegleGestionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Cherche les QR codes d'un PDF (typiquement une fiche papier passee au scanner) : chaque page
 * est rendue en image puis analysee par ZXing. Aucune regle metier ici, seulement les textes
 * decodes ; leur interpretation revient a {@link LecteurContenuQr}.
 */
@Component
public class DetecteurQrPdf {

    /**
     * 300 DPI : resolution courante d'un scanner de bureau. En dessous, les modules du QR (environ
     * 0,5 mm sur la fiche imprimee) deviennent trop petits une fois le scan recompresse.
     */
    static final int RESOLUTION_DPI = 300;

    private static final Map<DecodeHintType, Object> INDICES = Map.of(
            DecodeHintType.TRY_HARDER, Boolean.TRUE,
            DecodeHintType.CHARACTER_SET, "UTF-8");

    /**
     * Textes des QR codes trouves, dans l'ordre des pages. Liste vide si aucune page ne contient
     * de QR code lisible.
     *
     * @throws RegleGestionException si le PDF ne peut pas etre ouvert (endommage ou protege)
     */
    public List<String> detecter(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer rendu = new PDFRenderer(document);
            List<String> textes = new ArrayList<>();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                textes.addAll(decoder(rendu.renderImageWithDPI(page, RESOLUTION_DPI, ImageType.GRAY)));
            }
            return textes;
        } catch (IOException e) {
            throw new RegleGestionException("Le fichier PDF est illisible ou endommage");
        }
    }

    /**
     * Le lecteur multiple passe en premier : sur une page entiere, le lecteur simple se trompe
     * parfois de motifs de reperage alors que le lecteur multiple, qui evalue toutes les
     * combinaisons, les lit. Le binariseur hybride (seuil local) supporte bien les eclairages
     * inegaux d'un scan ; le global sert de second essai.
     */
    private List<String> decoder(BufferedImage image) {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        for (BinaryBitmap bitmap : List.of(new BinaryBitmap(new HybridBinarizer(source)),
                new BinaryBitmap(new GlobalHistogramBinarizer(source)))) {
            try {
                // Tableau vide (et non exception) quand des motifs sont reperes sans pouvoir etre decodes.
                List<String> textes = Arrays.stream(new QRCodeMultiReader().decodeMultiple(bitmap, INDICES))
                        .map(Result::getText).distinct().toList();
                if (!textes.isEmpty()) {
                    return textes;
                }
            } catch (ReaderException e) {
                // Aucun QR trouve par le lecteur multiple : essai du lecteur simple.
            }
            try {
                return List.of(new QRCodeReader().decode(bitmap, INDICES).getText());
            } catch (ReaderException e) {
                // Pas de QR lisible avec ce binariseur : essai suivant.
            }
        }
        return List.of();
    }
}
