package com.you.budget.budget;

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
 * BudgetRepository - Couche d'accès aux données pour les budgets
 * Utilise PostgreSQL avec Vert.x JDBCPool pour les opérations CRUD
 * Obtient le pool de connexions via DatabaseClient
 */
public class BudgetRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(BudgetRepository.class);
    private final JDBCPool jdbcPool;
    
    public BudgetRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }
    
    /**
     * Sauvegarde un budget
     * @param budget Le budget à sauvegarder
     * @return CompletableFuture<Budget> Le budget sauvegardé
     */
    public CompletableFuture<Budget> save(Budget budget) {
        CompletableFuture<Budget> future = new CompletableFuture<>();
        
        // Vérifier si le budget existe déjà
        jdbcPool.preparedQuery("SELECT id FROM budgets WHERE id = $1")
            .execute(Tuple.of(budget.getId()))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    if (rows.size() > 0) {
                        // Mettre à jour le budget existant
                        updateBudget(budget, future);
                    } else {
                        // Insérer un nouveau budget
                        insertBudget(budget, future);
                    }
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Insère un nouveau budget
     */
    private void insertBudget(Budget budget, CompletableFuture<Budget> future) {
        jdbcPool.preparedQuery("""
            INSERT INTO budgets (id, user_id, total_income, loisirs_budget, essentiels_budget, 
                               epargne_budget, loisirs_spent, essentiels_spent, epargne_spent, 
                               month_year, created_at, updated_at) 
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
            """)
            .execute(Tuple.of(
                budget.getId(),
                budget.getUserId(),
                budget.getTotalIncome().toString(),
                budget.getLoisirsBudget().toString(),
                budget.getEssentielsBudget().toString(),
                budget.getEpargneBudget().toString(),
                budget.getLoisirsSpent().toString(),
                budget.getEssentielsSpent().toString(),
                budget.getEpargneSpent().toString(),
                budget.getMonthYear(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
            ))
            .onComplete(result -> {
                if (result.succeeded()) {
                    logger.info("✅ Budget inséré: {} ({})", budget.getUserId(), budget.getId());
                    future.complete(budget);
                } else {
                    logger.error("❌ Erreur lors de l'insertion du budget: {}", result.cause().getMessage());
                    future.completeExceptionally(result.cause());
                }
            });
    }
    
    /**
     * Met à jour un budget existant
     */
    private void updateBudget(Budget budget, CompletableFuture<Budget> future) {
        jdbcPool.preparedQuery("""
            UPDATE budgets 
            SET user_id = $2, total_income = $3, loisirs_budget = $4, 
                essentiels_budget = $5, epargne_budget = $6, loisirs_spent = $7, 
                essentiels_spent = $8, epargne_spent = $9, month_year = $10, updated_at = $11 
            WHERE id = $1
            """)
            .execute(Tuple.of(
                budget.getId(),
                budget.getUserId(),
                budget.getTotalIncome().toString(),
                budget.getLoisirsBudget().toString(),
                budget.getEssentielsBudget().toString(),
                budget.getEpargneBudget().toString(),
                budget.getLoisirsSpent().toString(),
                budget.getEssentielsSpent().toString(),
                budget.getEpargneSpent().toString(),
                budget.getMonthYear(),
                budget.getUpdatedAt()
            ))
            .onComplete(result -> {
                if (result.succeeded()) {
                    logger.info("✅ Budget mis à jour: {} ({})", budget.getUserId(), budget.getId());
                    future.complete(budget);
                } else {
                    logger.error("❌ Erreur lors de la mise à jour du budget: {}", result.cause().getMessage());
                    future.completeExceptionally(result.cause());
                }
            });
    }
    
    /**
     * Trouve un budget par son ID
     * @param id L'ID du budget
     * @return CompletableFuture<Optional<Budget>> Le budget trouvé ou vide
     */
    public CompletableFuture<Optional<Budget>> findById(String id) {
        CompletableFuture<Optional<Budget>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM budgets WHERE id = $1")
            .execute(Tuple.of(id))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    if (rows.size() > 0) {
                        future.complete(Optional.of(mapRowToBudget(rows.iterator().next())));
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
     * Trouve un budget par utilisateur et mois/année
     * @param userId L'ID de l'utilisateur
     * @param monthYear Le mois/année du budget
     * @return CompletableFuture<Optional<Budget>> Le budget trouvé ou vide
     */
    public CompletableFuture<Optional<Budget>> findByUserIdAndMonth(String userId, LocalDateTime monthYear) {
        CompletableFuture<Optional<Budget>> future = new CompletableFuture<>();
        
        // Extraire l'année et le mois pour la comparaison
        int year = monthYear.getYear();
        int month = monthYear.getMonthValue();
        
        jdbcPool.preparedQuery("""
            SELECT * FROM budgets 
            WHERE user_id = $1 
            AND EXTRACT(YEAR FROM month_year) = $2 
            AND EXTRACT(MONTH FROM month_year) = $3
            """)
            .execute(Tuple.of(userId, year, month))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    if (rows.size() > 0) {
                        future.complete(Optional.of(mapRowToBudget(rows.iterator().next())));
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
     * Trouve tous les budgets d'un utilisateur
     * @param userId L'ID de l'utilisateur
     * @return CompletableFuture<List<Budget>> Liste des budgets de l'utilisateur
     */
    public CompletableFuture<List<Budget>> findByUserId(String userId) {
        CompletableFuture<List<Budget>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM budgets WHERE user_id = $1 ORDER BY created_at DESC")
            .execute(Tuple.of(userId))
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<Budget> budgets = new ArrayList<>();
                    for (Row row : result.result()) {
                        budgets.add(mapRowToBudget(row));
                    }
                    future.complete(budgets);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Trouve les budgets par catégorie
     * @param category La catégorie à rechercher
     * @return CompletableFuture<List<Budget>> Liste des budgets de la catégorie
     */
    public CompletableFuture<List<Budget>> findByCategory(String category) {
        CompletableFuture<List<Budget>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM budgets WHERE category = $1 ORDER BY created_at DESC")
            .execute(Tuple.of(category))
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<Budget> budgets = new ArrayList<>();
                    for (Row row : result.result()) {
                        budgets.add(mapRowToBudget(row));
                    }
                    future.complete(budgets);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Récupère tous les budgets
     * @return CompletableFuture<List<Budget>> Liste de tous les budgets
     */
    public CompletableFuture<List<Budget>> findAll() {
        CompletableFuture<List<Budget>> future = new CompletableFuture<>();
        
        jdbcPool.query("SELECT * FROM budgets ORDER BY created_at DESC")
            .execute()
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<Budget> budgets = new ArrayList<>();
                    for (Row row : result.result()) {
                        budgets.add(mapRowToBudget(row));
                    }
                    future.complete(budgets);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Supprime un budget par son ID
     * @param id L'ID du budget à supprimer
     * @return CompletableFuture<Boolean> true si supprimé, false si non trouvé
     */
    public CompletableFuture<Boolean> deleteById(String id) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("DELETE FROM budgets WHERE id = $1")
            .execute(Tuple.of(id))
            .onComplete(result -> {
                if (result.succeeded()) {
                    boolean deleted = result.result().rowCount() > 0;
                    if (deleted) {
                        logger.info("✅ Budget supprimé: {}", id);
                    } else {
                        logger.warn("⚠️ Budget non trouvé pour suppression: {}", id);
                    }
                    future.complete(deleted);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Met à jour le montant dépensé d'un budget
     * @param budgetId L'ID du budget
     * @param spentAmount Le nouveau montant dépensé
     * @return CompletableFuture<Optional<Budget>> Le budget mis à jour ou vide si non trouvé
     */
    public CompletableFuture<Optional<Budget>> updateSpentAmount(String budgetId, BigDecimal spentAmount) {
        CompletableFuture<Optional<Budget>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("""
            UPDATE budgets 
            SET spent_amount = $2, remaining_amount = total_amount - $2, updated_at = NOW() 
            WHERE id = $1
            """)
            .execute(Tuple.of(budgetId, spentAmount.toString()))
            .onComplete(result -> {
                if (result.succeeded()) {
                    if (result.result().rowCount() > 0) {
                        // Récupérer le budget mis à jour
                        findById(budgetId)
                            .thenAccept(budgetOpt -> future.complete(budgetOpt))
                            .exceptionally(throwable -> {
                                future.completeExceptionally(throwable);
                                return null;
                            });
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
     * Recherche des budgets par nom (recherche partielle)
     * @param name Le nom à rechercher
     * @return CompletableFuture<List<Budget>> Liste des budgets correspondants
     */
    public CompletableFuture<List<Budget>> findByNameContaining(String name) {
        CompletableFuture<List<Budget>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM budgets WHERE LOWER(name) LIKE LOWER($1) ORDER BY name")
            .execute(Tuple.of("%" + name + "%"))
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<Budget> budgets = new ArrayList<>();
                    for (Row row : result.result()) {
                        budgets.add(mapRowToBudget(row));
                    }
                    future.complete(budgets);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Compte le nombre total de budgets
     * @return CompletableFuture<Long> Le nombre de budgets
     */
    public CompletableFuture<Long> count() {
        CompletableFuture<Long> future = new CompletableFuture<>();
        
        jdbcPool.query("SELECT COUNT(*) FROM budgets")
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
     * Convertit une Row de la base de données en objet Budget
     * @param row La ligne de résultat de la base de données
     * @return Budget L'objet Budget correspondant
     */
    private Budget mapRowToBudget(Row row) {
        Budget budget = new Budget();
        budget.setId(row.getString("id"));
        budget.setUserId(row.getString("user_id"));
        budget.setName(row.getString("name"));
        budget.setDescription(row.getString("description"));
        budget.setTotalAmount(new BigDecimal(row.getString("total_amount")));
        budget.setSpentAmount(new BigDecimal(row.getString("spent_amount")));
        budget.setRemainingAmount(new BigDecimal(row.getString("remaining_amount")));
        budget.setCategory(row.getString("category"));
        budget.setStartDate(row.getLocalDateTime("start_date"));
        budget.setEndDate(row.getLocalDateTime("end_date"));
        budget.setCreatedAt(row.getLocalDateTime("created_at"));
        budget.setUpdatedAt(row.getLocalDateTime("updated_at"));
        return budget;
    }
}
