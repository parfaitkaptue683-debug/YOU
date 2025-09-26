package com.you.budget;

import com.you.budget.database.DatabaseClient;
import com.you.budget.http.HttpVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
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

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("🚀 Démarrage de l'application Budget...");
        
        // Configuration de la base de données
        JsonObject dbConfig = new JsonObject()
            .put("DB_HOST", "localhost")
            .put("DB_PORT", 5432)
            .put("DB_NAME", "you")
            .put("DB_USER", "postgres")
            .put("DB_PASSWORD", "chooseyourhistory")
            .put("DB_MAX_POOL_SIZE", 10)
            .put("DB_MIN_POOL_SIZE", 2)
            .put("DB_MAX_WAIT_QUEUE_SIZE", 20);
        
        try {
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