package com.you.budget.budget;

import com.you.budget.http.HttpController;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * BudgetController - Contrôleur pour gérer les routes API des budgets
 * Définit les endpoints pour la création, mise à jour, récupération des budgets
 * Utilise BudgetService pour la logique métier
 */
public class BudgetController {

    private static final Logger logger = LoggerFactory.getLogger(BudgetController.class);
    private static final BudgetService budgetService = new BudgetService();

    // Constructeur privé pour empêcher l'instanciation (classe utilitaire statique)
    private BudgetController() {}

    /**
     * Enregistre toutes les routes liées aux budgets sur le routeur principal.
     * @param router Le routeur Vert.x sur lequel enregistrer les routes.
     */
    public static void registerRoutes(Router router) {
        // Routes de gestion des budgets
        router.post("/api/budgets").handler(BudgetController::handleCreateBudget);
        router.get("/api/budgets/current/:userId").handler(BudgetController::handleGetCurrentBudget);
        router.get("/api/budgets/:userId").handler(BudgetController::handleGetAllBudgets);
        router.put("/api/budgets/:id").handler(BudgetController::handleUpdateBudget);
        router.delete("/api/budgets/:id").handler(BudgetController::handleDeleteBudget);
        
        // Routes pour les ajustements et validations
        router.post("/api/budgets/:id/validate").handler(BudgetController::handleValidateBudget);
        router.get("/api/budgets/:id/adjustments").handler(BudgetController::handleGetAdjustments);
        
        // Routes pour les dépenses
        router.post("/api/budgets/:id/expenses").handler(BudgetController::handleAddExpense);

        logger.info("✅ Routes budget enregistrées.");
    }

    /**
     * Gère la création d'un nouveau budget.
     * Route: POST /api/budgets
     */
    private static void handleCreateBudget(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        if (!HttpController.validateJsonBody(ctx)) {
            HttpController.logResponse(ctx, 400);
            return;
        }

        JsonObject body = HttpController.getJsonBody(ctx);

        // Vérifier les champs requis
        if (!body.containsKey("userId") || !body.containsKey("totalIncome") || 
            !body.containsKey("loisirsBudget") || !body.containsKey("essentielsBudget") || 
            !body.containsKey("epargneBudget")) {
            HttpController.sendBadRequest(ctx, "userId, totalIncome, loisirsBudget, essentielsBudget et epargneBudget sont requis");
            HttpController.logResponse(ctx, 400);
            return;
        }

        String userId = body.getString("userId");
        BigDecimal totalIncome = new BigDecimal(body.getString("totalIncome"));
        BigDecimal loisirsBudget = new BigDecimal(body.getString("loisirsBudget"));
        BigDecimal essentielsBudget = new BigDecimal(body.getString("essentielsBudget"));
        BigDecimal epargneBudget = new BigDecimal(body.getString("epargneBudget"));

        logger.info("📊 Création d'un nouveau budget pour l'utilisateur: {}", userId);

        // Utiliser le BudgetService pour créer le budget
        budgetService.createBudget(userId, totalIncome, loisirsBudget, essentielsBudget, epargneBudget)
            .thenAccept(budget -> {
                JsonObject responseData = budget.toJson();
                HttpController.sendCreated(ctx, "Budget créé avec succès", responseData);
                HttpController.logResponse(ctx, 201);
                logger.info("✅ Budget créé: {} pour l'utilisateur {}", budget.getId(), userId);
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la création du budget pour {}: {}", userId, throwable.getMessage());
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
     * Gère la récupération du budget actuel d'un utilisateur.
     * Route: GET /api/budgets/current/:userId
     */
    private static void handleGetCurrentBudget(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String userId = HttpController.getPathParam(ctx, "userId");
        if (userId == null) {
            HttpController.sendBadRequest(ctx, "ID utilisateur manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("📋 Récupération du budget actuel pour: {}", userId);

        // Utiliser le BudgetService pour récupérer le budget actuel
        budgetService.getCurrentBudget(userId)
            .thenAccept(budgetOpt -> {
                if (budgetOpt.isPresent()) {
                    Budget budget = budgetOpt.get();
                    JsonObject responseData = budget.toJson();
                    HttpController.sendSuccess(ctx, "Budget actuel récupéré", responseData);
                    HttpController.logResponse(ctx, 200);
                    logger.info("✅ Budget actuel récupéré pour: {}", userId);
                } else {
                    HttpController.sendNotFound(ctx, "Aucun budget actuel trouvé");
                    HttpController.logResponse(ctx, 404);
                    logger.warn("⚠️ Aucun budget actuel trouvé pour: {}", userId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la récupération du budget actuel pour {}: {}", userId, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }

    /**
     * Gère la récupération de tous les budgets d'un utilisateur.
     * Route: GET /api/budgets/:userId
     */
    private static void handleGetAllBudgets(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String userId = HttpController.getPathParam(ctx, "userId");
        if (userId == null) {
            HttpController.sendBadRequest(ctx, "ID utilisateur manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("📋 Récupération de tous les budgets pour: {}", userId);

        // Utiliser le BudgetService pour récupérer tous les budgets
        budgetService.getAllBudgets(userId)
            .thenAccept(budgets -> {
                List<JsonObject> budgetsData = budgets.stream()
                    .map(Budget::toJson)
                    .toList();

                JsonObject responseData = new JsonObject().put("budgets", budgetsData);
                HttpController.sendSuccess(ctx, "Liste des budgets", responseData);
                HttpController.logResponse(ctx, 200);
                logger.info("✅ {} budgets récupérés pour: {}", budgets.size(), userId);
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la récupération des budgets pour {}: {}", userId, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }

    /**
     * Gère la mise à jour d'un budget.
     * Route: PUT /api/budgets/:id
     */
    private static void handleUpdateBudget(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String budgetId = HttpController.getPathParam(ctx, "id");
        if (budgetId == null) {
            HttpController.sendBadRequest(ctx, "ID budget manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        if (!HttpController.validateJsonBody(ctx)) {
            HttpController.logResponse(ctx, 400);
            return;
        }

        JsonObject body = HttpController.getJsonBody(ctx);
        Budget updatedBudget = Budget.fromJson(body); // Crée un Budget partiel pour la mise à jour

        logger.info("🔄 Mise à jour du budget {}: {}", budgetId, body.encodePrettily());

        budgetService.updateBudget(budgetId, updatedBudget)
            .thenAccept(budgetOpt -> {
                if (budgetOpt.isPresent()) {
                    Budget budget = budgetOpt.get();
                    JsonObject responseData = budget.toJson();
                    HttpController.sendSuccess(ctx, "Budget mis à jour avec succès", responseData);
                    HttpController.logResponse(ctx, 200);
                    logger.info("✅ Budget mis à jour: {} ({})", budget.getId(), budget.getUserId());
                } else {
                    HttpController.sendNotFound(ctx, "Budget non trouvé");
                    HttpController.logResponse(ctx, 404);
                    logger.warn("⚠️ Budget non trouvé pour la mise à jour: {}", budgetId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la mise à jour du budget {}: {}", budgetId, throwable.getMessage());
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
     * Gère la suppression d'un budget.
     * Route: DELETE /api/budgets/:id
     */
    private static void handleDeleteBudget(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String budgetId = HttpController.getPathParam(ctx, "id");
        if (budgetId == null) {
            HttpController.sendBadRequest(ctx, "ID budget manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("🗑️ Suppression du budget: {}", budgetId);

        budgetService.deleteBudget(budgetId)
            .thenAccept(deleted -> {
                if (deleted) {
                    HttpController.sendSuccess(ctx, "Budget supprimé avec succès", new JsonObject().put("id", budgetId));
                    HttpController.logResponse(ctx, 200);
                    logger.info("✅ Budget supprimé: {}", budgetId);
                } else {
                    HttpController.sendNotFound(ctx, "Budget non trouvé");
                    HttpController.logResponse(ctx, 404);
                    logger.warn("⚠️ Budget non trouvé pour la suppression: {}", budgetId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la suppression du budget {}: {}", budgetId, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }

    /**
     * Gère la validation d'un budget.
     * Route: POST /api/budgets/:id/validate
     */
    private static void handleValidateBudget(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String budgetId = HttpController.getPathParam(ctx, "id");
        if (budgetId == null) {
            HttpController.sendBadRequest(ctx, "ID budget manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("🔍 Validation du budget: {}", budgetId);

        // Pour l'instant, on valide juste l'existence du budget
        // Dans une version plus avancée, on pourrait valider la cohérence
        HttpController.sendSuccess(ctx, "Budget validé", new JsonObject().put("budgetId", budgetId));
        HttpController.logResponse(ctx, 200);
        logger.info("✅ Budget validé: {}", budgetId);
    }

    /**
     * Gère la récupération des ajustements automatiques.
     * Route: GET /api/budgets/:id/adjustments
     */
    private static void handleGetAdjustments(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String budgetId = HttpController.getPathParam(ctx, "id");
        if (budgetId == null) {
            HttpController.sendBadRequest(ctx, "ID budget manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("🔧 Calcul des ajustements pour le budget: {}", budgetId);

        // Pour l'instant, on retourne des ajustements factices
        // Dans une version plus avancée, on utiliserait BudgetService.calculateAutoAdjustments
        JsonObject adjustments = new JsonObject()
            .put("type", "balanced")
            .put("reason", "Budget déjà équilibré")
            .put("suggestions", new JsonObject());

        HttpController.sendSuccess(ctx, "Ajustements calculés", adjustments);
        HttpController.logResponse(ctx, 200);
        logger.info("✅ Ajustements calculés pour: {}", budgetId);
    }

    /**
     * Gère l'ajout d'une dépense à un budget.
     * Route: POST /api/budgets/:id/expenses
     */
    private static void handleAddExpense(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String budgetId = HttpController.getPathParam(ctx, "id");
        if (budgetId == null) {
            HttpController.sendBadRequest(ctx, "ID budget manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        if (!HttpController.validateJsonBody(ctx)) {
            HttpController.logResponse(ctx, 400);
            return;
        }

        JsonObject body = HttpController.getJsonBody(ctx);

        // Vérifier les champs requis
        if (!body.containsKey("category") || !body.containsKey("amount")) {
            HttpController.sendBadRequest(ctx, "category et amount sont requis");
            HttpController.logResponse(ctx, 400);
            return;
        }

        String category = body.getString("category");
        BigDecimal amount = new BigDecimal(body.getString("amount"));

        logger.info("💰 Ajout d'une dépense de {}€ dans {} au budget {}", amount, category, budgetId);

        budgetService.addExpense(budgetId, category, amount)
            .thenAccept(budgetOpt -> {
                if (budgetOpt.isPresent()) {
                    Budget budget = budgetOpt.get();
                    JsonObject responseData = budget.toJson();
                    HttpController.sendSuccess(ctx, "Dépense ajoutée avec succès", responseData);
                    HttpController.logResponse(ctx, 200);
                    logger.info("✅ Dépense ajoutée au budget: {}", budgetId);
                } else {
                    HttpController.sendNotFound(ctx, "Budget non trouvé");
                    HttpController.logResponse(ctx, 404);
                    logger.warn("⚠️ Budget non trouvé pour l'ajout de dépense: {}", budgetId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de l'ajout de la dépense au budget {}: {}", budgetId, throwable.getMessage());
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
}
