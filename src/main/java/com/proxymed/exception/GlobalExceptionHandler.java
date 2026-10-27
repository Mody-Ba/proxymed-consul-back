package com.proxymed.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Ressource introuvable", ex.getMessage()));
    }

    /**
     * Refus d'un @PreAuthorize : l'exception est levee dans le controller, donc elle peut arriver
     * ici avant AccesRefuseHandler. Meme reponse que AccesRefuseHandler. Le type JSON est force :
     * sans cela, un refus sur un endpoint PDF (produces = application/pdf) ne pourrait pas
     * renvoyer ce message.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesRefuse(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Acces refuse",
                        "Votre role ne permet pas d'acceder a cette ressource"));
    }

    @ExceptionHandler(RegleGestionException.class)
    public ResponseEntity<ErrorResponse> handleRegleGestion(RegleGestionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Regle de gestion non respectee", ex.getMessage()));
    }

    @ExceptionHandler(ConflitEtatException.class)
    public ResponseEntity<ErrorResponse> handleConflitEtat(ConflitEtatException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Action incompatible avec le statut de la fiche", ex.getMessage()));
    }

    @ExceptionHandler(FicheSigneeExistanteException.class)
    public ResponseEntity<ErrorResponse> handleFicheSigneeExistante(FicheSigneeExistanteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Fiche signee deja importee", ex.getMessage()));
    }

    @ExceptionHandler(QrCodeIntrouvableException.class)
    public ResponseEntity<ErrorResponse> handleQrCodeIntrouvable(QrCodeIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ErrorResponse(HttpStatus.UNPROCESSABLE_CONTENT.value(), "Code QR introuvable", ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Requete invalide",
                        "La partie \"" + ex.getRequestPartName() + "\" (fichier PDF) est obligatoire"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation echouee", "Un ou plusieurs champs sont invalides", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation echouee", ex.getMessage()));
    }

    /**
     * Corps JSON illisible. Cas le plus courant : un champ inconnu du DTO (rejete grace a
     * spring.jackson.deserialization.fail-on-unknown-properties=true), par exemple un
     * "examenParAppareil" imbrique envoye a PUT /api/consultations/{id} au lieu de son
     * endpoint dedie. On nomme le champ fautif pour que le client sache quoi corriger.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = ex.getMostSpecificCause() instanceof UnrecognizedPropertyException upe
                ? "Champ inconnu dans la requete : " + upe.getPropertyName()
                : "Le corps de la requete est illisible ou mal forme";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Requete invalide", message));
    }
}
