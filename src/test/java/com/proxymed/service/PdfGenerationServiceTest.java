package com.proxymed.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.ExamensBiologiquesPrescrits;
import com.proxymed.enums.FrequenceVisites;
import com.proxymed.enums.NiveauAutonomie;
import com.proxymed.enums.NiveauPrecarite;
import com.proxymed.enums.OrigineDemande;
import com.proxymed.enums.RisqueIsolement;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.enums.TypeConstanteVitale;
import com.proxymed.service.model.AntecedentMaladieModel;
import com.proxymed.service.model.ConstanteVitaleModel;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.ExamenParAppareilModel;
import com.proxymed.service.model.FacteurDeRisqueModel;
import com.proxymed.service.model.MedecinModel;
import com.proxymed.service.model.PatientModel;
import com.proxymed.service.model.SituationSocialeModel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste le rendu complet de la Fiche 1 (template Thymeleaf reel + QR code + conversion PDF),
 * sans contexte Spring. Le PDF d'exemple est ecrit dans target/ pour verification visuelle.
 */
class PdfGenerationServiceTest {

    private PdfGenerationService pdfGenerationService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        pdfGenerationService = new PdfGenerationService(templateEngine, ZoneId.of("Africa/Dakar"));
    }

    @Test
    void genererFicheConsultation_produitUnPdfAvecLesHuitSectionsEtLesDonnees() throws Exception {
        ConsultationModel consultation = ficheComplete();

        byte[] pdf = pdfGenerationService.genererFicheConsultation(consultation);
        Files.write(Path.of("target", "fiche-consultation-exemple.pdf"), pdf);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String texte = new PDFTextStripper().getText(document);
            assertThat(texte).contains(
                    "1. Identification du patient",
                    "2. Motif de consultation",
                    "3. Antécédents médicaux",
                    "4. Habitudes de vie et facteurs de risque",
                    "5. Examen clinique",
                    "6. Évaluation sociale",
                    "7. Diagnostic et plan de suivi",
                    "8. Validation et signatures");
            assertThat(texte).contains(
                    "Mamadou Ba", "DOS-2026-0042", "Chute a domicile", "Hypertension arterielle",
                    "Tabac", "Vit seul", "Souffle systolique", "ÉLIGIBLE", "Dr Omar Fall", "M. Sy",
                    "Signé le 20/09/2026 10:15", "Hebdomadaire");
            assertThat(texte).contains(consultation.id().toString());
        }
    }

    @Test
    void genererFicheConsultation_qrCodeEncodeUniquementLUuid() throws Exception {
        ConsultationModel consultation = ficheComplete();

        byte[] pdf = pdfGenerationService.genererFicheConsultation(consultation);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            BufferedImage page = new PDFRenderer(document).renderImageWithDPI(0, 200);
            assertThat(decoderQrCode(page)).isEqualTo(consultation.id().toString());
        }
    }

    @Test
    void genererQrCodeBase64_produitUnPngDecodableContenantLUuid() throws Exception {
        UUID id = UUID.randomUUID();

        byte[] png = Base64.getDecoder().decode(pdfGenerationService.genererQrCodeBase64(id));

        BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertThat(decoderQrCode(image)).isEqualTo(id.toString());
    }

    @Test
    void genererFicheConsultation_supporteUnBrouillonQuasiVide() throws Exception {
        ConsultationModel brouillon = ConsultationModel.builder()
                .id(UUID.randomUUID())
                .statut(StatutConsultation.BROUILLON)
                .patient(PatientModel.builder().nomComplet("Awa Ndiaye").build())
                .maladiesChroniques(List.of())
                .facteursDeRisque(List.of())
                .situationSociale(List.of())
                .constantesVitales(List.of())
                .build();

        byte[] pdf = pdfGenerationService.genererFicheConsultation(brouillon);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String texte = new PDFTextStripper().getText(document);
            assertThat(texte).contains("Awa Ndiaye", "BROUILLON", "Aucune constante vitale saisie", "Non signé");
        }
    }

    private String decoderQrCode(BufferedImage image) throws Exception {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        return new QRCodeReader().decode(bitmap, Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE)).getText();
    }

    private ConsultationModel ficheComplete() {
        UUID id = UUID.randomUUID();
        return ConsultationModel.builder()
                .id(id)
                .statut(StatutConsultation.SIGNEE)
                .dateConsultation(LocalDate.of(2026, 9, 20))
                .heureConsultation(LocalTime.of(9, 30))
                .patient(PatientModel.builder()
                        .id(UUID.randomUUID())
                        .numeroDossierProxymed("DOS-2026-0042")
                        .numeroDmi("DMI-7781")
                        .nomComplet("Mamadou Ba")
                        .dateNaissance(LocalDate.of(1948, 6, 1))
                        .age(78)
                        .sexe(Sexe.M)
                        .telephone("77 123 45 67")
                        .adresseDomicile("Villa 12, rue 7")
                        .commune("Dakar Plateau")
                        .quartier("Médina")
                        .personneAContacterNom("Fatou Ba (fille)")
                        .personneAContacterTelephone("76 555 44 33")
                        .couvertureSociale(CouvertureSociale.IPM)
                        .build())
                .medecinSenior(MedecinModel.builder().id(1L).prenom("Awa").nom("Diop").numeroOrdre("SEN-001")
                        .role(RoleMedecin.SENIOR).structureRattachementLibelle("SAMU National").build())
                .origineDemande(OrigineDemande.SAMU)
                .motifPrincipalConsultation("Chute a domicile, douleur de hanche droite")
                .maladiesChroniques(List.of(
                        AntecedentMaladieModel.builder().maladieChroniqueLibelle("Hypertension arterielle").precision("Sous amlodipine").build(),
                        AntecedentMaladieModel.builder().maladieChroniqueLibelle("Diabete type 2").build()))
                .precisionsMaladiesChroniques("HTA connue depuis 2010")
                .allergiesConnues(true)
                .precisionsAllergiesMedicamenteuses("Penicilline")
                .hospitalisationsAnterieures(true)
                .chirurgiesAnterieures(false)
                .detailsHospitalisationsChirurgies("Hospitalisation 2022 pour decompensation diabetique")
                .facteursDeRisque(List.of(
                        FacteurDeRisqueModel.builder().id(1L).libelle("Tabac").build(),
                        FacteurDeRisqueModel.builder().id(2L).libelle("Sedentarite").build()))
                .situationSociale(List.of(SituationSocialeModel.builder().id(1L).libelle("Vit seul").build()))
                .constantesVitales(List.of(
                        ConstanteVitaleModel.builder().type(TypeConstanteVitale.TA).valeur(new BigDecimal("150"))
                                .heure(LocalTime.of(9, 40)).estNormal(false).estAlerte(false).commentaire("Bras gauche").build(),
                        ConstanteVitaleModel.builder().type(TypeConstanteVitale.SPO2).valeur(new BigDecimal("88"))
                                .heure(LocalTime.of(9, 42)).estNormal(false).estAlerte(true).build(),
                        ConstanteVitaleModel.builder().type(TypeConstanteVitale.POIDS).valeur(new BigDecimal("70"))
                                .estNormal(true).estAlerte(false).build(),
                        ConstanteVitaleModel.builder().type(TypeConstanteVitale.IMC).valeur(new BigDecimal("22.9"))
                                .estNormal(true).estAlerte(false).build()))
                .examenParAppareil(ExamenParAppareilModel.builder()
                        .etatGeneral("Altere, conscient")
                        .cardioVasculaire("Souffle systolique 2/6")
                        .respiratoire("RAS")
                        .digestif("RAS")
                        .neurologique("Pas de deficit")
                        .locomoteurCutaneAutre("Hematome hanche droite")
                        .build())
                .niveauAutonomie(NiveauAutonomie.PARTIELLEMENT_DEPENDANT)
                .risqueIsolement(RisqueIsolement.ELEVE)
                .niveauPrecarite(NiveauPrecarite.MODEREE)
                .besoinsSociauxIdentifies("Aide a domicile, portage de repas")
                .resumeSyndromiqueCim10("W19 - Chute ; I10 - HTA")
                .decisionEligibilite(DecisionEligibilite.ELIGIBLE)
                .medecinJuniorAffecte(MedecinModel.builder().id(2L).prenom("Omar").nom("Fall").role(RoleMedecin.JUNIOR).build())
                .frequenceVisitesSuivi(FrequenceVisites.HEBDOMADAIRE)
                .planSuiviPersonnalise("Surveillance TA et glycemie, kinesitherapie")
                .traitementPrescrit("Paracetamol 1g x3/j")
                .examensBiologiquesPrescrits(ExamensBiologiquesPrescrits.BILAN_COMPLET)
                .dateProchaineConsultationSenior(LocalDate.of(2026, 10, 20))
                .delaiRecommande("1 mois")
                .signatureMedecinSenior(true)
                .horodatageSignatureMedecinSenior(Instant.parse("2026-09-20T10:15:00Z"))
                .signaturePointFocalDass(true)
                .horodatageSignaturePointFocalDass(Instant.parse("2026-09-21T08:00:00Z"))
                .nomPointFocalDass("M. Sy")
                .visaDirecteurSamu(false)
                .nomDirecteurSamu("Pr Ndiaye")
                .numeroFicheDmi("FDMI-2026-118")
                .dateSaisieDmi(LocalDate.of(2026, 9, 20))
                .saisiePar("Dr Diop")
                .build();
    }
}
