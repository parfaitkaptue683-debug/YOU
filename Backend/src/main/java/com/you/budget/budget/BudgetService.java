package com.you.budget.budget;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * BudgetService - Couche de service pour la logique métier des budgets.
 * Gère la création, la mise à jour et la validation des budgets mensuels.
 * Implémente les règles de cohérence budgétaire.
 */
public class BudgetService {

    private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);
    private final BudgetRepository budgetRepository;

    public BudgetService() {
        this.budgetRepository = new BudgetRepository();
    }

    /**
     * Crée un nouveau budget mensuel pour un utilisateur.
     * Valide la cohérence budgétaire avant création.
     * 
     * @param userId L'ID de l'utilisateur
     * @param totalIncome Le revenu mensuel total
     * @param loisirsBudget Le budget prévu pour les loisirs
     * @param essentielsBudget Le budget prévu pour les essentiels
     * @param epargneBudget Le budget prévu pour l'épargne
     * @return CompletableFuture<Budget> Le budget créé
     * @throws IllegalArgumentException Si les données sont invalides
     */
    public CompletableFuture<Budget> createBudget(String userId, BigDecimal totalIncome, 
                                                 BigDecimal loisirsBudget, BigDecimal essentielsBudget, 
                                                 BigDecimal epargneBudget) {
        logger.info("📊 Création d'un nouveau budget pour l'utilisateur: {}", userId);

        // Créer le budget temporaire pour validation
        Budget budget = new Budget(userId, totalIncome, loisirsBudget, essentielsBudget, epargneBudget);
        
        // Valider la cohérence budgétaire
        String validationError = budget.isValid();
        if (validationError != null) {
            logger.warn("❌ Validation échouée pour le budget de {}: {}", userId, validationError);
            return CompletableFuture.failedFuture(new IllegalArgumentException(validationError));
        }

        // Vérifier s'il existe déjà un budget pour ce mois
        // Utiliser le premier jour du mois pour la comparaison
        LocalDateTime firstDayOfMonth = budget.getMonthYear().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return budgetRepository.findByUserIdAndMonth(userId, firstDayOfMonth)
            .thenCompose(existingBudgetOpt -> {
                if (existingBudgetOpt.isPresent()) {
                    logger.warn("⚠️ Budget existant trouvé pour {} en {}", userId, budget.getMonthYear());
                    return CompletableFuture.failedFuture(
                        new IllegalArgumentException("Un budget existe déjà pour ce mois. Utilisez la mise à jour."));
                }

                logger.info("✅ Budget validé, sauvegarde en cours...");
                return budgetRepository.save(budget);
            });
    }

    /**
     * Met à jour un budget existant.
     * Valide la cohérence budgétaire avant mise à jour.
     * 
     * @param budgetId L'ID du budget à mettre à jour
     * @param updatedBudget Les nouvelles données du budget
     * @return CompletableFuture<Optional<Budget>> Le budget mis à jour ou vide si non trouvé
     * @throws IllegalArgumentException Si les données sont invalides
     */
    public CompletableFuture<Optional<Budget>> updateBudget(String budgetId, Budget updatedBudget) {
        logger.info("🔄 Mise à jour du budget: {}", budgetId);

        return budgetRepository.findById(budgetId)
            .thenCompose(existingBudgetOpt -> {
                if (existingBudgetOpt.isEmpty()) {
                    logger.warn("⚠️ Budget non trouvé pour la mise à jour: {}", budgetId);
                    return CompletableFuture.completedFuture(Optional.empty());
                }

                Budget existingBudget = existingBudgetOpt.get();
                
                // Appliquer les mises à jour
                if (updatedBudget.getTotalIncome() != null) {
                    existingBudget.setTotalIncome(updatedBudget.getTotalIncome());
                }
                if (updatedBudget.getLoisirsBudget() != null) {
                    existingBudget.setLoisirsBudget(updatedBudget.getLoisirsBudget());
                }
                if (updatedBudget.getEssentielsBudget() != null) {
                    existingBudget.setEssentielsBudget(updatedBudget.getEssentielsBudget());
                }
                if (updatedBudget.getEpargneBudget() != null) {
                    existingBudget.setEpargneBudget(updatedBudget.getEpargneBudget());
                }
                
                existingBudget.touch(); // Mettre à jour la date de modification

                // Valider la cohérence budgétaire
                String validationError = existingBudget.isValid();
                if (validationError != null) {
                    logger.warn("❌ Validation échouée pour la mise à jour du budget {}: {}", budgetId, validationError);
                    return CompletableFuture.failedFuture(new IllegalArgumentException(validationError));
                }

                logger.info("✅ Budget mis à jour validé, sauvegarde en cours...");
                return budgetRepository.save(existingBudget).thenApply(Optional::of);
            });
    }

    /**
     * Ajoute une dépense à une catégorie spécifique.
     * Met à jour automatiquement les montants dépensés.
     * 
     * @param budgetId L'ID du budget
     * @param category La catégorie (loisirs, essentiels, epargne)
     * @param amount Le montant de la dépense
     * @return CompletableFuture<Optional<Budget>> Le budget mis à jour ou vide si non trouvé
     * @throws IllegalArgumentException Si la catégorie ou le montant est invalide
     */
    public CompletableFuture<Optional<Budget>> addExpense(String budgetId, String category, BigDecimal amount) {
        logger.info("💰 Ajout d'une dépense de {}€ dans la catégorie {} au budget {}", amount, category, budgetId);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Le montant doit être supérieur à 0."));
        }

        return budgetRepository.findById(budgetId)
            .thenCompose(budgetOpt -> {
                if (budgetOpt.isEmpty()) {
                    logger.warn("⚠️ Budget non trouvé pour l'ajout de dépense: {}", budgetId);
                    return CompletableFuture.completedFuture(Optional.empty());
                }

                Budget budget = budgetOpt.get();
                
                try {
                    // Ajouter la dépense
                    budget.addExpense(category, amount);
                    
                    // Vérifier les alertes
                    checkBudgetAlerts(budget, category);
                    
                    logger.info("✅ Dépense ajoutée avec succès");
                    return budgetRepository.save(budget).thenApply(Optional::of);
                    
                } catch (IllegalArgumentException e) {
                    logger.error("❌ Erreur lors de l'ajout de la dépense: {}", e.getMessage());
                    return CompletableFuture.failedFuture(e);
                }
            });
    }

    /**
     * Récupère le budget actuel d'un utilisateur (mois en cours).
     * 
     * @param userId L'ID de l'utilisateur
     * @return CompletableFuture<Optional<Budget>> Le budget actuel ou vide si non trouvé
     */
    public CompletableFuture<Optional<Budget>> getCurrentBudget(String userId) {
        logger.debug("📋 Récupération du budget actuel pour: {}", userId);
        
        // Utiliser le premier jour du mois actuel pour la comparaison
        LocalDateTime currentMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return budgetRepository.findByUserIdAndMonth(userId, currentMonth);
    }

    /**
     * Récupère tous les budgets d'un utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @return CompletableFuture<List<Budget>> Liste des budgets de l'utilisateur
     */
    public CompletableFuture<List<Budget>> getAllBudgets(String userId) {
        logger.debug("📋 Récupération de tous les budgets pour: {}", userId);
        return budgetRepository.findByUserId(userId);
    }

    /**
     * Supprime un budget par son ID.
     * 
     * @param budgetId L'ID du budget à supprimer
     * @return CompletableFuture<Boolean> true si supprimé, false sinon
     */
    public CompletableFuture<Boolean> deleteBudget(String budgetId) {
        logger.info("🗑️ Suppression du budget: {}", budgetId);
        return budgetRepository.deleteById(budgetId);
    }

    /**
     * Calcule les ajustements automatiques pour équilibrer le budget.
     * 
     * @param budget Le budget à analyser
     * @return JsonObject avec les ajustements suggérés
     */
    public JsonObject calculateAutoAdjustments(Budget budget) {
        logger.info("🔧 Calcul des ajustements automatiques pour le budget: {}", budget.getId());

        BigDecimal totalIncome = budget.getTotalIncome();
        BigDecimal totalBudget = budget.getTotalBudget();
        
        JsonObject adjustments = new JsonObject();
        
        if (totalBudget.compareTo(totalIncome) > 0) {
            // Budget total dépasse les revenus
            BigDecimal reductionNeeded = totalBudget.subtract(totalIncome);
            
            // Calculer les réductions proportionnelles
            BigDecimal loisirsRatio = budget.getLoisirsBudget().divide(totalBudget, 4, BigDecimal.ROUND_HALF_UP);
            BigDecimal essentielsRatio = budget.getEssentielsBudget().divide(totalBudget, 4, BigDecimal.ROUND_HALF_UP);
            BigDecimal epargneRatio = budget.getEpargneBudget().divide(totalBudget, 4, BigDecimal.ROUND_HALF_UP);
            
            adjustments.put("type", "reduction")
                .put("reason", "Budget total dépasse les revenus")
                .put("reductionNeeded", reductionNeeded.toString())
                .put("suggestions", new JsonObject()
                    .put("loisirs", budget.getLoisirsBudget().subtract(reductionNeeded.multiply(loisirsRatio)).toString())
                    .put("essentiels", budget.getEssentielsBudget().subtract(reductionNeeded.multiply(essentielsRatio)).toString())
                    .put("epargne", budget.getEpargneBudget().subtract(reductionNeeded.multiply(epargneRatio)).toString()));
        } else {
            // Vérifier l'épargne
            BigDecimal revenuDisponibleApresEssentiels = totalIncome.subtract(budget.getEssentielsBudget());
            if (budget.getEpargneBudget().compareTo(revenuDisponibleApresEssentiels) > 0) {
                adjustments.put("type", "epargne_adjustment")
                    .put("reason", "Épargne dépasse le revenu disponible après essentiels")
                    .put("maxEpargne", revenuDisponibleApresEssentiels.toString())
                    .put("suggestions", new JsonObject()
                        .put("epargne", revenuDisponibleApresEssentiels.toString()));
            } else {
                adjustments.put("type", "balanced")
                    .put("reason", "Budget déjà équilibré")
                    .put("suggestions", new JsonObject());
            }
        }
        
        return adjustments;
    }

    /**
     * Vérifie les alertes de budget et génère des notifications.
     * 
     * @param budget Le budget à vérifier
     * @param category La catégorie concernée
     */
    private void checkBudgetAlerts(Budget budget, String category) {
        double percentage = budget.getSpentPercentage(category);
        
        if (percentage >= 100) {
            logger.warn("🚨 ALERTE: Budget {} dépassé de {}%", category, Math.round(percentage - 100));
        } else if (percentage >= 90) {
            logger.warn("⚠️ ALERTE: Budget {} à 90%", category);
        } else if (percentage >= 75) {
            logger.warn("⚠️ ALERTE: Budget {} à 75%", category);
        }
    }

    /**
     * Valide la cohérence d'un budget avant sauvegarde.
     * 
     * @param budget Le budget à valider
     * @return CompletableFuture<Boolean> true si valide, false sinon
     */
    public CompletableFuture<Boolean> validateBudget(Budget budget) {
        String validationError = budget.isValid();
        if (validationError != null) {
            logger.warn("❌ Budget invalide: {}", validationError);
            return CompletableFuture.completedFuture(false);
        }
        
        logger.info("✅ Budget valide");
        return CompletableFuture.completedFuture(true);
    }
}
