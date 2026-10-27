package com.proxymed.service;

import com.proxymed.exception.RegleGestionException;
import com.proxymed.service.model.ContenuQrModel;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

/**
 * Lit et valide le texte d'un QR code detecte sur une fiche scannee (format produit par
 * {@link GenerateurContenuQr}). Seul le type FICHE_1_CONSULTATION_INITIALE est accepte par ce service.
 */
@Component
public class LecteurContenuQr {

    static final String CHAMP_TYPE = "type";
    static final String CHAMP_PATIENT = "patientId";
    static final String CHAMP_CONSULTATION = "consultationId";

    static final String MESSAGE_PAS_PROXYMED = "Le code QR detecte n'est pas un code QR PROXYMED";
    static final String MESSAGE_ANCIEN_FORMAT = "Le code QR detecte est celui d'une ancienne fiche (il ne contient "
            + "que l'id de la consultation) : regenerez le PDF de la fiche, faites-la signer puis importez-la a nouveau";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * @throws RegleGestionException si le texte est un QR d'ancienne fiche (id de consultation
     *                               seul), n'est pas un QR PROXYMED (JSON illisible, champ manquant,
     *                               id qui n'est pas un UUID) ou si le type de fiche n'est pas
     *                               FICHE_1_CONSULTATION_INITIALE
     */
    public ContenuQrModel lire(String texte) {
        if (uuidCanonique(texte) != null) {
            // Pas de compatibilite : on explique seulement pourquoi la fiche est refusee.
            throw new RegleGestionException(MESSAGE_ANCIEN_FORMAT);
        }
        JsonNode racine = lireJson(texte)
                .filter(JsonNode::isObject)
                .orElseThrow(() -> new RegleGestionException(MESSAGE_PAS_PROXYMED));
        String type = texte(racine, CHAMP_TYPE);
        UUID patientId = uuid(racine, CHAMP_PATIENT);
        UUID consultationId = uuid(racine, CHAMP_CONSULTATION);
        if (type == null || patientId == null || consultationId == null) {
            throw new RegleGestionException(MESSAGE_PAS_PROXYMED);
        }
        if (!GenerateurContenuQr.TYPE_FICHE_1_CONSULTATION_INITIALE.equals(type)) {
            throw new RegleGestionException("Type de fiche non pris en charge : " + type + " (seule la fiche "
                    + GenerateurContenuQr.TYPE_FICHE_1_CONSULTATION_INITIALE + " peut etre importee ici)");
        }
        return ContenuQrModel.builder().type(type).patientId(patientId).consultationId(consultationId).build();
    }

    private Optional<JsonNode> lireJson(String texte) {
        if (texte == null || texte.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JSON.readTree(texte));
        } catch (JacksonException e) {
            return Optional.empty();
        }
    }

    private String texte(JsonNode racine, String champ) {
        JsonNode valeur = racine.get(champ);
        return valeur != null && valeur.isString() && !valeur.asString().isBlank() ? valeur.asString() : null;
    }

    private UUID uuid(JsonNode racine, String champ) {
        return uuidCanonique(texte(racine, champ));
    }

    /**
     * UUID.fromString tolere des formes abregees ("1-2-3-4-5") : on exige la forme canonique.
     */
    private UUID uuidCanonique(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(valeur.strip());
            return uuid.toString().equalsIgnoreCase(valeur.strip()) ? uuid : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
