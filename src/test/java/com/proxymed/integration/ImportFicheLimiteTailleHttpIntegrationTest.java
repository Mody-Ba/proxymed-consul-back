package com.proxymed.integration;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.enums.TypeVisite;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.support.FabriqueBadge;
import com.proxymed.support.PdfDeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Limites de l'import en lot sur un vrai serveur Tomcat (MockMvc ne passe ni par les limites
 * spring.servlet.multipart.* ni par celles de Tomcat), avec un vrai client HTTP :
 * <ul>
 *     <li>un fichier de plus de 10 Mo dans un envoi valide : 413 pour ce fichier seul, les autres
 *     sont importes (limite verifiee par le service) ;</li>
 *     <li>un envoi de plus de 100 Mo : refuse d'un bloc par le serveur, en 413 JSON lisible ;</li>
 *     <li>21 fichiers : le 400 du service arrive bien (Tomcat accepte assez de parties).</li>
 * </ul>
 * src/test/resources/application.properties masque celui de src/main : les limites sont donc
 * relues dans le fichier de production. Sans max-swallow-size suffisant, Tomcat fermerait la
 * connexion avant que le client ait fini d'envoyer, et le client perdrait la reponse 413.
 * Pas de transaction de test (vrai serveur) : les donnees creees sont supprimees apres chaque test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FabriqueBadge.class)
class ImportFicheLimiteTailleHttpIntegrationTest {

    private static final int UN_MO = 1024 * 1024;
    private static final Path CONFIG_PRODUCTION = Path.of("src", "main", "resources", "application.properties");
    private static final List<String> CLES_LIMITES = List.of(
            "spring.servlet.multipart.max-file-size",
            "spring.servlet.multipart.max-request-size",
            "server.tomcat.max-swallow-size");

    @DynamicPropertySource
    static void limitesDeProduction(DynamicPropertyRegistry registre) throws IOException {
        Properties production = new Properties();
        try (Reader lecteur = Files.newBufferedReader(CONFIG_PRODUCTION, StandardCharsets.UTF_8)) {
            production.load(lecteur);
        }
        for (String cle : CLES_LIMITES) {
            String valeur = production.getProperty(cle);
            if (valeur == null) {
                throw new IllegalStateException(cle + " absent de " + CONFIG_PRODUCTION);
            }
            registre.add(cle, () -> valeur);
        }
    }

    @LocalServerPort
    private int port;
    @Autowired
    private FabriqueBadge fabriqueBadge;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MultipartProperties multipartProperties;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ConsultationRepository consultationRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID patientCree;

    @AfterEach
    void nettoyer() {
        if (patientCree != null) {
            jdbc.update("DELETE FROM fiche_signee WHERE consultation_id IN "
                    + "(SELECT id FROM consultation_initiale WHERE patient_id = ?)", patientCree);
            jdbc.update("DELETE FROM consultation_initiale WHERE patient_id = ?", patientCree);
            jdbc.update("DELETE FROM patient WHERE id = ?", patientCree);
        }
    }

    @Test
    void leServeurTourneAvecLesLimitesDeProduction() {
        assertThat(multipartProperties.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(100));
        assertThat(multipartProperties.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(100));
    }

    @Test
    void unFichierDePlusDe10Mo_413PourCeFichierSeul_lAutreEstImporte() throws Exception {
        UUID visite = creerVisite();
        byte[] gros = pdfDeTaille(10 * UN_MO + 100 * 1024);
        byte[] fiche = PdfDeTest.pdfAvecQr("{\"type\":\"FICHE_1_CONSULTATION_INITIALE\",\"patientId\":\""
                + patientCree + "\",\"consultationId\":\"" + visite + "\"}");

        HttpResponse<String> reponse = importer(List.of(gros, fiche));

        assertThat(reponse.statusCode()).isEqualTo(200);
        JsonNode corps = objectMapper.readTree(reponse.body());
        JsonNode trop = corps.get("resultats").get(0);
        assertThat(trop.get("code").asInt()).isEqualTo(413);
        assertThat(trop.get("erreur").asString()).isEqualTo("Fichier trop volumineux");
        assertThat(trop.get("message").asString()).isEqualTo("Le fichier importe depasse la taille maximale de 10 Mo");
        JsonNode importe = corps.get("resultats").get(1);
        assertThat(importe.get("code").asInt()).isEqualTo(201);
        assertThat(importe.get("consultationId").asString()).isEqualTo(visite.toString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM fiche_signee WHERE consultation_id = ?", Long.class, visite))
                .isEqualTo(1L);
    }

    @Test
    void envoiDePlusDe100Mo_refuseDUnBloc_en413AuFormatJson() throws Exception {
        HttpResponse<String> reponse = importer(List.of(pdfDeTaille(100 * UN_MO + 100 * 1024)));

        assertThat(reponse.statusCode()).isEqualTo(413);
        assertThat(reponse.headers().firstValue(HttpHeaders.CONTENT_TYPE)).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/json"));
        JsonNode corps = objectMapper.readTree(reponse.body());
        assertThat(corps.get("status").asInt()).isEqualTo(413);
        assertThat(corps.get("erreur").asString()).isEqualTo("Fichier trop volumineux");
        assertThat(corps.get("message").asString())
                .isEqualTo("L'envoi depasse la taille totale maximale de 100 Mo : importez les fichiers en plusieurs fois");
        assertThat(corps.get("horodatage").isNull()).isFalse();
        assertThat(corps.get("details").isArray()).isTrue();
    }

    @Test
    void vingtEtUnFichiers_400AuFormatJson() throws Exception {
        List<byte[]> fichiers = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            fichiers.add(PdfDeTest.pdfAvecQr((String) null));
        }

        HttpResponse<String> reponse = importer(fichiers);

        assertThat(reponse.statusCode()).isEqualTo(400);
        JsonNode corps = objectMapper.readTree(reponse.body());
        assertThat(corps.get("message").asString())
                .isEqualTo("Au plus 20 fichiers par import (21 recus) : rien n'a ete importe");
    }

    private HttpResponse<String> importer(List<byte[]> fichiers) throws Exception {
        String frontiere = "----proxymed-" + UUID.randomUUID();
        ByteArrayOutputStream corps = new ByteArrayOutputStream();
        for (int i = 0; i < fichiers.size(); i++) {
            corps.write(("--" + frontiere + "\r\n"
                    + "Content-Disposition: form-data; name=\"fichiers\"; filename=\"scan-" + i + ".pdf\"\r\n"
                    + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            corps.write(fichiers.get(i));
            corps.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        }
        corps.write(("--" + frontiere + "--\r\n").getBytes(StandardCharsets.US_ASCII));

        // HTTP/1.1 explicite : pas de tentative de passage en h2c, une variable de moins.
        HttpRequest requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/fiches-signees/import"))
                .header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("ADMIN"))
                .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + frontiere)
                .version(HttpClient.Version.HTTP_1_1)
                .POST(HttpRequest.BodyPublishers.ofByteArray(corps.toByteArray()))
                .build();
        return HttpClient.newHttpClient().send(requete, HttpResponse.BodyHandlers.ofString());
    }

    private static byte[] pdfDeTaille(int taille) {
        byte[] contenu = new byte[taille];
        System.arraycopy("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII), 0, contenu, 0, 9);
        return contenu;
    }

    private UUID creerVisite() {
        patientCree = patientRepository.save(Patient.builder()
                .numeroDossierProxymed("DOS-HTTP-" + System.nanoTime()).nomComplet("Patient Limite")
                .dateNaissance(LocalDate.of(1950, 1, 1)).sexe(Sexe.M).couvertureSociale(CouvertureSociale.CMU)
                .build()).getId();
        return consultationRepository.save(ConsultationInitiale.builder()
                .patient(patientRepository.getReferenceById(patientCree))
                .statut(StatutConsultation.BROUILLON)
                .typeVisite(TypeVisite.INITIALE)
                .build()).getId();
    }
}
