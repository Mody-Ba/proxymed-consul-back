package com.proxymed.integration;

import com.proxymed.support.FabriqueBadge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Limite de 10 Mo appliquee par le serveur (spring.servlet.multipart.*), avant le controller :
 * MockMvc ne la declenche pas, d'ou un vrai serveur et un vrai client HTTP.
 * <p>
 * src/test/resources/application.properties masque celui de src/main : sans rien faire, ce test
 * tournerait avec les valeurs par defaut de Spring Boot (fichier 1 Mo, max-swallow-size 2 Mo).
 * Tomcat fermerait alors la connexion apres 2 Mo avales alors que le client envoie encore ~8 Mo :
 * selon le timing, le client perdait la reponse 413 (pourtant complete). Les limites sont donc
 * relues dans le fichier de production : le serveur avale tout le corps avant de fermer, et le
 * client lit toujours la reponse entiere.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FabriqueBadge.class)
class ImportFicheLimiteTailleHttpIntegrationTest {

    private static final int DIX_MO = 10 * 1024 * 1024;
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

    @Test
    void import_fichierDePlusDe10Mo_renvoie413AuFormatJson() throws Exception {
        // Le serveur tourne bien avec les limites de production (10 Mo) : c'est elles qui refusent l'envoi.
        assertThat(multipartProperties.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(10));
        assertThat(multipartProperties.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(10));

        byte[] gros = new byte[DIX_MO + 100 * 1024];
        System.arraycopy("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII), 0, gros, 0, 9);

        HttpResponse<String> reponse = importer(gros);

        assertThat(reponse.statusCode()).isEqualTo(413);
        assertThat(reponse.headers().firstValue(HttpHeaders.CONTENT_TYPE)).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/json"));
        JsonNode corps = objectMapper.readTree(reponse.body());
        assertThat(corps.get("status").asInt()).isEqualTo(413);
        assertThat(corps.get("erreur").asString()).isEqualTo("Fichier trop volumineux");
        assertThat(corps.get("message").asString())
                .isEqualTo("Le fichier importe depasse la taille maximale de 10 Mo");
        assertThat(corps.get("horodatage").isNull()).isFalse();
        assertThat(corps.get("details").isArray()).isTrue();
    }

    private HttpResponse<String> importer(byte[] contenu) throws Exception {
        String frontiere = "----proxymed-" + UUID.randomUUID();
        ByteArrayOutputStream corps = new ByteArrayOutputStream();
        corps.write(("--" + frontiere + "\r\n"
                + "Content-Disposition: form-data; name=\"fichier\"; filename=\"gros.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        corps.write(contenu);
        corps.write(("\r\n--" + frontiere + "--\r\n").getBytes(StandardCharsets.US_ASCII));

        // HTTP/1.1 explicite : pas de tentative de passage en h2c, une variable de moins.
        HttpRequest requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/fiches-signees/import"))
                .header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("MEDECIN_SENIOR"))
                .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + frontiere)
                .version(HttpClient.Version.HTTP_1_1)
                .POST(HttpRequest.BodyPublishers.ofByteArray(corps.toByteArray()))
                .build();
        return HttpClient.newHttpClient().send(requete, HttpResponse.BodyHandlers.ofString());
    }
}
