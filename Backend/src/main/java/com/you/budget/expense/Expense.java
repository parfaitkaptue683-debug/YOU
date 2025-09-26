package com.you.budget.expense;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modèle Expense - Représente une dépense individuelle dans l'application.
 * Inclut des méthodes pour la conversion JSON et la validation.
 */
public class Expense {

    private String id;
    private String userId;
    private String budgetId;
    private String category; // "loisirs", "essentiels", "epargne"
    private BigDecimal amount;
    private String description;
    private String paymentMethod; // "cash", "card", "transfer", etc.
    private String location;
    private String notes;
    private LocalDateTime expenseDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Expense() {
        this.id = UUID.randomUUID().toString();
        this.expenseDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Expense(String userId, String budgetId, String category, BigDecimal amount, String description) {
        this(); // Appelle le constructeur par défaut pour initialiser id et dates
        this.userId = userId;
        this.budgetId = budgetId;
        this.category = category;
        this.amount = amount;
        this.description = description;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getBudgetId() {
        return budgetId;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getLocation() {
        return location;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getExpenseDate() {
        return expenseDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public Expense setId(String id) {
        this.id = id;
        return this;
    }

    public Expense setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Expense setBudgetId(String budgetId) {
        this.budgetId = budgetId;
        return this;
    }

    public Expense setCategory(String category) {
        this.category = category;
        return this;
    }

    public Expense setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public Expense setDescription(String description) {
        this.description = description;
        return this;
    }

    public Expense setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public Expense setLocation(String location) {
        this.location = location;
        return this;
    }

    public Expense setNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public Expense setExpenseDate(LocalDateTime expenseDate) {
        this.expenseDate = expenseDate;
        return this;
    }

    public Expense setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Expense setUpdatedAt(LocalDateTime updatedAt) {
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
     * Vérifie si la catégorie est valide.
     * @return true si la catégorie est valide
     */
    public boolean isValidCategory() {
        return "loisirs".equalsIgnoreCase(category) || 
               "essentiels".equalsIgnoreCase(category) || 
               "epargne".equalsIgnoreCase(category);
    }

    /**
     * Convertit l'objet Expense en JsonObject.
     * @return JsonObject représentant la dépense.
     */
    public JsonObject toJson() {
        return new JsonObject()
                .put("id", id)
                .put("userId", userId)
                .put("budgetId", budgetId)
                .put("category", category)
                .put("amount", amount.toString())
                .put("description", description)
                .put("paymentMethod", paymentMethod)
                .put("location", location)
                .put("notes", notes)
                .put("expenseDate", expenseDate.toString())
                .put("createdAt", createdAt.toString())
                .put("updatedAt", updatedAt.toString())
                .put("isValidCategory", isValidCategory());
    }

    /**
     * Crée un objet Expense à partir d'un JsonObject.
     * @param json Le JsonObject source.
     * @return Un nouvel objet Expense.
     */
    public static Expense fromJson(JsonObject json) {
        Expense expense = new Expense();
        if (json.containsKey("id")) expense.setId(json.getString("id"));
        if (json.containsKey("userId")) expense.setUserId(json.getString("userId"));
        if (json.containsKey("budgetId")) expense.setBudgetId(json.getString("budgetId"));
        if (json.containsKey("category")) expense.setCategory(json.getString("category"));
        if (json.containsKey("amount")) expense.setAmount(new BigDecimal(json.getString("amount")));
        if (json.containsKey("description")) expense.setDescription(json.getString("description"));
        if (json.containsKey("paymentMethod")) expense.setPaymentMethod(json.getString("paymentMethod"));
        if (json.containsKey("location")) expense.setLocation(json.getString("location"));
        if (json.containsKey("notes")) expense.setNotes(json.getString("notes"));
        if (json.containsKey("expenseDate")) expense.setExpenseDate(LocalDateTime.parse(json.getString("expenseDate")));
        if (json.containsKey("createdAt")) expense.setCreatedAt(LocalDateTime.parse(json.getString("createdAt")));
        if (json.containsKey("updatedAt")) expense.setUpdatedAt(LocalDateTime.parse(json.getString("updatedAt")));
        return expense;
    }

    /**
     * Valide les champs requis pour une dépense.
     * @return Une chaîne d'erreur si invalide, null sinon.
     */
    public String isValid() {
        if (userId == null || userId.trim().isEmpty()) {
            return "L'ID utilisateur est requis.";
        }
        if (budgetId == null || budgetId.trim().isEmpty()) {
            return "L'ID budget est requis.";
        }
        if (category == null || category.trim().isEmpty()) {
            return "La catégorie est requise.";
        }
        if (!isValidCategory()) {
            return "La catégorie doit être 'loisirs', 'essentiels' ou 'epargne'.";
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Le montant doit être supérieur à 0.";
        }
        if (description == null || description.trim().isEmpty()) {
            return "La description est requise.";
        }
        if (description.length() < 3) {
            return "La description doit contenir au moins 3 caractères.";
        }
        return null; // Valide
    }
}
