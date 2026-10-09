package com.proxymed.service;

import com.proxymed.entity.FicheSignee;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.service.mappers.FicheSigneeMapper;
import com.proxymed.service.model.FicheSigneeModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * La seule entite JPA visible dans cette classe est FicheSignee (l'aggregat propre a ce
 * service). ConsultationInitiale (aggregat parent) n'est jamais manipulee ici : la
 * verification qu'elle existe passe par ConsultationService.findById, et le rattachement
 * JPA (id -> reference) est delegue au mapper DB.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FicheSigneeServiceImpl implements FicheSigneeService {

    private static final String TYPE_PDF = "application/pdf";
    private static final byte[] SIGNATURE_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final FicheSigneeRepository ficheSigneeRepository;
    private final ConsultationService consultationService;
    private final FicheSigneeMapper ficheSigneeMapper;

    @Override
    public FicheSigneeModel importer(UUID consultationId, FicheSigneeModel model) {
        consultationService.findById(consultationId);
        if (!estPdf(model)) {
            throw new RegleGestionException("Le fichier importe doit etre un PDF");
        }
        FicheSigneeModel aEnregistrer = model.toBuilder().dateImport(Instant.now()).build();
        FicheSignee entity = ficheSigneeRepository.save(ficheSigneeMapper.toEntity(aEnregistrer, consultationId));
        return ficheSigneeMapper.toModel(entity);
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
