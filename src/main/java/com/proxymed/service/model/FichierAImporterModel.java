package com.proxymed.service.model;

import lombok.Builder;

import java.util.function.Supplier;

/**
 * Couche interne neutre : un fichier recu dans un import en lot, pas encore lu. Le contenu n'est
 * lu (contenu.get()) qu'au moment de traiter ce fichier, et seulement s'il ne depasse pas la taille
 * maximale : la requete n'est jamais chargee en memoire d'un seul coup.
 */
@Builder(toBuilder = true)
public record FichierAImporterModel(
        String nomFichier,
        String typeContenu,
        long tailleOctets,
        Supplier<byte[]> contenu
) {
}
