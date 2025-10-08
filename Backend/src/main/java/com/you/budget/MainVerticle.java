package com.you.budget;

import com.you.budget.database.DatabaseClient;
import com.you.budget.http.HttpVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe principale de l'application Vert.x
 * C'est le point d'entrée de l'application, comme server.js en Node.js
 * Initialise d'abord la base de données, puis déploie l'API HTTP
 */
public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
    
    /**
     * Point d'entrée principal de l'application
     */
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle())
            .onSuccess(id -> {
                logger.info("Application BudgetPro demarree avec succes!");
            })
            .onFailure(err -> {
                logger.error("Erreur lors du demarrage de l'application", err);
                System.exit(1);
            });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("🚀 Démarrage de l'application Budget...");
        
        // Configuration de la base de données avec variables d'environnement
        JsonObject dbConfig = new JsonObject()
            .put("DB_HOST", System.getenv().getOrDefault("DB_HOST", "localhost"))
            .put("DB_PORT", Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "5432")))
            .put("DB_NAME", System.getenv().getOrDefault("DB_NAME", "you"))
            .put("DB_USER", System.getenv().getOrDefault("DB_USER", "postgres"))
            .put("DB_PASSWORD", System.getenv().getOrDefault("DB_PASSWORD", "postgres"))
            .put("DB_MAX_POOL_SIZE", Integer.parseInt(System.getenv().getOrDefault("DB_MAX_POOL_SIZE", "10")))
            .put("DB_MIN_POOL_SIZE", Integer.parseInt(System.getenv().getOrDefault("DB_MIN_POOL_SIZE", "2")))
            .put("DB_MAX_WAIT_QUEUE_SIZE", Integer.parseInt(System.getenv().getOrDefault("DB_MAX_WAIT_QUEUE_SIZE", "20")));
        
        try {
            // Log de la configuration utilisée (sans le mot de passe)
            logger.info("🔧 Configuration DB: {}:{}@{} (pool: {}-{})", 
                       dbConfig.getString("DB_USER"), 
                       dbConfig.getInteger("DB_PORT"), 
                       dbConfig.getString("DB_HOST"),
                       dbConfig.getInteger("DB_MIN_POOL_SIZE"),
                       dbConfig.getInteger("DB_MAX_POOL_SIZE"));
            
            // 1. Initialiser la base de données
            DatabaseClient.initialize(vertx, dbConfig);
            logger.info("✅ DatabaseClient initialisé");
            
            // 2. Déployer l'API HTTP
            vertx.deployVerticle(new HttpVerticle())
                .onSuccess(httpId -> {
                    logger.info("✅ HttpVerticle déployé (ID: {})", httpId);
                    logger.info("🌐 API disponible sur: http://localhost:8080");
                    logger.info("📊 Base de données: PostgreSQL connectée");
                    startPromise.complete();
                })
                .onFailure(throwable -> {
                    logger.error("❌ Erreur lors du déploiement du HttpVerticle: {}", throwable.getMessage());
                    startPromise.fail(throwable);
                });
                
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'initialisation: {}", e.getMessage());
            startPromise.fail(e);
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        logger.info("🛑 Arrêt de l'application...");
        DatabaseClient.close();
        stopPromise.complete();
    }
}