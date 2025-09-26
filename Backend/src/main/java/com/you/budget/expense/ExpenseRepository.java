package com.you.budget.expense;

import com.you.budget.database.DatabaseClient;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ExpenseRepository - Couche d'accès aux données pour les dépenses
 * Utilise PostgreSQL avec Vert.x JDBCPool pour les opérations CRUD
 * Obtient le pool de connexions via DatabaseClient
 */
public class ExpenseRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpenseRepository.class);
    private final JDBCPool jdbcPool;
    
    public ExpenseRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }
    
    /**
     * Sauvegarde une dépense
     * @param expense La dépense à sauvegarder
     * @return CompletableFuture<Expense> La dépense sauvegardée
     */
    public CompletableFuture<Expense> save(Expense expense) {
        CompletableFuture<Expense> future = new CompletableFuture<>();
        
        // Vérifier si la dépense existe déjà
        jdbcPool.preparedQuery("SELECT id FROM expenses WHERE id = $1")
            .execute(Tuple.of(expense.getId()))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    if (rows.size() > 0) {
                        // Mettre à jour la dépense existante
                        updateExpense(expense, future);
                    } else {
                        // Insérer une nouvelle dépense
                        insertExpense(expense, future);
                    }
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Insère une nouvelle dépense
     */
    private void insertExpense(Expense expense, CompletableFuture<Expense> future) {
        jdbcPool.preparedQuery("""
            INSERT INTO expenses (id, user_id, budget_id, category, amount, description, 
                                payment_method, location, notes, expense_date, created_at, updated_at) 
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
            """)
            .execute(Tuple.of(
                expense.getId(),
                expense.getUserId(),
                expense.getBudgetId(),
                expense.getCategory(),
                expense.getAmount().toString(),
                expense.getDescription(),
                expense.getPaymentMethod(),
                expense.getLocation(),
                expense.getNotes(),
                expense.getExpenseDate(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
            ))
            .onComplete(result -> {
                if (result.succeeded()) {
                    logger.info("✅ Dépense insérée: {} ({})", expense.getDescription(), expense.getId());
                    future.complete(expense);
                } else {
                    logger.error("❌ Erreur lors de l'insertion de la dépense: {}", result.cause().getMessage());
                    future.completeExceptionally(result.cause());
                }
            });
    }
    
    /**
     * Met à jour une dépense existante
     */
    private void updateExpense(Expense expense, CompletableFuture<Expense> future) {
        jdbcPool.preparedQuery("""
            UPDATE expenses 
            SET user_id = $2, budget_id = $3, category = $4, amount = $5, 
                description = $6, payment_method = $7, location = $8, 
                notes = $9, expense_date = $10, updated_at = $11 
            WHERE id = $1
            """)
            .execute(Tuple.of(
                expense.getId(),
                expense.getUserId(),
                expense.getBudgetId(),
                expense.getCategory(),
                expense.getAmount().toString(),
                expense.getDescription(),
                expense.getPaymentMethod(),
                expense.getLocation(),
                expense.getNotes(),
                expense.getExpenseDate(),
                expense.getUpdatedAt()
            ))
            .onComplete(result -> {
                if (result.succeeded()) {
                    logger.info("✅ Dépense mise à jour: {} ({})", expense.getDescription(), expense.getId());
                    future.complete(expense);
                } else {
                    logger.error("❌ Erreur lors de la mise à jour de la dépense: {}", result.cause().getMessage());
                    future.completeExceptionally(result.cause());
                }
            });
    }
    
    /**
     * Trouve une dépense par son ID
     * @param id L'ID de la dépense
     * @return CompletableFuture<Optional<Expense>> La dépense trouvée ou vide
     */
    public CompletableFuture<Optional<Expense>> findById(String id) {
        CompletableFuture<Optional<Expense>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM expenses WHERE id = $1")
            .execute(Tuple.of(id))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    if (rows.size() > 0) {
                        future.complete(Optional.of(mapRowToExpense(rows.iterator().next())));
                    } else {
                        future.complete(Optional.empty());
                    }
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Trouve toutes les dépenses d'un utilisateur
     * @param userId L'ID de l'utilisateur
     * @return CompletableFuture<List<Expense>> Liste des dépenses de l'utilisateur
     */
    public CompletableFuture<List<Expense>> findByUserId(String userId) {
        CompletableFuture<List<Expense>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM expenses WHERE user_id = $1 ORDER BY expense_date DESC")
            .execute(Tuple.of(userId))
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<Expense> expenses = new ArrayList<>();
                    for (Row row : result.result()) {
                        expenses.add(mapRowToExpense(row));
                    }
                    future.complete(expenses);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Trouve toutes les dépenses d'un budget
     * @param budgetId L'ID du budget
     * @return CompletableFuture<List<Expense>> Liste des dépenses du budget
     */
    public CompletableFuture<List<Expense>> findByBudgetId(String budgetId) {
        CompletableFuture<List<Expense>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM expenses WHERE budget_id = $1 ORDER BY expense_date DESC")
            .execute(Tuple.of(budgetId))
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<Expense> expenses = new ArrayList<>();
                    for (Row row : result.result()) {
                        expenses.add(mapRowToExpense(row));
                    }
                    future.complete(expenses);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Trouve les dépenses par catégorie
     * @param category La catégorie à rechercher
     * @return CompletableFuture<List<Expense>> Liste des dépenses de la catégorie
     */
    public CompletableFuture<List<Expense>> findByCategory(String category) {
        CompletableFuture<List<Expense>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM expenses WHERE category = $1 ORDER BY expense_date DESC")
            .execute(Tuple.of(category))
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<Expense> expenses = new ArrayList<>();
                    for (Row row : result.result()) {
                        expenses.add(mapRowToExpense(row));
                    }
                    future.complete(expenses);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Trouve les dépenses dans une période donnée
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return CompletableFuture<List<Expense>> Liste des dépenses dans la période
     */
    public CompletableFuture<List<Expense>> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        CompletableFuture<List<Expense>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM expenses WHERE expense_date BETWEEN $1 AND $2 ORDER BY expense_date DESC")
            .execute(Tuple.of(startDate, endDate))
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<Expense> expenses = new ArrayList<>();
                    for (Row row : result.result()) {
                        expenses.add(mapRowToExpense(row));
                    }
                    future.complete(expenses);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Récupère toutes les dépenses
     * @return CompletableFuture<List<Expense>> Liste de toutes les dépenses
     */
    public CompletableFuture<List<Expense>> findAll() {
        CompletableFuture<List<Expense>> future = new CompletableFuture<>();
        
        jdbcPool.query("SELECT * FROM expenses ORDER BY expense_date DESC")
            .execute()
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<Expense> expenses = new ArrayList<>();
                    for (Row row : result.result()) {
                        expenses.add(mapRowToExpense(row));
                    }
                    future.complete(expenses);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Supprime une dépense par son ID
     * @param id L'ID de la dépense à supprimer
     * @return CompletableFuture<Boolean> true si supprimée, false si non trouvée
     */
    public CompletableFuture<Boolean> deleteById(String id) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("DELETE FROM expenses WHERE id = $1")
            .execute(Tuple.of(id))
            .onComplete(result -> {
                if (result.succeeded()) {
                    boolean deleted = result.result().rowCount() > 0;
                    if (deleted) {
                        logger.info("✅ Dépense supprimée: {}", id);
                    } else {
                        logger.warn("⚠️ Dépense non trouvée pour suppression: {}", id);
                    }
                    future.complete(deleted);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Calcule le total des dépenses pour une catégorie dans un budget
     * @param budgetId L'ID du budget
     * @param category La catégorie
     * @return CompletableFuture<BigDecimal> Le total des dépenses
     */
    public CompletableFuture<BigDecimal> calculateTotalForCategory(String budgetId, String category) {
        CompletableFuture<BigDecimal> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT COALESCE(SUM(amount), 0) as total FROM expenses WHERE budget_id = $1 AND category = $2")
            .execute(Tuple.of(budgetId, category))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    BigDecimal total = new BigDecimal(rows.iterator().next().getString("total"));
                    future.complete(total);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Recherche des dépenses par description (recherche partielle)
     * @param description La description à rechercher
     * @return CompletableFuture<List<Expense>> Liste des dépenses correspondantes
     */
    public CompletableFuture<List<Expense>> findByDescriptionContaining(String description) {
        CompletableFuture<List<Expense>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM expenses WHERE LOWER(description) LIKE LOWER($1) ORDER BY expense_date DESC")
            .execute(Tuple.of("%" + description + "%"))
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<Expense> expenses = new ArrayList<>();
                    for (Row row : result.result()) {
                        expenses.add(mapRowToExpense(row));
                    }
                    future.complete(expenses);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Compte le nombre total de dépenses
     * @return CompletableFuture<Long> Le nombre de dépenses
     */
    public CompletableFuture<Long> count() {
        CompletableFuture<Long> future = new CompletableFuture<>();
        
        jdbcPool.query("SELECT COUNT(*) FROM expenses")
            .execute()
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    long count = rows.iterator().next().getLong(0);
                    future.complete(count);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Convertit une Row de la base de données en objet Expense
     * @param row La ligne de résultat de la base de données
     * @return Expense L'objet Expense correspondant
     */
    private Expense mapRowToExpense(Row row) {
        Expense expense = new Expense();
        expense.setId(row.getString("id"));
        expense.setUserId(row.getString("user_id"));
        expense.setBudgetId(row.getString("budget_id"));
        expense.setCategory(row.getString("category"));
        expense.setAmount(new BigDecimal(row.getString("amount")));
        expense.setDescription(row.getString("description"));
        expense.setPaymentMethod(row.getString("payment_method"));
        expense.setLocation(row.getString("location"));
        expense.setNotes(row.getString("notes"));
        expense.setExpenseDate(row.getLocalDateTime("expense_date"));
        expense.setCreatedAt(row.getLocalDateTime("created_at"));
        expense.setUpdatedAt(row.getLocalDateTime("updated_at"));
        return expense;
    }
}
