package com.proxymed.service;

import com.proxymed.service.model.FicheSigneeModel;

import java.util.List;
import java.util.UUID;

public interface FicheSigneeService {

    /**
     * Importe une fiche signee scannee (PDF) sans id : la consultation et le patient sont deduits
     * du QR code imprime sur la fiche, cherche sur toutes les pages. Une consultation n'a qu'une
     * seule fiche signee : un second import (meme fichier ou non) est refuse.
     *
     * @param juniorAControlerId compte du badge d'un medecin junior, qui doit etre le junior assigne
     *        a la consultation du QR ; null si l'appelant n'est pas soumis a ce controle (senior)
     * @throws com.proxymed.exception.FichierTropVolumineuxException si le fichier depasse 10 Mo
     * @throws com.proxymed.exception.FichierNonPdfException si le fichier n'est pas un PDF
     * @throws com.proxymed.exception.RegleGestionException si le PDF est illisible, si le QR n'est pas un QR PROXYMED (dont l'ancien format, id de
     *         consultation seul) ou pas une FICHE_1_CONSULTATION_INITIALE, ou si le patient du QR
     *         n'est pas celui de la consultation
     * @throws com.proxymed.exception.QrCodeIntrouvableException si aucun QR code n'est trouve
     * @throws com.proxymed.exception.ResourceNotFoundException si le patient ou la consultation
     *         du QR n'existe pas
     * @throws org.springframework.security.access.AccessDeniedException si la consultation du QR est
     *         assignee a un autre medecin junior
     * @throws com.proxymed.exception.FicheSigneeExistanteException si la consultation a deja une
     *         fiche signee
     */
    FicheSigneeModel importer(FicheSigneeModel model, UUID juniorAControlerId);

    /**
     * Renvoie la fiche signee de la consultation.
     *
     * @throws com.proxymed.exception.ResourceNotFoundException si la consultation n'existe pas
     *                                                          ou n'a aucune fiche importee
     */
    FicheSigneeModel telecharger(UUID consultationId);

    /**
     * Fiches signees du dossier du patient (metadonnees, sans contenu), de la plus recemment
     * importee a la plus ancienne.
     *
     * @throws com.proxymed.exception.ResourceNotFoundException si le patient n'existe pas
     */
    List<FicheSigneeModel> findByPatient(UUID patientId);
}
