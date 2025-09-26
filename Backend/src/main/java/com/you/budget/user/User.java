package com.you.budget.user;

import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modèle User - Représente un utilisateur de l'application.
 * Inclut des méthodes pour la conversion JSON et la validation.
 */
public class User {

    private String id;
    private String email;
    private String name;
    private String password; // Sera hashé
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public User(String email, String name, String password) {
        this(); // Appelle le constructeur par défaut pour initialiser id et dates
        this.email = email;
        this.name = name;
        this.password = password;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public User setId(String id) {
        this.id = id;
        return this;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
    }

    public User setName(String name) {
        this.name = name;
        return this;
    }

    public User setPassword(String password) {
        this.password = password;
        return this;
    }

    public User setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public User setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    /**
     * Met à jour la date de modification.
     */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Convertit l'objet User en JsonObject.
     * @return JsonObject représentant l'utilisateur.
     */
    public JsonObject toJson() {
        return new JsonObject()
            .put("id", id)
            .put("email", email)
            .put("name", name)
            .put("password", password) // Inclure le mot de passe pour les opérations internes si nécessaire
            .put("createdAt", createdAt.toString())
            .put("updatedAt", updatedAt.toString());
    }

    /**
     * Convertit l'objet User en JsonObject sans le mot de passe (pour les réponses API publiques).
     * @return JsonObject représentant l'utilisateur sans le mot de passe.
     */
    public JsonObject toJsonSafe() {
        return new JsonObject()
            .put("id", id)
            .put("email", email)
            .put("name", name)
            .put("createdAt", createdAt.toString())
            .put("updatedAt", updatedAt.toString());
    }

    /**
     * Crée un objet User à partir d'un JsonObject.
     * @param json Le JsonObject source.
     * @return Un nouvel objet User.
     */
    public static User fromJson(JsonObject json) {
        User user = new User();
        if (json.containsKey("id")) user.setId(json.getString("id"));
        if (json.containsKey("email")) user.setEmail(json.getString("email"));
        if (json.containsKey("name")) user.setName(json.getString("name"));
        if (json.containsKey("password")) user.setPassword(json.getString("password"));
        if (json.containsKey("createdAt")) user.setCreatedAt(LocalDateTime.parse(json.getString("createdAt")));
        if (json.containsKey("updatedAt")) user.setUpdatedAt(LocalDateTime.parse(json.getString("updatedAt")));
        return user;
    }

    /**
     * Valide les champs requis pour un utilisateur.
     * @return Une chaîne d'erreur si invalide, null sinon.
     */
    public String isValid() {
        if (email == null || email.trim().isEmpty()) {
            return "L'email est requis.";
        }
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            return "Format d'email invalide.";
        }
        if (name == null || name.trim().isEmpty()) {
            return "Le nom est requis.";
        }
        if (password == null || password.trim().isEmpty()) {
            return "Le mot de passe est requis.";
        }
        if (password.length() < 6) {
            return "Le mot de passe doit contenir au moins 6 caractères.";
        }
        return null; // Valide
    }
}
