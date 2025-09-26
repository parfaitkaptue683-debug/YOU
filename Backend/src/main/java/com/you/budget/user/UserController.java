package com.you.budget.user;

import com.you.budget.http.HttpController;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * UserController - Contrôleur pour gérer les routes API des utilisateurs
 * Définit les endpoints pour l'inscription, connexion, profil, etc.
 * Utilise UserService pour la logique métier
 */
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final UserService userService = new UserService();
    
    /**
     * Enregistre toutes les routes utilisateur
     * @param router Le routeur Vert.x
     */
    public static void registerRoutes(Router router) {
        logger.info("🔗 Enregistrement des routes utilisateur...");
        
        // Routes d'authentification
        router.post("/api/users/register").handler(UserController::handleRegister);
        router.post("/api/users/login").handler(UserController::handleLogin);
        
        // Routes de gestion des utilisateurs
        router.get("/api/users/profile/:id").handler(UserController::handleGetProfile);
        router.get("/api/users").handler(UserController::handleGetAllUsers);
        router.put("/api/users/:id").handler(UserController::handleUpdateUser);
        router.delete("/api/users/:id").handler(UserController::handleDeleteUser);
        
        logger.info("✅ Routes utilisateur enregistrées");
    }
    
    /**
     * Handler pour l'inscription d'un utilisateur
     * POST /api/users/register
     */
    private static void handleRegister(RoutingContext ctx) {
        HttpController.logRequest(ctx);
        
        if (!HttpController.validateJsonBody(ctx)) {
            HttpController.logResponse(ctx, 400);
            return;
        }
        
        JsonObject body = HttpController.getJsonBody(ctx);
        
        // Vérifier les champs requis
        if (!body.containsKey("email") || !body.containsKey("name") || !body.containsKey("password")) {
            HttpController.sendBadRequest(ctx, "Email, nom et mot de passe sont requis");
            HttpController.logResponse(ctx, 400);
            return;
        }
        
        String email = body.getString("email");
        String name = body.getString("name");
        String password = body.getString("password");
        
        logger.info("📝 Tentative d'inscription: {}", email);
        
        // Utiliser le UserService pour créer l'utilisateur
        userService.createUser(email, name, password)
            .thenAccept(user -> {
                JsonObject responseData = user.toJsonSafe();
                HttpController.sendCreated(ctx, "Utilisateur créé avec succès", responseData);
                HttpController.logResponse(ctx, 201);
                logger.info("✅ Inscription réussie: {} ({})", user.getName(), user.getId());
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de l'inscription de {}: {}", email, throwable.getMessage());
                if (throwable.getCause() instanceof IllegalArgumentException) {
                    HttpController.sendBadRequest(ctx, throwable.getCause().getMessage());
                    HttpController.logResponse(ctx, 400);
                } else {
                    HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                    HttpController.logResponse(ctx, 500);
                }
                return null;
            });
    }
    
    /**
     * Handler pour la connexion d'un utilisateur
     * POST /api/users/login
     */
    private static void handleLogin(RoutingContext ctx) {
        HttpController.logRequest(ctx);
        
        if (!HttpController.validateJsonBody(ctx)) {
            HttpController.logResponse(ctx, 400);
            return;
        }
        
        JsonObject body = HttpController.getJsonBody(ctx);
        
        // Vérifier les champs requis
        if (!body.containsKey("email") || !body.containsKey("password")) {
            HttpController.sendBadRequest(ctx, "Email et mot de passe sont requis");
            HttpController.logResponse(ctx, 400);
            return;
        }
        
        String email = body.getString("email");
        String password = body.getString("password");
        
        logger.info("🔐 Tentative de connexion: {}", email);
        
        // Utiliser le UserService pour authentifier l'utilisateur
        userService.authenticateUser(email, password)
            .thenAccept(userOpt -> {
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    JsonObject responseData = user.toJsonSafe();
                    HttpController.sendSuccess(ctx, "Connexion réussie", responseData);
                    HttpController.logResponse(ctx, 200);
                    logger.info("✅ Connexion réussie: {} ({})", user.getName(), user.getId());
                } else {
                    HttpController.sendBadRequest(ctx, "Email ou mot de passe incorrect");
                    HttpController.logResponse(ctx, 401);
                    logger.warn("❌ Échec de connexion: {}", email);
                }
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la connexion de {}: {}", email, throwable.getMessage());
                if (throwable.getCause() instanceof IllegalArgumentException) {
                    HttpController.sendBadRequest(ctx, throwable.getCause().getMessage());
                    HttpController.logResponse(ctx, 400);
                } else {
                    HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                    HttpController.logResponse(ctx, 500);
                }
                return null;
            });
    }
    
    /**
     * Handler pour récupérer le profil d'un utilisateur
     * GET /api/users/profile/:id
     */
    private static void handleGetProfile(RoutingContext ctx) {
        HttpController.logRequest(ctx);
        
        String userId = HttpController.getPathParam(ctx, "id");
        if (userId == null) {
            HttpController.sendBadRequest(ctx, "ID utilisateur requis");
            HttpController.logResponse(ctx, 400);
            return;
        }
        
        logger.info("👤 Récupération du profil: {}", userId);
        
        // Utiliser le UserService pour récupérer l'utilisateur
        userService.getUserById(userId)
            .thenAccept(userOpt -> {
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    JsonObject responseData = user.toJsonSafe();
                    HttpController.sendSuccess(ctx, "Profil utilisateur", responseData);
                    HttpController.logResponse(ctx, 200);
                    logger.info("✅ Profil récupéré: {} ({})", user.getName(), userId);
                } else {
                    HttpController.sendNotFound(ctx, "Utilisateur non trouvé");
                    HttpController.logResponse(ctx, 404);
                    logger.warn("⚠️ Utilisateur non trouvé: {}", userId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la récupération du profil {}: {}", userId, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }
    
    /**
     * Handler pour récupérer tous les utilisateurs
     * GET /api/users
     */
    private static void handleGetAllUsers(RoutingContext ctx) {
        HttpController.logRequest(ctx);
        
        logger.info("📋 Récupération de tous les utilisateurs");
        
        // Utiliser le UserService pour récupérer tous les utilisateurs
        userService.getAllUsers()
            .thenAccept(users -> {
                List<JsonObject> usersData = users.stream()
                    .map(User::toJsonSafe)
                    .toList();
                
                JsonObject responseData = new JsonObject().put("users", usersData);
                HttpController.sendSuccess(ctx, "Liste des utilisateurs", responseData);
                HttpController.logResponse(ctx, 200);
                logger.info("✅ {} utilisateurs récupérés", users.size());
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la récupération des utilisateurs: {}", throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }
    
    /**
     * Handler pour mettre à jour un utilisateur
     * PUT /api/users/:id
     */
    private static void handleUpdateUser(RoutingContext ctx) {
        HttpController.logRequest(ctx);
        
        String userId = HttpController.getPathParam(ctx, "id");
        if (userId == null) {
            HttpController.sendBadRequest(ctx, "ID utilisateur requis");
            HttpController.logResponse(ctx, 400);
            return;
        }
        
        if (!HttpController.validateJsonBody(ctx)) {
            HttpController.logResponse(ctx, 400);
            return;
        }
        
        JsonObject body = HttpController.getJsonBody(ctx);
        String email = body.getString("email");
        String name = body.getString("name");
        String password = body.getString("password");
        
        logger.info("✏️ Mise à jour de l'utilisateur: {}", userId);
        
        // Utiliser le UserService pour mettre à jour l'utilisateur
        userService.updateUser(userId, email, name, password)
            .thenAccept(userOpt -> {
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    JsonObject responseData = user.toJsonSafe();
                    HttpController.sendSuccess(ctx, "Utilisateur mis à jour", responseData);
                    HttpController.logResponse(ctx, 200);
                    logger.info("✅ Utilisateur mis à jour: {} ({})", user.getName(), userId);
                } else {
                    HttpController.sendNotFound(ctx, "Utilisateur non trouvé");
                    HttpController.logResponse(ctx, 404);
                    logger.warn("⚠️ Utilisateur non trouvé pour mise à jour: {}", userId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la mise à jour de l'utilisateur {}: {}", userId, throwable.getMessage());
                if (throwable.getCause() instanceof IllegalArgumentException) {
                    HttpController.sendBadRequest(ctx, throwable.getCause().getMessage());
                    HttpController.logResponse(ctx, 400);
                } else {
                    HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                    HttpController.logResponse(ctx, 500);
                }
                return null;
            });
    }
    
    /**
     * Handler pour supprimer un utilisateur
     * DELETE /api/users/:id
     */
    private static void handleDeleteUser(RoutingContext ctx) {
        HttpController.logRequest(ctx);
        
        String userId = HttpController.getPathParam(ctx, "id");
        if (userId == null) {
            HttpController.sendBadRequest(ctx, "ID utilisateur requis");
            HttpController.logResponse(ctx, 400);
            return;
        }
        
        logger.info("🗑️ Suppression de l'utilisateur: {}", userId);
        
        // Utiliser le UserService pour supprimer l'utilisateur
        userService.deleteUser(userId)
            .thenAccept(deleted -> {
                if (deleted) {
                    HttpController.sendSuccess(ctx, "Utilisateur supprimé avec succès");
                    HttpController.logResponse(ctx, 200);
                    logger.info("✅ Utilisateur supprimé: {}", userId);
                } else {
                    HttpController.sendNotFound(ctx, "Utilisateur non trouvé");
                    HttpController.logResponse(ctx, 404);
                    logger.warn("⚠️ Utilisateur non trouvé pour suppression: {}", userId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la suppression de l'utilisateur {}: {}", userId, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }
}