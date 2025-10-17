package com.you.budget.expense;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.you.budget.http.HttpController;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * ExpenseController - Contrôleur pour gérer les routes API des dépenses
 * Définit les endpoints pour la création, récupération, mise à jour des dépenses
 * Utilise ExpenseService pour la logique métier
 */
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);
    private static final ExpenseService expenseService = new ExpenseService();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // Constructeur privé pour empêcher l'instanciation (classe utilitaire statique)
    private ExpenseController() {}

    /**
     * Enregistre toutes les routes liées aux dépenses sur le routeur principal.
     * @param router Le routeur Vert.x sur lequel enregistrer les routes.
     */
    public static void registerRoutes(Router router) {
        // Routes de gestion des dépenses
        router.post("/api/expenses").handler(ExpenseController::handleCreateExpense);
        router.get("/api/expenses/user/:userId").handler(ExpenseController::handleGetExpensesByUser);
        router.get("/api/expenses/budget/:budgetId").handler(ExpenseController::handleGetExpensesByBudget);
        router.get("/api/expenses/category/:category").handler(ExpenseController::handleGetExpensesByCategory);
        router.get("/api/expenses/search").handler(ExpenseController::handleSearchExpenses);
        router.get("/api/expenses/summary/:userId").handler(ExpenseController::handleGetExpenseSummary);
        router.put("/api/expenses/:id").handler(ExpenseController::handleUpdateExpense);
        router.delete("/api/expenses/:id").handler(ExpenseController::handleDeleteExpense);

        logger.info("✅ Routes expense enregistrées.");
    }

    /**
     * Gère la création d'une nouvelle dépense.
     * Route: POST /api/expenses
     */
    private static void handleCreateExpense(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        if (!HttpController.validateJsonBody(ctx)) {
            HttpController.logResponse(ctx, 400);
            return;
        }

        JsonObject body = HttpController.getJsonBody(ctx);

        // Vérifier les champs requis
        if (!body.containsKey("userId") || !body.containsKey("budgetId") || 
            !body.containsKey("category") || !body.containsKey("amount") || 
            !body.containsKey("description")) {
            HttpController.sendBadRequest(ctx, "userId, budgetId, category, amount et description sont requis");
            HttpController.logResponse(ctx, 400);
            return;
        }

        String userId = body.getString("userId");
        String budgetId = body.getString("budgetId");
        String category = body.getString("category");
        BigDecimal amount = new BigDecimal(body.getString("amount"));
        String description = body.getString("description");
        String paymentMethod = body.getString("paymentMethod");
        String location = body.getString("location");
        String notes = body.getString("notes");

        // Validation supplémentaire des paramètres
        if (budgetId == null || budgetId.trim().isEmpty()) {
            HttpController.sendBadRequest(ctx, "budgetId est requis");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("💰 Création d'une nouvelle dépense: {}€ dans {} pour l'utilisateur {}", amount, category, userId);

        // Utiliser le ExpenseService pour créer la dépense
        expenseService.createExpense(userId, budgetId, category, amount, description, paymentMethod, location, notes)
            .thenAccept(expense -> {
                JsonObject responseData = expense.toJson();
                HttpController.sendCreated(ctx, "Dépense créée avec succès", responseData);
                HttpController.logResponse(ctx, 201);
                logger.info("✅ Dépense créée: {} ({})", expense.getDescription(), expense.getId());
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la création de la dépense pour {}: {}", userId, throwable.getMessage());
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
     * Gère la récupération des dépenses d'un utilisateur.
     * Route: GET /api/expenses/user/:userId
     */
    private static void handleGetExpensesByUser(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String userId = HttpController.getPathParam(ctx, "userId");
        if (userId == null) {
            HttpController.sendBadRequest(ctx, "ID utilisateur manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("📋 Récupération des dépenses pour l'utilisateur: {}", userId);

        // Utiliser le ExpenseService pour récupérer les dépenses
        expenseService.getExpensesByUser(userId)
            .thenAccept(expenses -> {
                List<JsonObject> expensesData = expenses.stream()
                    .map(Expense::toJson)
                    .toList();

                JsonObject responseData = new JsonObject()
                    .put("expenses", expensesData)
                    .put("count", expenses.size());
                HttpController.sendSuccess(ctx, "Liste des dépenses", responseData);
                HttpController.logResponse(ctx, 200);
                logger.info("✅ {} dépenses récupérées pour: {}", expenses.size(), userId);
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la récupération des dépenses pour {}: {}", userId, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }

    /**
     * Gère la récupération des dépenses d'un budget.
     * Route: GET /api/expenses/budget/:budgetId
     */
    private static void handleGetExpensesByBudget(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String budgetId = HttpController.getPathParam(ctx, "budgetId");
        if (budgetId == null) {
            HttpController.sendBadRequest(ctx, "ID budget manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("📋 Récupération des dépenses pour le budget: {}", budgetId);

        // Utiliser le ExpenseService pour récupérer les dépenses
        expenseService.getExpensesByBudget(budgetId)
            .thenAccept(expenses -> {
                List<JsonObject> expensesData = expenses.stream()
                    .map(Expense::toJson)
                    .toList();

                JsonObject responseData = new JsonObject()
                    .put("expenses", expensesData)
                    .put("count", expenses.size());
                HttpController.sendSuccess(ctx, "Liste des dépenses du budget", responseData);
                HttpController.logResponse(ctx, 200);
                logger.info("✅ {} dépenses récupérées pour le budget: {}", expenses.size(), budgetId);
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la récupération des dépenses pour le budget {}: {}", budgetId, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }

    /**
     * Gère la récupération des dépenses par catégorie.
     * Route: GET /api/expenses/category/:category
     */
    private static void handleGetExpensesByCategory(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String category = HttpController.getPathParam(ctx, "category");
        if (category == null) {
            HttpController.sendBadRequest(ctx, "Catégorie manquante");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("📋 Récupération des dépenses pour la catégorie: {}", category);

        // Utiliser le ExpenseService pour récupérer les dépenses
        expenseService.getExpensesByCategory(category)
            .thenAccept(expenses -> {
                List<JsonObject> expensesData = expenses.stream()
                    .map(Expense::toJson)
                    .toList();

                JsonObject responseData = new JsonObject()
                    .put("expenses", expensesData)
                    .put("category", category)
                    .put("count", expenses.size());
                HttpController.sendSuccess(ctx, "Liste des dépenses par catégorie", responseData);
                HttpController.logResponse(ctx, 200);
                logger.info("✅ {} dépenses récupérées pour la catégorie: {}", expenses.size(), category);
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la récupération des dépenses pour la catégorie {}: {}", category, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }

    /**
     * Gère la recherche de dépenses par description.
     * Route: GET /api/expenses/search?q=description
     */
    private static void handleSearchExpenses(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String query = ctx.request().getParam("q");
        if (query == null || query.trim().isEmpty()) {
            HttpController.sendBadRequest(ctx, "Paramètre de recherche 'q' requis");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("🔍 Recherche de dépenses: {}", query);

        // Utiliser le ExpenseService pour rechercher les dépenses
        expenseService.searchExpensesByDescription(query)
            .thenAccept(expenses -> {
                List<JsonObject> expensesData = expenses.stream()
                    .map(Expense::toJson)
                    .toList();

                JsonObject responseData = new JsonObject()
                    .put("expenses", expensesData)
                    .put("query", query)
                    .put("count", expenses.size());
                HttpController.sendSuccess(ctx, "Résultats de recherche", responseData);
                HttpController.logResponse(ctx, 200);
                logger.info("✅ {} dépenses trouvées pour: {}", expenses.size(), query);
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la recherche de dépenses pour '{}': {}", query, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }

    /**
     * Gère la récupération du résumé des dépenses d'un utilisateur.
     * Route: GET /api/expenses/summary/:userId
     */
    private static void handleGetExpenseSummary(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String userId = HttpController.getPathParam(ctx, "userId");
        if (userId == null) {
            HttpController.sendBadRequest(ctx, "ID utilisateur manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("📊 Génération du résumé des dépenses pour: {}", userId);

        // Utiliser le ExpenseService pour générer le résumé
        expenseService.generateExpenseSummary(userId)
            .thenAccept(summary -> {
                HttpController.sendSuccess(ctx, "Résumé des dépenses", summary);
                HttpController.logResponse(ctx, 200);
                logger.info("✅ Résumé des dépenses généré pour: {}", userId);
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la génération du résumé pour {}: {}", userId, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }

    /**
     * Gère la mise à jour d'une dépense.
     * Route: PUT /api/expenses/:id
     */
    private static void handleUpdateExpense(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String expenseId = HttpController.getPathParam(ctx, "id");
        if (expenseId == null) {
            HttpController.sendBadRequest(ctx, "ID dépense manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        if (!HttpController.validateJsonBody(ctx)) {
            HttpController.logResponse(ctx, 400);
            return;
        }

        JsonObject body = HttpController.getJsonBody(ctx);
        Expense updatedExpense = Expense.fromJson(body); // Crée un Expense partiel pour la mise à jour

        logger.info("🔄 Mise à jour de la dépense {}: {}", expenseId, body.encodePrettily());

        expenseService.updateExpense(expenseId, updatedExpense)
            .thenAccept(expenseOpt -> {
                if (expenseOpt.isPresent()) {
                    Expense expense = expenseOpt.get();
                    JsonObject responseData = expense.toJson();
                    HttpController.sendSuccess(ctx, "Dépense mise à jour avec succès", responseData);
                    HttpController.logResponse(ctx, 200);
                    logger.info("✅ Dépense mise à jour: {} ({})", expense.getDescription(), expense.getId());
                } else {
                    HttpController.sendNotFound(ctx, "Dépense non trouvée");
                    HttpController.logResponse(ctx, 404);
                    logger.warn("⚠️ Dépense non trouvée pour la mise à jour: {}", expenseId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la mise à jour de la dépense {}: {}", expenseId, throwable.getMessage());
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
     * Gère la suppression d'une dépense.
     * Route: DELETE /api/expenses/:id
     */
    private static void handleDeleteExpense(RoutingContext ctx) {
        HttpController.logRequest(ctx);

        String expenseId = HttpController.getPathParam(ctx, "id");
        if (expenseId == null) {
            HttpController.sendBadRequest(ctx, "ID dépense manquant");
            HttpController.logResponse(ctx, 400);
            return;
        }

        logger.info("🗑️ Suppression de la dépense: {}", expenseId);

        expenseService.deleteExpense(expenseId)
            .thenAccept(deleted -> {
                if (deleted) {
                    HttpController.sendSuccess(ctx, "Dépense supprimée avec succès", new JsonObject().put("id", expenseId));
                    HttpController.logResponse(ctx, 200);
                    logger.info("✅ Dépense supprimée: {}", expenseId);
                } else {
                    HttpController.sendNotFound(ctx, "Dépense non trouvée");
                    HttpController.logResponse(ctx, 404);
                    logger.warn("⚠️ Dépense non trouvée pour la suppression: {}", expenseId);
                }
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la suppression de la dépense {}: {}", expenseId, throwable.getMessage());
                HttpController.sendInternalError(ctx, "Erreur interne du serveur");
                HttpController.logResponse(ctx, 500);
                return null;
            });
    }
}
