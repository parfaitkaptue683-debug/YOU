package com.you.budget.expense;

import com.you.budget.budget.Budget;
import com.you.budget.budget.BudgetService;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ExpenseService - Couche de service pour la logique métier des dépenses.
 * Gère la création, la mise à jour et le suivi des dépenses individuelles.
 * Intègre avec BudgetService pour la cohérence budgétaire.
 */
public class ExpenseService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);
    private final ExpenseRepository expenseRepository;
    private final BudgetService budgetService;

    public ExpenseService() {
        this.expenseRepository = new ExpenseRepository();
        this.budgetService = new BudgetService();
    }

    /**
     * Crée une nouvelle dépense et met à jour le budget associé.
     * Valide la cohérence avant création.
     * 
     * @param userId L'ID de l'utilisateur
     * @param budgetId L'ID du budget associé
     * @param category La catégorie (loisirs, essentiels, epargne)
     * @param amount Le montant de la dépense
     * @param description La description de la dépense
     * @param paymentMethod La méthode de paiement (optionnel)
     * @param location Le lieu de la dépense (optionnel)
     * @param notes Notes supplémentaires (optionnel)
     * @return CompletableFuture<Expense> La dépense créée
     * @throws IllegalArgumentException Si les données sont invalides
     */
    public CompletableFuture<Expense> createExpense(String userId, String budgetId, String category, 
                                                   BigDecimal amount, String description,
                                                   String paymentMethod, String location, String notes) {
        logger.info("💰 Création d'une nouvelle dépense: {}€ dans {} pour l'utilisateur {}", amount, category, userId);

        // Créer la dépense temporaire pour validation
        Expense expense = new Expense(userId, budgetId, category, amount, description);
        expense.setPaymentMethod(paymentMethod);
        expense.setLocation(location);
        expense.setNotes(notes);

        // Valider la dépense
        String validationError = expense.isValid();
        if (validationError != null) {
            logger.warn("❌ Validation échouée pour la dépense: {}", validationError);
            return CompletableFuture.failedFuture(new IllegalArgumentException(validationError));
        }

        // Vérifier que le budget existe et récupérer les informations
        return budgetService.getCurrentBudget(userId)
            .thenCompose(budgetOpt -> {
                if (budgetOpt.isEmpty()) {
                    logger.warn("⚠️ Aucun budget actuel trouvé pour l'utilisateur: {}", userId);
                    return CompletableFuture.failedFuture(
                        new IllegalArgumentException("Aucun budget actuel trouvé. Créez d'abord un budget."));
                }

                Budget budget = budgetOpt.get();
                
                // Vérifier que le budgetId correspond au budget actuel
                if (!budget.getId().equals(budgetId)) {
                    logger.warn("⚠️ Le budgetId {} ne correspond pas au budget actuel {} de l'utilisateur {}", 
                               budgetId, budget.getId(), userId);
                    return CompletableFuture.failedFuture(
                        new IllegalArgumentException("Le budgetId ne correspond pas au budget actuel de l'utilisateur."));
                }
                
                // Vérifier si l'ajout de cette dépense dépassera le budget
                BigDecimal currentSpent = getCurrentSpentForCategory(budget, category);
                BigDecimal newTotal = currentSpent.add(amount);
                BigDecimal budgetLimit = getBudgetLimitForCategory(budget, category);
                
                if (newTotal.compareTo(budgetLimit) > 0) {
                    BigDecimal overspend = newTotal.subtract(budgetLimit);
                    logger.warn("⚠️ Cette dépense dépasserait le budget {} de {}€", category, overspend);
                    return CompletableFuture.failedFuture(
                        new IllegalArgumentException(
                            String.format("Cette dépense dépasserait votre budget %s de %.2f€", category, overspend)));
                }

                // Sauvegarder la dépense
                logger.info("✅ Dépense validée, sauvegarde en cours...");
                return expenseRepository.save(expense)
                    .thenCompose(savedExpense -> {
                        // Mettre à jour le budget avec la nouvelle dépense (utiliser le budget récupéré)
                        return budgetService.addExpense(budget.getId(), category, amount)
                            .thenApply(updatedBudgetOpt -> {
                                if (updatedBudgetOpt.isPresent()) {
                                    logger.info("✅ Budget mis à jour avec la nouvelle dépense");
                                }
                                return savedExpense;
                            });
                    });
            });
    }

    /**
     * Récupère toutes les dépenses d'un utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @return CompletableFuture<List<Expense>> Liste des dépenses de l'utilisateur
     */
    public CompletableFuture<List<Expense>> getExpensesByUser(String userId) {
        logger.debug("📋 Récupération des dépenses pour l'utilisateur: {}", userId);
        return expenseRepository.findByUserId(userId);
    }

    /**
     * Récupère les dépenses d'un budget spécifique.
     * 
     * @param budgetId L'ID du budget
     * @return CompletableFuture<List<Expense>> Liste des dépenses du budget
     */
    public CompletableFuture<List<Expense>> getExpensesByBudget(String budgetId) {
        logger.debug("📋 Récupération des dépenses pour le budget: {}", budgetId);
        return expenseRepository.findByBudgetId(budgetId);
    }

    /**
     * Récupère les dépenses par catégorie.
     * 
     * @param category La catégorie (loisirs, essentiels, epargne)
     * @return CompletableFuture<List<Expense>> Liste des dépenses de la catégorie
     */
    public CompletableFuture<List<Expense>> getExpensesByCategory(String category) {
        logger.debug("📋 Récupération des dépenses pour la catégorie: {}", category);
        return expenseRepository.findByCategory(category);
    }

    /**
     * Récupère les dépenses dans une période donnée.
     * 
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return CompletableFuture<List<Expense>> Liste des dépenses dans la période
     */
    public CompletableFuture<List<Expense>> getExpensesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("📋 Récupération des dépenses entre {} et {}", startDate, endDate);
        return expenseRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Met à jour une dépense existante.
     * 
     * @param expenseId L'ID de la dépense à mettre à jour
     * @param updatedExpense Les nouvelles données de la dépense
     * @return CompletableFuture<Optional<Expense>> La dépense mise à jour ou vide si non trouvée
     * @throws IllegalArgumentException Si les données sont invalides
     */
    public CompletableFuture<Optional<Expense>> updateExpense(String expenseId, Expense updatedExpense) {
        logger.info("🔄 Mise à jour de la dépense: {}", expenseId);

        return expenseRepository.findById(expenseId)
            .thenCompose(existingExpenseOpt -> {
                if (existingExpenseOpt.isEmpty()) {
                    logger.warn("⚠️ Dépense non trouvée pour la mise à jour: {}", expenseId);
                    return CompletableFuture.completedFuture(Optional.empty());
                }

                Expense existingExpense = existingExpenseOpt.get();
                
                // Appliquer les mises à jour
                if (updatedExpense.getCategory() != null) {
                    existingExpense.setCategory(updatedExpense.getCategory());
                }
                if (updatedExpense.getAmount() != null) {
                    existingExpense.setAmount(updatedExpense.getAmount());
                }
                if (updatedExpense.getDescription() != null) {
                    existingExpense.setDescription(updatedExpense.getDescription());
                }
                if (updatedExpense.getPaymentMethod() != null) {
                    existingExpense.setPaymentMethod(updatedExpense.getPaymentMethod());
                }
                if (updatedExpense.getLocation() != null) {
                    existingExpense.setLocation(updatedExpense.getLocation());
                }
                if (updatedExpense.getNotes() != null) {
                    existingExpense.setNotes(updatedExpense.getNotes());
                }
                if (updatedExpense.getExpenseDate() != null) {
                    existingExpense.setExpenseDate(updatedExpense.getExpenseDate());
                }
                
                existingExpense.touch(); // Mettre à jour la date de modification

                // Valider la dépense mise à jour
                String validationError = existingExpense.isValid();
                if (validationError != null) {
                    logger.warn("❌ Validation échouée pour la mise à jour de la dépense {}: {}", expenseId, validationError);
                    return CompletableFuture.failedFuture(new IllegalArgumentException(validationError));
                }

                logger.info("✅ Dépense mise à jour validée, sauvegarde en cours...");
                return expenseRepository.save(existingExpense).thenApply(Optional::of);
            });
    }

    /**
     * Supprime une dépense par son ID.
     * 
     * @param expenseId L'ID de la dépense à supprimer
     * @return CompletableFuture<Boolean> true si supprimée, false sinon
     */
    public CompletableFuture<Boolean> deleteExpense(String expenseId) {
        logger.info("🗑️ Suppression de la dépense: {}", expenseId);
        return expenseRepository.deleteById(expenseId);
    }

    /**
     * Calcule le total des dépenses pour une catégorie dans un budget.
     * 
     * @param budgetId L'ID du budget
     * @param category La catégorie
     * @return CompletableFuture<BigDecimal> Le total des dépenses
     */
    public CompletableFuture<BigDecimal> calculateTotalForCategory(String budgetId, String category) {
        logger.debug("🧮 Calcul du total pour la catégorie {} dans le budget {}", category, budgetId);
        
        return expenseRepository.findByBudgetId(budgetId)
            .thenApply(expenses -> {
                return expenses.stream()
                    .filter(expense -> category.equalsIgnoreCase(expense.getCategory()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            });
    }

    /**
     * Recherche des dépenses par description.
     * 
     * @param description La description à rechercher
     * @return CompletableFuture<List<Expense>> Liste des dépenses correspondantes
     */
    public CompletableFuture<List<Expense>> searchExpensesByDescription(String description) {
        logger.debug("🔍 Recherche de dépenses par description: {}", description);
        return expenseRepository.findByDescriptionContaining(description);
    }

    /**
     * Génère un résumé des dépenses pour un utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @return CompletableFuture<JsonObject> Résumé des dépenses
     */
    public CompletableFuture<JsonObject> generateExpenseSummary(String userId) {
        logger.info("📊 Génération du résumé des dépenses pour l'utilisateur: {}", userId);
        
        return getExpensesByUser(userId)
            .thenApply(expenses -> {
                JsonObject summary = new JsonObject();
                
                // Calculer les totaux par catégorie
                BigDecimal loisirsTotal = expenses.stream()
                    .filter(e -> "loisirs".equalsIgnoreCase(e.getCategory()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                BigDecimal essentielsTotal = expenses.stream()
                    .filter(e -> "essentiels".equalsIgnoreCase(e.getCategory()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                BigDecimal epargneTotal = expenses.stream()
                    .filter(e -> "epargne".equalsIgnoreCase(e.getCategory()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal grandTotal = loisirsTotal.add(essentielsTotal).add(epargneTotal);
                
                summary.put("totalExpenses", grandTotal.toString())
                    .put("categories", new JsonObject()
                        .put("loisirs", loisirsTotal.toString())
                        .put("essentiels", essentielsTotal.toString())
                        .put("epargne", epargneTotal.toString()))
                    .put("expenseCount", expenses.size())
                    .put("averageExpense", expenses.isEmpty() ? "0" : 
                        grandTotal.divide(BigDecimal.valueOf(expenses.size()), 2, BigDecimal.ROUND_HALF_UP).toString());
                
                return summary;
            });
    }

    /**
     * Méthodes utilitaires privées
     */
    private BigDecimal getCurrentSpentForCategory(Budget budget, String category) {
        switch (category.toLowerCase()) {
            case "loisirs": return budget.getLoisirsSpent();
            case "essentiels": return budget.getEssentielsSpent();
            case "epargne": return budget.getEpargneSpent();
            default: return BigDecimal.ZERO;
        }
    }

    private BigDecimal getBudgetLimitForCategory(Budget budget, String category) {
        switch (category.toLowerCase()) {
            case "loisirs": return budget.getLoisirsBudget();
            case "essentiels": return budget.getEssentielsBudget();
            case "epargne": return budget.getEpargneBudget();
            default: return BigDecimal.ZERO;
        }
    }
}
