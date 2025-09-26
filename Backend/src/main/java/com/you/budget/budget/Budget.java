package com.you.budget.budget;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modèle Budget - Représente le budget mensuel d'un utilisateur.
 * Contient les 3 catégories principales : loisirs, essentiels, épargne
 * Inclut des méthodes pour la conversion JSON et la validation.
 */
public class Budget {

    private String id;
    private String userId;
    
    // Revenu total mensuel
    private BigDecimal totalIncome;
    
    // Budgets prévus par catégorie
    private BigDecimal loisirsBudget;
    private BigDecimal essentielsBudget;
    private BigDecimal epargneBudget;
    
    // Dépenses actuelles par catégorie
    private BigDecimal loisirsSpent;
    private BigDecimal essentielsSpent;
    private BigDecimal epargneSpent;
    
    // Métadonnées
    private LocalDateTime monthYear; // Mois/année du budget
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Budget() {
        this.id = UUID.randomUUID().toString();
        this.loisirsSpent = BigDecimal.ZERO;
        this.essentielsSpent = BigDecimal.ZERO;
        this.epargneSpent = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.monthYear = LocalDateTime.now(); // Budget du mois actuel
    }

    public Budget(String userId, BigDecimal totalIncome, BigDecimal loisirsBudget, 
                  BigDecimal essentielsBudget, BigDecimal epargneBudget) {
        this(); // Appelle le constructeur par défaut pour initialiser id et dates
        this.userId = userId;
        this.totalIncome = totalIncome;
        this.loisirsBudget = loisirsBudget;
        this.essentielsBudget = essentielsBudget;
        this.epargneBudget = epargneBudget;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getLoisirsBudget() {
        return loisirsBudget;
    }

    public BigDecimal getEssentielsBudget() {
        return essentielsBudget;
    }

    public BigDecimal getEpargneBudget() {
        return epargneBudget;
    }

    public BigDecimal getLoisirsSpent() {
        return loisirsSpent;
    }

    public BigDecimal getEssentielsSpent() {
        return essentielsSpent;
    }

    public BigDecimal getEpargneSpent() {
        return epargneSpent;
    }

    public LocalDateTime getMonthYear() {
        return monthYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public Budget setId(String id) {
        this.id = id;
        return this;
    }

    public Budget setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Budget setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
        return this;
    }

    public Budget setLoisirsBudget(BigDecimal loisirsBudget) {
        this.loisirsBudget = loisirsBudget;
        return this;
    }

    public Budget setEssentielsBudget(BigDecimal essentielsBudget) {
        this.essentielsBudget = essentielsBudget;
        return this;
    }

    public Budget setEpargneBudget(BigDecimal epargneBudget) {
        this.epargneBudget = epargneBudget;
        return this;
    }

    public Budget setLoisirsSpent(BigDecimal loisirsSpent) {
        this.loisirsSpent = loisirsSpent;
        return this;
    }

    public Budget setEssentielsSpent(BigDecimal essentielsSpent) {
        this.essentielsSpent = essentielsSpent;
        return this;
    }

    public Budget setEpargneSpent(BigDecimal epargneSpent) {
        this.epargneSpent = epargneSpent;
        return this;
    }

    public Budget setMonthYear(LocalDateTime monthYear) {
        this.monthYear = monthYear;
        return this;
    }

    public Budget setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Budget setUpdatedAt(LocalDateTime updatedAt) {
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
     * Ajoute une dépense à une catégorie spécifique.
     * @param category La catégorie (loisirs, essentiels, epargne)
     * @param amount Le montant à ajouter
     */
    public void addExpense(String category, BigDecimal amount) {
        switch (category.toLowerCase()) {
            case "loisirs":
                this.loisirsSpent = this.loisirsSpent.add(amount);
                break;
            case "essentiels":
                this.essentielsSpent = this.essentielsSpent.add(amount);
                break;
            case "epargne":
                this.epargneSpent = this.epargneSpent.add(amount);
                break;
            default:
                throw new IllegalArgumentException("Catégorie invalide: " + category);
        }
        touch();
    }

    /**
     * Calcule le montant restant pour une catégorie.
     * @param category La catégorie
     * @return Le montant restant
     */
    public BigDecimal getRemainingAmount(String category) {
        switch (category.toLowerCase()) {
            case "loisirs":
                return this.loisirsBudget.subtract(this.loisirsSpent);
            case "essentiels":
                return this.essentielsBudget.subtract(this.essentielsSpent);
            case "epargne":
                return this.epargneBudget.subtract(this.epargneSpent);
            default:
                throw new IllegalArgumentException("Catégorie invalide: " + category);
        }
    }

    /**
     * Calcule le pourcentage dépensé pour une catégorie.
     * @param category La catégorie
     * @return Le pourcentage dépensé (0-100)
     */
    public double getSpentPercentage(String category) {
        BigDecimal budget, spent;
        switch (category.toLowerCase()) {
            case "loisirs":
                budget = this.loisirsBudget;
                spent = this.loisirsSpent;
                break;
            case "essentiels":
                budget = this.essentielsBudget;
                spent = this.essentielsSpent;
                break;
            case "epargne":
                budget = this.epargneBudget;
                spent = this.epargneSpent;
                break;
            default:
                throw new IllegalArgumentException("Catégorie invalide: " + category);
        }
        
        if (budget.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return spent.divide(budget, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /**
     * Vérifie si une catégorie est dépassée.
     * @param category La catégorie
     * @return true si le budget est dépassé
     */
    public boolean isOverBudget(String category) {
        BigDecimal budget, spent;
        switch (category.toLowerCase()) {
            case "loisirs":
                budget = this.loisirsBudget;
                spent = this.loisirsSpent;
                break;
            case "essentiels":
                budget = this.essentielsBudget;
                spent = this.essentielsSpent;
                break;
            case "epargne":
                budget = this.epargneBudget;
                spent = this.epargneSpent;
                break;
            default:
                throw new IllegalArgumentException("Catégorie invalide: " + category);
        }
        return spent.compareTo(budget) > 0;
    }

    /**
     * Calcule le total des budgets prévus.
     * @return Le total des budgets
     */
    public BigDecimal getTotalBudget() {
        return this.loisirsBudget.add(this.essentielsBudget).add(this.epargneBudget);
    }

    /**
     * Calcule le total des dépenses actuelles.
     * @return Le total des dépenses
     */
    public BigDecimal getTotalSpent() {
        return this.loisirsSpent.add(this.essentielsSpent).add(this.epargneSpent);
    }

    /**
     * Calcule le solde restant (revenu - total dépensé).
     * @return Le solde restant
     */
    public BigDecimal getRemainingBalance() {
        return this.totalIncome.subtract(getTotalSpent());
    }

    /**
     * Vérifie si le budget total dépasse les revenus.
     * @return true si le budget dépasse les revenus
     */
    public boolean isBudgetOverIncome() {
        return getTotalBudget().compareTo(this.totalIncome) > 0;
    }

    /**
     * Convertit l'objet Budget en JsonObject.
     * @return JsonObject représentant le budget.
     */
    public JsonObject toJson() {
        return new JsonObject()
                .put("id", id)
                .put("userId", userId)
                .put("totalIncome", totalIncome.toString())
                .put("loisirsBudget", loisirsBudget.toString())
                .put("essentielsBudget", essentielsBudget.toString())
                .put("epargneBudget", epargneBudget.toString())
                .put("loisirsSpent", loisirsSpent.toString())
                .put("essentielsSpent", essentielsSpent.toString())
                .put("epargneSpent", epargneSpent.toString())
                .put("monthYear", monthYear.toString())
                .put("createdAt", createdAt.toString())
                .put("updatedAt", updatedAt.toString())
                .put("totalBudget", getTotalBudget().toString())
                .put("totalSpent", getTotalSpent().toString())
                .put("remainingBalance", getRemainingBalance().toString())
                .put("isBudgetOverIncome", isBudgetOverIncome())
                .put("categories", new JsonObject()
                    .put("loisirs", new JsonObject()
                        .put("budget", loisirsBudget.toString())
                        .put("spent", loisirsSpent.toString())
                        .put("remaining", getRemainingAmount("loisirs").toString())
                        .put("percentage", getSpentPercentage("loisirs"))
                        .put("isOverBudget", isOverBudget("loisirs")))
                    .put("essentiels", new JsonObject()
                        .put("budget", essentielsBudget.toString())
                        .put("spent", essentielsSpent.toString())
                        .put("remaining", getRemainingAmount("essentiels").toString())
                        .put("percentage", getSpentPercentage("essentiels"))
                        .put("isOverBudget", isOverBudget("essentiels")))
                    .put("epargne", new JsonObject()
                        .put("budget", epargneBudget.toString())
                        .put("spent", epargneSpent.toString())
                        .put("remaining", getRemainingAmount("epargne").toString())
                        .put("percentage", getSpentPercentage("epargne"))
                        .put("isOverBudget", isOverBudget("epargne"))));
    }

    /**
     * Crée un objet Budget à partir d'un JsonObject.
     * @param json Le JsonObject source.
     * @return Un nouvel objet Budget.
     */
    public static Budget fromJson(JsonObject json) {
        Budget budget = new Budget();
        if (json.containsKey("id")) budget.setId(json.getString("id"));
        if (json.containsKey("userId")) budget.setUserId(json.getString("userId"));
        if (json.containsKey("totalIncome")) budget.setTotalIncome(new BigDecimal(json.getString("totalIncome")));
        if (json.containsKey("loisirsBudget")) budget.setLoisirsBudget(new BigDecimal(json.getString("loisirsBudget")));
        if (json.containsKey("essentielsBudget")) budget.setEssentielsBudget(new BigDecimal(json.getString("essentielsBudget")));
        if (json.containsKey("epargneBudget")) budget.setEpargneBudget(new BigDecimal(json.getString("epargneBudget")));
        if (json.containsKey("loisirsSpent")) budget.setLoisirsSpent(new BigDecimal(json.getString("loisirsSpent")));
        if (json.containsKey("essentielsSpent")) budget.setEssentielsSpent(new BigDecimal(json.getString("essentielsSpent")));
        if (json.containsKey("epargneSpent")) budget.setEpargneSpent(new BigDecimal(json.getString("epargneSpent")));
        if (json.containsKey("monthYear")) budget.setMonthYear(LocalDateTime.parse(json.getString("monthYear")));
        if (json.containsKey("createdAt")) budget.setCreatedAt(LocalDateTime.parse(json.getString("createdAt")));
        if (json.containsKey("updatedAt")) budget.setUpdatedAt(LocalDateTime.parse(json.getString("updatedAt")));
        return budget;
    }

    /**
     * Valide les champs requis pour un budget.
     * @return Une chaîne d'erreur si invalide, null sinon.
     */
    public String isValid() {
        if (userId == null || userId.trim().isEmpty()) {
            return "L'ID utilisateur est requis.";
        }
        if (totalIncome == null || totalIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return "Le revenu total doit être supérieur à 0.";
        }
        if (loisirsBudget == null || loisirsBudget.compareTo(BigDecimal.ZERO) < 0) {
            return "Le budget loisirs doit être positif ou nul.";
        }
        if (essentielsBudget == null || essentielsBudget.compareTo(BigDecimal.ZERO) < 0) {
            return "Le budget essentiels doit être positif ou nul.";
        }
        if (epargneBudget == null || epargneBudget.compareTo(BigDecimal.ZERO) < 0) {
            return "Le budget épargne doit être positif ou nul.";
        }
        
        // Vérifier que le budget total ne dépasse pas les revenus
        BigDecimal totalBudget = getTotalBudget();
        if (totalBudget.compareTo(totalIncome) > 0) {
            return "Le budget total (" + totalBudget + ") dépasse les revenus (" + totalIncome + ").";
        }
        
        // Vérifier que l'épargne ne dépasse pas le revenu disponible après essentiels
        BigDecimal revenuDisponibleApresEssentiels = totalIncome.subtract(essentielsBudget);
        if (epargneBudget.compareTo(revenuDisponibleApresEssentiels) > 0) {
            return "L'épargne (" + epargneBudget + ") dépasse le revenu disponible après essentiels (" + revenuDisponibleApresEssentiels + ").";
        }
        
        return null; // Valide
    }
}
