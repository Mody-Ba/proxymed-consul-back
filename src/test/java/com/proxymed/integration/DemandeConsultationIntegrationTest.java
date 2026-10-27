package com.proxymed.integration;

import com.proxymed.support.FabriqueBadge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Demandes de consultation initiale : droits par role, lien demandeur / junior assigne lu dans le
 * badge, transitions EN_ATTENTE -> ACCEPTEE / REFUSEE, validation et pagination.
 * Pas de BadgeMedecinParDefaut ici : chaque requete pose son propre badge, pour un compte precis.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
@Transactional
class DemandeConsultationIntegrationTest {

    private static final String URL = "/api/demandes-consultation";
    private static final String MESSAGE_ROLE = "Votre role ne permet pas d'acceder a cette ressource";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FabriqueBadge fabriqueBadge;

    private final UUID utilisateurA = UUID.randomUUID();
    private final UUID utilisateurB = UUID.randomUUID();
    private final UUID senior = UUID.randomUUID();
    private final UUID junior = UUID.randomUUID();
    private final UUID autreJunior = UUID.randomUUID();

    // ---------- POST : creation ----------

    @Test
    void creer_201_demandeurLuDansLeBadge_statutEnAttente() throws Exception {
        mockMvc.perform(en("UTILISATEUR", utilisateurA, json(post(URL), demande("Awa Diop", true, "Toux persistante"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.demandeurId", is(utilisateurA.toString())))
                .andExpect(jsonPath("$.nomPatient", is("Awa Diop")))
                .andExpect(jsonPath("$.pourProche", is(true)))
                .andExpect(jsonPath("$.motif", is("Toux persistante")))
                .andExpect(jsonPath("$.statut", is("EN_ATTENTE")))
                .andExpect(jsonPath("$.medecinSeniorId", nullValue()))
                .andExpect(jsonPath("$.juniorId", nullValue()))
                .andExpect(jsonPath("$.motifRefus", nullValue()))
                .andExpect(jsonPath("$.dateCreation", notNullValue()))
                .andExpect(jsonPath("$.dateDecision", nullValue()));
    }

    /** Un demandeurId ou un statut glisse dans le corps est rejete, jamais pris en compte. */
    @Test
    void creer_400_siIdentiteOuStatutDansLeCorps() throws Exception {
        for (String champ : List.of("demandeurId", "statut", "juniorId", "medecinSeniorId")) {
            Map<String, Object> corps = new HashMap<>(demande("Awa Diop", false, "Fievre"));
            corps.put(champ, champ.equals("statut") ? "ACCEPTEE" : utilisateurB.toString());
            verifierErreur(mockMvc.perform(en("UTILISATEUR", utilisateurA, json(post(URL), corps))),
                    400, "Requete invalide", "Champ inconnu dans la requete : " + champ);
        }
        mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void creer_400_siChampsObligatoiresManquants() throws Exception {
        verifierErreur(mockMvc.perform(en("UTILISATEUR", utilisateurA, json(post(URL), Map.of()))),
                400, "Validation echouee", "Un ou plusieurs champs sont invalides")
                .andExpect(jsonPath("$.details", hasSize(3)));

        verifierErreur(mockMvc.perform(en("UTILISATEUR", utilisateurA,
                        json(post(URL), demande("   ", false, "")))),
                400, "Validation echouee", "Un ou plusieurs champs sont invalides")
                .andExpect(jsonPath("$.details", hasSize(2)));
    }

    @Test
    void creer_400_siTropLong() throws Exception {
        verifierErreur(mockMvc.perform(en("UTILISATEUR", utilisateurA,
                        json(post(URL), demande("N".repeat(201), false, "M".repeat(2001))))),
                400, "Validation echouee", "Un ou plusieurs champs sont invalides")
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "nomPatient : le nom du patient ne doit pas depasser 200 caracteres",
                        "motif : le motif ne doit pas depasser 2000 caracteres")));

        mockMvc.perform(en("UTILISATEUR", utilisateurA,
                        json(post(URL), demande("N".repeat(200), false, "M".repeat(2000)))))
                .andExpect(status().isCreated());
    }

    @Test
    void creer_400_siJsonInvalide() throws Exception {
        verifierErreur(mockMvc.perform(en("UTILISATEUR", utilisateurA,
                        post(URL).contentType("application/json").content("{\"nomPatient\": "))),
                400, "Requete invalide", "Le corps de la requete est illisible ou mal forme");

        verifierErreur(mockMvc.perform(en("UTILISATEUR", utilisateurA,
                        json(post(URL), Map.of("nomPatient", "Awa", "pourProche", "peut-etre", "motif", "Fievre")))),
                400, "Requete invalide", "Le corps de la requete est illisible ou mal forme");
    }

    @Test
    void creer_403_siPasUtilisateur() throws Exception {
        for (String role : List.of("MEDECIN_SENIOR", "MEDECIN_JUNIOR", "ADMIN")) {
            verifierErreur(mockMvc.perform(en(role, UUID.randomUUID(),
                            json(post(URL), demande("Awa Diop", false, "Fievre")))),
                    403, "Acces refuse", MESSAGE_ROLE);
        }
    }

    @Test
    void creer_401_sansBadge() throws Exception {
        verifierErreur(mockMvc.perform(json(post(URL), demande("Awa Diop", false, "Fievre"))),
                401, "Non authentifie", "Badge d'authentification manquant");
    }

    // ---------- GET /mes-demandes ----------

    @Test
    void mesDemandes_neRenvoieQueLesSiennes_paginees() throws Exception {
        creerDemande(utilisateurA, "Patient A1");
        creerDemande(utilisateurA, "Patient A2");
        creerDemande(utilisateurA, "Patient A3");
        creerDemande(utilisateurB, "Patient B1");

        mockMvc.perform(en("UTILISATEUR", utilisateurA, get(URL + "/mes-demandes").param("taille", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements", hasSize(2)))
                .andExpect(jsonPath("$.elements[*].demandeurId", containsInAnyOrder(
                        utilisateurA.toString(), utilisateurA.toString())))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.taille", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)));

        mockMvc.perform(en("UTILISATEUR", utilisateurA,
                        get(URL + "/mes-demandes").param("page", "1").param("taille", "2")))
                .andExpect(jsonPath("$.elements", hasSize(1)))
                .andExpect(jsonPath("$.elements[0].demandeurId", is(utilisateurA.toString())));

        mockMvc.perform(en("UTILISATEUR", utilisateurB, get(URL + "/mes-demandes")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.elements[0].nomPatient", is("Patient B1")));
    }

    @Test
    void mesDemandes_403_siPasUtilisateur() throws Exception {
        for (String role : List.of("MEDECIN_SENIOR", "MEDECIN_JUNIOR", "ADMIN")) {
            verifierErreur(mockMvc.perform(en(role, UUID.randomUUID(), get(URL + "/mes-demandes"))),
                    403, "Acces refuse", MESSAGE_ROLE);
        }
    }

    // ---------- GET : liste senior ----------

    @Test
    void findAll_senior_voitTout_etFiltreParStatut() throws Exception {
        UUID d1 = creerDemande(utilisateurA, "P1");
        UUID d2 = creerDemande(utilisateurB, "P2");
        creerDemande(utilisateurB, "P3");
        accepter(d1, junior).andExpect(status().isOk());
        refuser(d2, "Hors zone").andExpect(status().isOk());

        mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)));

        for (String statut : List.of("EN_ATTENTE", "ACCEPTEE", "REFUSEE")) {
            mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL).param("statut", statut)))
                    .andExpect(jsonPath("$.totalElements", is(1)))
                    .andExpect(jsonPath("$.elements[0].statut", is(statut)));
        }
    }

    @Test
    void findAll_400_siStatutOuPaginationInvalide() throws Exception {
        verifierErreur(mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL).param("statut", "INCONNU"))),
                400, "Requete invalide", "Valeur invalide pour le parametre \"statut\"");
        verifierErreur(mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL).param("page", "abc"))),
                400, "Requete invalide", "Valeur invalide pour le parametre \"page\"");
        verifierErreur(mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL).param("page", "-1"))),
                400, "Regle de gestion non respectee", "Le numero de page doit etre positif ou nul");
        for (String taille : List.of("0", "101")) {
            verifierErreur(mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL).param("taille", taille))),
                    400, "Regle de gestion non respectee", "La taille de page doit etre comprise entre 1 et 100");
        }
    }

    @Test
    void findAll_403_siPasSenior() throws Exception {
        for (String role : List.of("UTILISATEUR", "MEDECIN_JUNIOR", "ADMIN")) {
            verifierErreur(mockMvc.perform(en(role, UUID.randomUUID(), get(URL))),
                    403, "Acces refuse", MESSAGE_ROLE);
        }
    }

    // ---------- GET /{id} ----------

    @Test
    void findById_autoriseSeniorCreateurEtJuniorAssigne() throws Exception {
        UUID id = creerDemande(utilisateurA, "Awa Diop");
        accepter(id, junior).andExpect(status().isOk());

        for (var acces : List.of(Map.entry("MEDECIN_SENIOR", UUID.randomUUID()),
                Map.entry("UTILISATEUR", utilisateurA),
                Map.entry("MEDECIN_JUNIOR", junior))) {
            mockMvc.perform(en(acces.getKey(), acces.getValue(), get(URL + "/" + id)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(id.toString())))
                    .andExpect(jsonPath("$.nomPatient", is("Awa Diop")));
        }
    }

    @Test
    void findById_utilisateurNeVoitJamaisLaDemandeDunAutre() throws Exception {
        UUID id = creerDemande(utilisateurA, "Awa Diop");

        verifierErreur(mockMvc.perform(en("UTILISATEUR", utilisateurB, get(URL + "/" + id))),
                403, "Acces refuse", MESSAGE_ROLE)
                .andExpect(content().string(not(containsString("Awa Diop"))));
    }

    @Test
    void findById_juniorNonAssigne_403() throws Exception {
        UUID enAttente = creerDemande(utilisateurA, "P1");
        UUID acceptee = creerDemande(utilisateurA, "P2");
        accepter(acceptee, junior).andExpect(status().isOk());

        verifierErreur(mockMvc.perform(en("MEDECIN_JUNIOR", autreJunior, get(URL + "/" + acceptee))),
                403, "Acces refuse", MESSAGE_ROLE);
        verifierErreur(mockMvc.perform(en("MEDECIN_JUNIOR", junior, get(URL + "/" + enAttente))),
                403, "Acces refuse", MESSAGE_ROLE);
    }

    @Test
    void findById_admin_403() throws Exception {
        UUID id = creerDemande(utilisateurA, "P1");
        verifierErreur(mockMvc.perform(en("ADMIN", UUID.randomUUID(), get(URL + "/" + id))),
                403, "Acces refuse", MESSAGE_ROLE);
    }

    @Test
    void findById_404_siAbsente() throws Exception {
        UUID inconnue = UUID.randomUUID();
        for (String role : List.of("MEDECIN_SENIOR", "MEDECIN_JUNIOR", "UTILISATEUR")) {
            verifierErreur(mockMvc.perform(en(role, UUID.randomUUID(), get(URL + "/" + inconnue))),
                    404, "Ressource introuvable", "Demande de consultation introuvable : " + inconnue);
        }
    }

    @Test
    void findById_400_siIdPasUnUuid() throws Exception {
        verifierErreur(mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL + "/pas-un-uuid"))),
                400, "Requete invalide", "Valeur invalide pour le parametre \"id\"");
    }

    // ---------- GET /assignees-a-moi ----------

    @Test
    void assigneesAMoi_neRenvoieQueLesAccepteesAssigneesAuJunior() throws Exception {
        UUID pourMoi1 = creerDemande(utilisateurA, "P1");
        UUID pourMoi2 = creerDemande(utilisateurB, "P2");
        UUID pourAutre = creerDemande(utilisateurA, "P3");
        UUID refusee = creerDemande(utilisateurA, "P4");
        creerDemande(utilisateurA, "P5");
        accepter(pourMoi1, junior).andExpect(status().isOk());
        accepter(pourMoi2, junior).andExpect(status().isOk());
        accepter(pourAutre, autreJunior).andExpect(status().isOk());
        refuser(refusee, "Doublon").andExpect(status().isOk());

        mockMvc.perform(en("MEDECIN_JUNIOR", junior, get(URL + "/assignees-a-moi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.elements[*].id", containsInAnyOrder(pourMoi1.toString(), pourMoi2.toString())))
                .andExpect(jsonPath("$.elements[*].statut", containsInAnyOrder("ACCEPTEE", "ACCEPTEE")));

        mockMvc.perform(en("MEDECIN_JUNIOR", junior, get(URL + "/assignees-a-moi").param("taille", "1")))
                .andExpect(jsonPath("$.elements", hasSize(1)))
                .andExpect(jsonPath("$.totalPages", is(2)));

        mockMvc.perform(en("MEDECIN_JUNIOR", UUID.randomUUID(), get(URL + "/assignees-a-moi")))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void assigneesAMoi_403_siPasJunior() throws Exception {
        for (String role : List.of("UTILISATEUR", "MEDECIN_SENIOR", "ADMIN")) {
            verifierErreur(mockMvc.perform(en(role, UUID.randomUUID(), get(URL + "/assignees-a-moi"))),
                    403, "Acces refuse", MESSAGE_ROLE);
        }
    }

    // ---------- PATCH /{id}/accepter ----------

    @Test
    void accepter_enregistreSeniorDuBadgeJuniorEtDate() throws Exception {
        UUID id = creerDemande(utilisateurA, "P1");

        accepter(id, junior)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("ACCEPTEE")))
                .andExpect(jsonPath("$.medecinSeniorId", is(senior.toString())))
                .andExpect(jsonPath("$.juniorId", is(junior.toString())))
                .andExpect(jsonPath("$.dateDecision", notNullValue()))
                .andExpect(jsonPath("$.motifRefus", nullValue()));

        mockMvc.perform(en("UTILISATEUR", utilisateurA, get(URL + "/" + id)))
                .andExpect(jsonPath("$.statut", is("ACCEPTEE")));
    }

    @Test
    void accepter_409_siDejaAccepteeOuRefusee() throws Exception {
        UUID acceptee = creerDemande(utilisateurA, "P1");
        accepter(acceptee, junior).andExpect(status().isOk());
        verifierErreur(accepter(acceptee, autreJunior), 409, "Demande deja traitee",
                "La demande de consultation " + acceptee + " a deja ete traitee : seule une demande EN_ATTENTE "
                        + "peut etre acceptee ou refusee");
        mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL + "/" + acceptee)))
                .andExpect(jsonPath("$.juniorId", is(junior.toString())));

        UUID refusee = creerDemande(utilisateurA, "P2");
        refuser(refusee, "Hors zone").andExpect(status().isOk());
        verifierErreur(accepter(refusee, junior), 409, "Demande deja traitee",
                "La demande de consultation " + refusee + " a deja ete traitee : seule une demande EN_ATTENTE "
                        + "peut etre acceptee ou refusee");
    }

    @Test
    void accepter_400_siJuniorAbsentInvalideOuChampInconnu() throws Exception {
        UUID id = creerDemande(utilisateurA, "P1");

        verifierErreur(mockMvc.perform(en("MEDECIN_SENIOR", senior, json(patch(URL + "/" + id + "/accepter"), Map.of()))),
                400, "Validation echouee", "Un ou plusieurs champs sont invalides")
                .andExpect(jsonPath("$.details[0]", is("juniorId : l'identifiant du medecin junior est obligatoire")));

        verifierErreur(mockMvc.perform(en("MEDECIN_SENIOR", senior,
                        json(patch(URL + "/" + id + "/accepter"), Map.of("juniorId", "pas-un-uuid")))),
                400, "Requete invalide", "Le corps de la requete est illisible ou mal forme");

        verifierErreur(mockMvc.perform(en("MEDECIN_SENIOR", senior, json(patch(URL + "/" + id + "/accepter"),
                        Map.of("juniorId", junior.toString(), "medecinSeniorId", UUID.randomUUID().toString())))),
                400, "Requete invalide", "Champ inconnu dans la requete : medecinSeniorId");

        mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL + "/" + id)))
                .andExpect(jsonPath("$.statut", is("EN_ATTENTE")));
    }

    @Test
    void accepter_404_siAbsente() throws Exception {
        UUID inconnue = UUID.randomUUID();
        verifierErreur(accepter(inconnue, junior), 404, "Ressource introuvable",
                "Demande de consultation introuvable : " + inconnue);
    }

    @Test
    void accepter_403_siPasSenior() throws Exception {
        UUID id = creerDemande(utilisateurA, "P1");
        for (var appelant : List.of(Map.entry("UTILISATEUR", utilisateurA),
                Map.entry("MEDECIN_JUNIOR", junior), Map.entry("ADMIN", UUID.randomUUID()))) {
            verifierErreur(mockMvc.perform(en(appelant.getKey(), appelant.getValue(),
                            json(patch(URL + "/" + id + "/accepter"), Map.of("juniorId", junior.toString())))),
                    403, "Acces refuse", MESSAGE_ROLE);
        }
        mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL + "/" + id)))
                .andExpect(jsonPath("$.statut", is("EN_ATTENTE")));
    }

    // ---------- PATCH /{id}/refuser ----------

    @Test
    void refuser_enregistreMotifSeniorEtDate() throws Exception {
        UUID id = creerDemande(utilisateurA, "P1");

        refuser(id, "Hors zone d'intervention")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("REFUSEE")))
                .andExpect(jsonPath("$.motifRefus", is("Hors zone d'intervention")))
                .andExpect(jsonPath("$.medecinSeniorId", is(senior.toString())))
                .andExpect(jsonPath("$.juniorId", nullValue()))
                .andExpect(jsonPath("$.dateDecision", notNullValue()));

        mockMvc.perform(en("UTILISATEUR", utilisateurA, get(URL + "/" + id)))
                .andExpect(jsonPath("$.statut", is("REFUSEE")))
                .andExpect(jsonPath("$.motifRefus", is("Hors zone d'intervention")));
    }

    @Test
    void refuser_409_siDejaRefuseeOuAcceptee() throws Exception {
        UUID refusee = creerDemande(utilisateurA, "P1");
        refuser(refusee, "Hors zone").andExpect(status().isOk());
        verifierErreur(refuser(refusee, "Autre motif"), 409, "Demande deja traitee",
                "La demande de consultation " + refusee + " a deja ete traitee : seule une demande EN_ATTENTE "
                        + "peut etre acceptee ou refusee");
        mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL + "/" + refusee)))
                .andExpect(jsonPath("$.motifRefus", is("Hors zone")));

        UUID acceptee = creerDemande(utilisateurA, "P2");
        accepter(acceptee, junior).andExpect(status().isOk());
        refuser(acceptee, "Trop tard").andExpect(status().isConflict());
    }

    @Test
    void refuser_400_siMotifAbsentVideOuTropLong() throws Exception {
        UUID id = creerDemande(utilisateurA, "P1");

        for (Map<String, Object> corps : List.<Map<String, Object>>of(Map.of(), Map.of("motifRefus", "   "))) {
            verifierErreur(mockMvc.perform(en("MEDECIN_SENIOR", senior, json(patch(URL + "/" + id + "/refuser"), corps))),
                    400, "Validation echouee", "Un ou plusieurs champs sont invalides")
                    .andExpect(jsonPath("$.details[0]", is("motifRefus : le motif de refus est obligatoire")));
        }
        verifierErreur(refuser(id, "R".repeat(1001)), 400, "Validation echouee", "Un ou plusieurs champs sont invalides")
                .andExpect(jsonPath("$.details[0]",
                        is("motifRefus : le motif de refus ne doit pas depasser 1000 caracteres")));

        mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL + "/" + id)))
                .andExpect(jsonPath("$.statut", is("EN_ATTENTE")));
    }

    @Test
    void refuser_404_siAbsente() throws Exception {
        refuser(UUID.randomUUID(), "Hors zone").andExpect(status().isNotFound());
    }

    @Test
    void refuser_403_siPasSenior() throws Exception {
        UUID id = creerDemande(utilisateurA, "P1");
        for (var appelant : List.of(Map.entry("UTILISATEUR", utilisateurA),
                Map.entry("MEDECIN_JUNIOR", junior), Map.entry("ADMIN", UUID.randomUUID()))) {
            verifierErreur(mockMvc.perform(en(appelant.getKey(), appelant.getValue(),
                            json(patch(URL + "/" + id + "/refuser"), Map.of("motifRefus", "Hors zone")))),
                    403, "Acces refuse", MESSAGE_ROLE);
        }
    }

    // ---------- Roles sur l'ensemble de l'API ----------

    @Test
    void admin_refusePartout() throws Exception {
        UUID id = creerDemande(utilisateurA, "P1");
        UUID admin = UUID.randomUUID();
        List<MockHttpServletRequestBuilder> requetes = List.of(
                json(post(URL), demande("Awa", false, "Fievre")),
                get(URL + "/mes-demandes"),
                get(URL),
                get(URL + "/" + id),
                get(URL + "/assignees-a-moi"),
                json(patch(URL + "/" + id + "/accepter"), Map.of("juniorId", junior.toString())),
                json(patch(URL + "/" + id + "/refuser"), Map.of("motifRefus", "Hors zone")));
        for (MockHttpServletRequestBuilder requete : requetes) {
            verifierErreur(mockMvc.perform(en("ADMIN", admin, requete)), 403, "Acces refuse", MESSAGE_ROLE);
        }
    }

    /** Seuls les endpoints de demandes de consultation s'ouvrent au role UTILISATEUR. */
    @Test
    void utilisateur_refuseSurLesAnciensEndpoints() throws Exception {
        UUID uuid = UUID.randomUUID();
        List<String> adresses = List.of(
                "/api/medecins", "/api/medecins/1",
                "/api/patients", "/api/patients/" + uuid, "/api/patients/" + uuid + "/fiches-signees",
                "/api/consultations", "/api/consultations/en-attente-dass", "/api/consultations/" + uuid,
                "/api/consultations/" + uuid + "/antecedents-maladies",
                "/api/consultations/" + uuid + "/examen-par-appareil",
                "/api/referentiels/structures", "/api/referentiels/maladies-chroniques",
                "/api/referentiels/facteurs-risque", "/api/referentiels/situations-sociales");
        for (String adresse : adresses) {
            verifierErreur(mockMvc.perform(en("UTILISATEUR", utilisateurA, get(adresse))),
                    403, "Acces refuse", MESSAGE_ROLE);
        }
    }

    // ---------- Outils ----------

    private UUID creerDemande(UUID demandeur, String nomPatient) throws Exception {
        String reponse = mockMvc.perform(en("UTILISATEUR", demandeur,
                        json(post(URL), demande(nomPatient, false, "Motif de " + nomPatient))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(reponse).get("id").asString());
    }

    private ResultActions accepter(UUID id, UUID juniorId) throws Exception {
        return mockMvc.perform(en("MEDECIN_SENIOR", senior,
                json(patch(URL + "/" + id + "/accepter"), Map.of("juniorId", juniorId.toString()))));
    }

    private ResultActions refuser(UUID id, String motifRefus) throws Exception {
        return mockMvc.perform(en("MEDECIN_SENIOR", senior,
                json(patch(URL + "/" + id + "/refuser"), Map.of("motifRefus", motifRefus))));
    }

    private static Map<String, Object> demande(String nomPatient, boolean pourProche, String motif) {
        return Map.of("nomPatient", nomPatient, "pourProche", pourProche, "motif", motif);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, Object corps) {
        return requete.contentType("application/json").content(objectMapper.writeValueAsString(corps));
    }

    private MockHttpServletRequestBuilder en(String role, UUID compte, MockHttpServletRequestBuilder requete) {
        return requete.header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer(role, compte));
    }

    private static ResultActions verifierErreur(ResultActions resultat, int statut, String erreur, String message)
            throws Exception {
        return resultat.andExpect(status().is(statut))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status", is(statut)))
                .andExpect(jsonPath("$.erreur", is(erreur)))
                .andExpect(jsonPath("$.message", is(message)))
                .andExpect(jsonPath("$.horodatage", notNullValue()));
    }
}
