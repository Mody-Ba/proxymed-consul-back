package com.proxymed.service;

import com.proxymed.entity.FicheSignee;
import com.proxymed.exception.FicheSigneeExistanteException;
import com.proxymed.exception.QrCodeIntrouvableException;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.service.mappers.FicheSigneeMapper;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.ContenuQrModel;
import com.proxymed.service.model.FicheSigneeModel;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * La seule entite JPA visible dans cette classe est FicheSignee (l'aggregat propre a ce
 * service). ConsultationInitiale (aggregat parent) n'est jamais manipulee ici : la
 * verification qu'elle existe passe par ConsultationService.findById, et le rattachement
 * JPA (id -> reference) est delegue au mapper DB.
 * A l'import, aucun id n'est fourni : la consultation est lue dans le QR code du PDF scanne
 * (DetecteurQrPdf puis LecteurContenuQr), puis recoupee avec le patient du QR. Pour un medecin
 * junior, l'assignation a la consultation est verifiee par DemandeConsultationService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FicheSigneeServiceImpl implements FicheSigneeService {

    static final long TAILLE_MAX_OCTETS = 10L * 1024 * 1024;
    static final String MESSAGE_FICHE_EXISTANTE = "Une fiche signee existe deja pour cette consultation";
    private static final String TYPE_PDF = "application/pdf";
    private static final byte[] SIGNATURE_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final FicheSigneeRepository ficheSigneeRepository;
    private final ConsultationService consultationService;
    private final PatientService patientService;
    private final DemandeConsultationService demandeConsultationService;
    private final FicheSigneeMapper ficheSigneeMapper;
    private final DetecteurQrPdf detecteurQrPdf;
    private final LecteurContenuQr lecteurContenuQr;

    @Override
    public FicheSigneeModel importer(FicheSigneeModel model, UUID juniorAControlerId) {
        if (model.contenu() != null && model.contenu().length > TAILLE_MAX_OCTETS) {
            throw new RegleGestionException("Le fichier importe depasse la taille maximale de 10 Mo");
        }
        if (!estPdf(model)) {
            throw new RegleGestionException("Le fichier importe doit etre un PDF");
        }
        ContenuQrModel qr = lireQrCode(model.contenu());
        patientService.findById(qr.patientId());
        ConsultationModel consultation = consultationService.findById(qr.consultationId());
        // Avant tout autre controle sur la consultation : un autre junior n'apprend rien de plus
        // (ni le patient, ni l'existence d'une fiche deja importee).
        if (juniorAControlerId != null) {
            demandeConsultationService.verifierJuniorAssigne(consultation.id(), juniorAControlerId);
        }
        if (!qr.patientId().equals(consultation.patient().id())) {
            throw new RegleGestionException("Le patient du code QR (" + qr.patientId()
                    + ") ne correspond pas au patient de la consultation " + qr.consultationId());
        }
        UUID consultationId = consultation.id();
        if (ficheSigneeRepository.existsByConsultation_Id(consultationId)) {
            throw new FicheSigneeExistanteException(MESSAGE_FICHE_EXISTANTE + " : " + consultationId);
        }
        FicheSigneeModel aEnregistrer = model.toBuilder().dateImport(Instant.now()).build();
        try {
            // saveAndFlush : la contrainte d'unicite est verifiee ici, et non au commit, pour que
            // l'import simultane perdant recoive lui aussi un 409.
            FicheSignee entity = ficheSigneeRepository.saveAndFlush(ficheSigneeMapper.toEntity(aEnregistrer, consultationId));
            return ficheSigneeMapper.toModel(entity);
        } catch (DataIntegrityViolationException e) {
            // Seule contrainte possible ici : uk_fiche_signee_consultation (la consultation existe, verifiee plus haut).
            throw new FicheSigneeExistanteException(MESSAGE_FICHE_EXISTANTE + " : " + consultationId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FicheSigneeModel telecharger(UUID consultationId) {
        consultationService.findById(consultationId);
        return ficheSigneeRepository.findByConsultation_Id(consultationId)
                .map(ficheSigneeMapper::toModel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune fiche signee importee pour la consultation : " + consultationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FicheSigneeModel> findByPatient(UUID patientId) {
        patientService.findById(patientId);
        return ficheSigneeRepository.findResumesByPatientId(patientId)
                .stream().map(ficheSigneeMapper::toModel).toList();
    }

    /**
     * Premier QR PROXYMED valide trouve, dans l'ordre des pages : une autre page peut porter un
     * QR etranger (ex. document joint). Si aucun ne l'est, l'erreur du premier QR est renvoyee.
     */
    private ContenuQrModel lireQrCode(byte[] pdf) {
        List<String> textes = detecteurQrPdf.detecter(pdf);
        if (textes.isEmpty()) {
            throw new QrCodeIntrouvableException("Aucun code QR n'a ete trouve dans le PDF importe : "
                    + "verifiez que la fiche a ete scannee en entier et que le code QR est net");
        }
        RegleGestionException premiereErreur = null;
        for (String texte : textes) {
            try {
                return lecteurContenuQr.lire(texte);
            } catch (RegleGestionException e) {
                premiereErreur = premiereErreur == null ? e : premiereErreur;
            }
        }
        throw premiereErreur;
    }

    /**
     * Le type de contenu declare par le client ne suffit pas (il est librement falsifiable) :
     * on exige en plus que le fichier commence par la signature d'un PDF.
     */
    private boolean estPdf(FicheSigneeModel model) {
        byte[] contenu = model.contenu();
        return TYPE_PDF.equalsIgnoreCase(model.typeContenu())
                && contenu != null
                && contenu.length >= SIGNATURE_PDF.length
                && Arrays.equals(contenu, 0, SIGNATURE_PDF.length, SIGNATURE_PDF, 0, SIGNATURE_PDF.length);
    }
}
