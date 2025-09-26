package com.you.budget.user;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserService - Couche de logique métier pour les utilisateurs
 * Contient la logique d'authentification, validation, et gestion des utilisateurs
 * Utilise UserRepository pour les opérations de base de données
 */
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    
    public UserService() {
        this.userRepository = new UserRepository();
        logger.info("✅ UserService initialisé avec UserRepository");
    }
    
    /**
     * Crée un nouvel utilisateur
     * @param email L'email de l'utilisateur
     * @param name Le nom de l'utilisateur
     * @param password Le mot de passe de l'utilisateur
     * @return CompletableFuture<User> L'utilisateur créé
     * @throws IllegalArgumentException Si les données sont invalides
     */
    public CompletableFuture<User> createUser(String email, String name, String password) {
        logger.info("🔧 Création d'un nouvel utilisateur: {}", email);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validation des données
                validateUserData(email, name, password);
                
                // Vérifier si l'utilisateur existe déjà
                return userRepository.existsByEmail(email)
                    .thenCompose(exists -> {
                        if (exists) {
                            logger.warn("⚠️ Utilisateur avec email {} existe déjà", email);
                            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
                        }
                        
                        // Créer le nouvel utilisateur
                        User newUser = new User(email, name, password);
                        logger.info("👤 Nouvel utilisateur créé: {} ({})", newUser.getName(), newUser.getId());
                        return userRepository.save(newUser);
                    })
                    .join();
            } catch (Exception e) {
                logger.error("❌ Erreur lors de la création de l'utilisateur {}: {}", email, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Authentifie un utilisateur
     * @param email L'email de l'utilisateur
     * @param password Le mot de passe de l'utilisateur
     * @return CompletableFuture<Optional<User>> L'utilisateur authentifié ou vide
     */
    public CompletableFuture<Optional<User>> authenticateUser(String email, String password) {
        logger.info("🔐 Tentative d'authentification pour: {}", email);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validation des paramètres
                if (email == null || email.trim().isEmpty()) {
                    throw new IllegalArgumentException("L'email est requis");
                }
                if (password == null || password.trim().isEmpty()) {
                    throw new IllegalArgumentException("Le mot de passe est requis");
                }
                
                return userRepository.findByEmailAndPassword(email, password)
                    .thenApply(userOpt -> {
                        if (userOpt.isPresent()) {
                            logger.info("✅ Authentification réussie pour: {}", email);
                        } else {
                            logger.warn("❌ Échec d'authentification pour: {}", email);
                        }
                        return userOpt;
                    })
                    .join();
            } catch (Exception e) {
                logger.error("❌ Erreur lors de l'authentification de {}: {}", email, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Récupère un utilisateur par son ID
     * @param id L'ID de l'utilisateur
     * @return CompletableFuture<Optional<User>> L'utilisateur trouvé ou vide
     */
    public CompletableFuture<Optional<User>> getUserById(String id) {
        logger.info("🔍 Recherche d'utilisateur par ID: {}", id);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (id == null || id.trim().isEmpty()) {
                    throw new IllegalArgumentException("L'ID est requis");
                }
                
                return userRepository.findById(id)
                    .thenApply(userOpt -> {
                        if (userOpt.isPresent()) {
                            logger.info("✅ Utilisateur trouvé: {} ({})", userOpt.get().getName(), id);
                        } else {
                            logger.warn("⚠️ Utilisateur non trouvé: {}", id);
                        }
                        return userOpt;
                    })
                    .join();
            } catch (Exception e) {
                logger.error("❌ Erreur lors de la recherche d'utilisateur {}: {}", id, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Récupère un utilisateur par son email
     * @param email L'email de l'utilisateur
     * @return CompletableFuture<Optional<User>> L'utilisateur trouvé ou vide
     */
    public CompletableFuture<Optional<User>> getUserByEmail(String email) {
        logger.info("🔍 Recherche d'utilisateur par email: {}", email);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (email == null || email.trim().isEmpty()) {
                    throw new IllegalArgumentException("L'email est requis");
                }
                
                return userRepository.findByEmail(email)
                    .thenApply(userOpt -> {
                        if (userOpt.isPresent()) {
                            logger.info("✅ Utilisateur trouvé: {} ({})", userOpt.get().getName(), email);
                        } else {
                            logger.warn("⚠️ Utilisateur non trouvé: {}", email);
                        }
                        return userOpt;
                    })
                    .join();
            } catch (Exception e) {
                logger.error("❌ Erreur lors de la recherche d'utilisateur {}: {}", email, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Récupère tous les utilisateurs
     * @return CompletableFuture<List<User>> Liste de tous les utilisateurs
     */
    public CompletableFuture<List<User>> getAllUsers() {
        logger.info("📋 Récupération de tous les utilisateurs");
        
        return userRepository.findAll()
            .thenApply(users -> {
                logger.info("✅ {} utilisateurs récupérés", users.size());
                return users;
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors de la récupération des utilisateurs: {}", throwable.getMessage());
                throw new RuntimeException("Erreur lors de la récupération des utilisateurs", throwable);
            });
    }
    
    /**
     * Met à jour un utilisateur
     * @param id L'ID de l'utilisateur à mettre à jour
     * @param email Le nouvel email (optionnel)
     * @param name Le nouveau nom (optionnel)
     * @param password Le nouveau mot de passe (optionnel)
     * @return CompletableFuture<Optional<User>> L'utilisateur mis à jour ou vide si non trouvé
     */
    public CompletableFuture<Optional<User>> updateUser(String id, String email, String name, String password) {
        logger.info("✏️ Mise à jour de l'utilisateur: {}", id);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (id == null || id.trim().isEmpty()) {
                    throw new IllegalArgumentException("L'ID est requis");
                }
                
                return userRepository.findById(id)
                    .thenCompose(userOpt -> {
                        if (userOpt.isEmpty()) {
                            logger.warn("⚠️ Utilisateur non trouvé pour mise à jour: {}", id);
                            return CompletableFuture.completedFuture(Optional.empty());
                        }
                        
                        User user = userOpt.get();
                        logger.info("👤 Mise à jour de: {} ({})", user.getName(), user.getEmail());
                        
                        // Mettre à jour les champs fournis
                        if (email != null && !email.trim().isEmpty()) {
                            // Vérifier si le nouvel email n'est pas déjà utilisé par un autre utilisateur
                            return userRepository.existsByEmail(email)
                                .thenCompose(exists -> {
                                    if (exists && !user.getEmail().equals(email)) {
                                        logger.warn("⚠️ Email {} déjà utilisé par un autre utilisateur", email);
                                        throw new IllegalArgumentException("Cet email est déjà utilisé par un autre utilisateur");
                                    }
                                    user.setEmail(email);
                                    return CompletableFuture.completedFuture(null);
                                })
                                .thenCompose(v -> {
                                    if (name != null && !name.trim().isEmpty()) {
                                        user.setName(name);
                                    }
                                    if (password != null && !password.trim().isEmpty()) {
                                        user.setPassword(password);
                                    }
                                    
                                    user.touch(); // Mettre à jour la date de modification
                                    logger.info("✅ Utilisateur mis à jour: {} ({})", user.getName(), user.getEmail());
                                    return userRepository.save(user);
                                })
                                .thenApply(updatedUser -> Optional.of(updatedUser));
                        } else {
                            // Pas de changement d'email, mise à jour directe
                            if (name != null && !name.trim().isEmpty()) {
                                user.setName(name);
                            }
                            if (password != null && !password.trim().isEmpty()) {
                                user.setPassword(password);
                            }
                            
                            user.touch();
                            logger.info("✅ Utilisateur mis à jour: {} ({})", user.getName(), user.getEmail());
                            return userRepository.save(user)
                                .thenApply(updatedUser -> Optional.of(updatedUser));
                        }
                    })
                    .join();
            } catch (Exception e) {
                logger.error("❌ Erreur lors de la mise à jour de l'utilisateur {}: {}", id, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Supprime un utilisateur
     * @param id L'ID de l'utilisateur à supprimer
     * @return CompletableFuture<Boolean> true si supprimé, false si non trouvé
     */
    public CompletableFuture<Boolean> deleteUser(String id) {
        logger.info("🗑️ Suppression de l'utilisateur: {}", id);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (id == null || id.trim().isEmpty()) {
                    throw new IllegalArgumentException("L'ID est requis");
                }
                
                return userRepository.deleteById(id)
                    .thenApply(deleted -> {
                        if (deleted) {
                            logger.info("✅ Utilisateur supprimé: {}", id);
                        } else {
                            logger.warn("⚠️ Utilisateur non trouvé pour suppression: {}", id);
                        }
                        return deleted;
                    })
                    .join();
            } catch (Exception e) {
                logger.error("❌ Erreur lors de la suppression de l'utilisateur {}: {}", id, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Recherche des utilisateurs par nom
     * @param name Le nom à rechercher
     * @return CompletableFuture<List<User>> Liste des utilisateurs correspondants
     */
    public CompletableFuture<List<User>> searchUsersByName(String name) {
        logger.info("🔍 Recherche d'utilisateurs par nom: {}", name);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (name == null || name.trim().isEmpty()) {
                    throw new IllegalArgumentException("Le nom de recherche est requis");
                }
                
                return userRepository.findByNameContaining(name)
                    .thenApply(users -> {
                        logger.info("✅ {} utilisateurs trouvés pour '{}'", users.size(), name);
                        return users;
                    })
                    .join();
            } catch (Exception e) {
                logger.error("❌ Erreur lors de la recherche d'utilisateurs '{}': {}", name, e.getMessage());
                throw e;
            }
        });
    }
    
    /**
     * Compte le nombre total d'utilisateurs
     * @return CompletableFuture<Long> Le nombre d'utilisateurs
     */
    public CompletableFuture<Long> getUserCount() {
        logger.info("📊 Comptage du nombre d'utilisateurs");
        
        return userRepository.count()
            .thenApply(count -> {
                logger.info("✅ Nombre d'utilisateurs: {}", count);
                return count;
            })
            .exceptionally(throwable -> {
                logger.error("❌ Erreur lors du comptage des utilisateurs: {}", throwable.getMessage());
                throw new RuntimeException("Erreur lors du comptage des utilisateurs", throwable);
            });
    }
    
    /**
     * Valide les données d'un utilisateur
     * @param email L'email à valider
     * @param name Le nom à valider
     * @param password Le mot de passe à valider
     * @throws IllegalArgumentException Si les données sont invalides
     */
    private void validateUserData(String email, String name, String password) {
        logger.debug("🔍 Validation des données utilisateur: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est requis");
        }
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            throw new IllegalArgumentException("Format d'email invalide");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom est requis");
        }
        if (name.length() < 2) {
            throw new IllegalArgumentException("Le nom doit contenir au moins 2 caractères");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est requis");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }
        
        logger.debug("✅ Données utilisateur valides: {}", email);
    }
}
