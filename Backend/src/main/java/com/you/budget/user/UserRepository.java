package com.you.budget.user;

import com.you.budget.database.DatabaseClient;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * UserRepository - Couche d'accès aux données pour les utilisateurs
 * Utilise PostgreSQL avec Vert.x JDBCPool pour les opérations CRUD
 * Obtient le pool de connexions via DatabaseClient
 */
public class UserRepository {
    
    private final JDBCPool jdbcPool;
    
    public UserRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }
    
    /**
     * Sauvegarde un utilisateur
     * @param user L'utilisateur à sauvegarder
     * @return CompletableFuture<User> L'utilisateur sauvegardé
     */
    public CompletableFuture<User> save(User user) {
        CompletableFuture<User> future = new CompletableFuture<>();
        
        // Vérifier si l'utilisateur existe déjà
        jdbcPool.preparedQuery("SELECT id FROM users WHERE id = $1")
            .execute(Tuple.of(user.getId()))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    if (rows.size() > 0) {
                        // Mettre à jour l'utilisateur existant
                        updateUser(user, future);
                    } else {
                        // Insérer un nouvel utilisateur
                        insertUser(user, future);
                    }
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Insère un nouvel utilisateur
     */
    private void insertUser(User user, CompletableFuture<User> future) {
        jdbcPool.preparedQuery("""
            INSERT INTO users (id, email, name, password, created_at, updated_at) 
            VALUES ($1, $2, $3, $4, $5, $6)
            """)
            .execute(Tuple.of(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPassword(),
                user.getCreatedAt(),
                user.getUpdatedAt()
            ))
            .onComplete(result -> {
                if (result.succeeded()) {
                    future.complete(user);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
    }
    
    /**
     * Met à jour un utilisateur existant
     */
    private void updateUser(User user, CompletableFuture<User> future) {
        jdbcPool.preparedQuery("""
            UPDATE users 
            SET email = $2, name = $3, password = $4, updated_at = $5 
            WHERE id = $1
            """)
            .execute(Tuple.of(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPassword(),
                user.getUpdatedAt()
            ))
            .onComplete(result -> {
                if (result.succeeded()) {
                    future.complete(user);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
    }
    
    /**
     * Trouve un utilisateur par son ID
     * @param id L'ID de l'utilisateur
     * @return CompletableFuture<Optional<User>> L'utilisateur trouvé ou vide
     */
    public CompletableFuture<Optional<User>> findById(String id) {
        CompletableFuture<Optional<User>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM users WHERE id = $1")
            .execute(Tuple.of(id))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    if (rows.size() > 0) {
                        future.complete(Optional.of(mapRowToUser(rows.iterator().next())));
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
     * Trouve un utilisateur par son email
     * @param email L'email de l'utilisateur
     * @return CompletableFuture<Optional<User>> L'utilisateur trouvé ou vide
     */
    public CompletableFuture<Optional<User>> findByEmail(String email) {
        CompletableFuture<Optional<User>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM users WHERE email = $1")
            .execute(Tuple.of(email))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    if (rows.size() > 0) {
                        future.complete(Optional.of(mapRowToUser(rows.iterator().next())));
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
     * Trouve un utilisateur par email et mot de passe (pour l'authentification)
     * @param email L'email de l'utilisateur
     * @param password Le mot de passe de l'utilisateur
     * @return CompletableFuture<Optional<User>> L'utilisateur trouvé ou vide
     */
    public CompletableFuture<Optional<User>> findByEmailAndPassword(String email, String password) {
        CompletableFuture<Optional<User>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM users WHERE email = $1 AND password = $2")
            .execute(Tuple.of(email, password))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    if (rows.size() > 0) {
                        future.complete(Optional.of(mapRowToUser(rows.iterator().next())));
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
     * Récupère tous les utilisateurs
     * @return CompletableFuture<List<User>> Liste de tous les utilisateurs
     */
    public CompletableFuture<List<User>> findAll() {
        CompletableFuture<List<User>> future = new CompletableFuture<>();
        
        jdbcPool.query("SELECT * FROM users ORDER BY created_at DESC")
            .execute()
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<User> users = new ArrayList<>();
                    for (Row row : result.result()) {
                        users.add(mapRowToUser(row));
                    }
                    future.complete(users);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Supprime un utilisateur par son ID
     * @param id L'ID de l'utilisateur à supprimer
     * @return CompletableFuture<Boolean> true si supprimé, false si non trouvé
     */
    public CompletableFuture<Boolean> deleteById(String id) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("DELETE FROM users WHERE id = $1")
            .execute(Tuple.of(id))
            .onComplete(result -> {
                if (result.succeeded()) {
                    future.complete(result.result().rowCount() > 0);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Vérifie si un utilisateur existe avec cet email
     * @param email L'email à vérifier
     * @return CompletableFuture<Boolean> true si l'email existe déjà
     */
    public CompletableFuture<Boolean> existsByEmail(String email) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        System.out.println("DEBUG: Vérification existence email: " + email);
        
        jdbcPool.preparedQuery("SELECT COUNT(*) FROM users WHERE email = $1")
            .execute(Tuple.of(email))
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    System.out.println("DEBUG: Nombre de lignes retournées: " + rows.size());
                    
                    if (rows.size() > 0) {
                        Row row = rows.iterator().next();
                        long count = row.getLong(0);
                        System.out.println("DEBUG: Count = " + count);
                        future.complete(count > 0);
                    } else {
                        System.out.println("DEBUG: Aucune ligne retournée");
                        future.complete(false);
                    }
                } else {
                    System.out.println("DEBUG: Erreur dans la requête: " + result.cause().getMessage());
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Compte le nombre total d'utilisateurs
     * @return CompletableFuture<Long> Le nombre d'utilisateurs
     */
    public CompletableFuture<Long> count() {
        CompletableFuture<Long> future = new CompletableFuture<>();
        
        jdbcPool.query("SELECT COUNT(*) FROM users")
            .execute()
            .onComplete(result -> {
                if (result.succeeded()) {
                    RowSet<Row> rows = result.result();
                    if (rows.size() > 0) {
                        long count = rows.iterator().next().getLong(0);
                        future.complete(count);
                    } else {
                        future.complete(0L);
                    }
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Recherche des utilisateurs par nom (recherche partielle)
     * @param name Le nom à rechercher
     * @return CompletableFuture<List<User>> Liste des utilisateurs correspondants
     */
    public CompletableFuture<List<User>> findByNameContaining(String name) {
        CompletableFuture<List<User>> future = new CompletableFuture<>();
        
        jdbcPool.preparedQuery("SELECT * FROM users WHERE LOWER(name) LIKE LOWER($1) ORDER BY name")
            .execute(Tuple.of("%" + name + "%"))
            .onComplete(result -> {
                if (result.succeeded()) {
                    List<User> users = new ArrayList<>();
                    for (Row row : result.result()) {
                        users.add(mapRowToUser(row));
                    }
                    future.complete(users);
                } else {
                    future.completeExceptionally(result.cause());
                }
            });
        
        return future;
    }
    
    /**
     * Convertit une Row de la base de données en objet User
     * @param row La ligne de résultat de la base de données
     * @return User L'objet User correspondant
     */
    private User mapRowToUser(Row row) {
        User user = new User();
        user.setId(row.getString("id"));
        user.setEmail(row.getString("email"));
        user.setName(row.getString("name"));
        user.setPassword(row.getString("password"));
        user.setCreatedAt(row.getLocalDateTime("created_at"));
        user.setUpdatedAt(row.getLocalDateTime("updated_at"));
        return user;
    }
}
