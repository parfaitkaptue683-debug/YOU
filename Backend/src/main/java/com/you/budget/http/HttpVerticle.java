package com.you.budget.http;

import com.you.budget.budget.BudgetController;
import com.you.budget.database.DatabaseClient;
import com.you.budget.expense.ExpenseController;
import com.you.budget.user.UserController;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpVerticle - Gère toutes les routes HTTP de l'API
 * C'est l'équivalent des routes Express.js
 */
public class HttpVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(HttpVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        // Créer le routeur principal
        Router router = Router.router(vertx);
        
        // Configuration CORS
        router.route().handler(CorsHandler.create("*")
            .allowedMethods(java.util.Set.of(
                io.vertx.core.http.HttpMethod.GET,
                io.vertx.core.http.HttpMethod.POST,
                io.vertx.core.http.HttpMethod.PUT,
                io.vertx.core.http.HttpMethod.DELETE,
                io.vertx.core.http.HttpMethod.OPTIONS
            ))
            .allowedHeaders(java.util.Set.of(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Accept",
                "Origin"
            ))
            .allowCredentials(true)
            .maxAgeSeconds(3600));
        
        // Parser le body des requêtes JSON
        router.route().handler(BodyHandler.create());
        
        // Routes de base
        setupBaseRoutes(router);
        
        // Routes API
        UserController.registerRoutes(router);
        BudgetController.registerRoutes(router);
        ExpenseController.registerRoutes(router);
        
        // Démarrer le serveur HTTP
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080, "0.0.0.0")
            .onSuccess(server -> {
                logger.info("HttpVerticle demarre sur le port 8080");
                logger.info("API disponible sur: http://localhost:8080");
                logger.info("API disponible sur: http://127.0.0.1:8080");
                startPromise.complete();
            })
            .onFailure(err -> {
                logger.error("Erreur demarrage HttpVerticle: {}", err.getMessage());
                startPromise.fail(err);
            });
    }
    
    /**
     * Configure les routes de base (test, santé, etc.)
     */
    private void setupBaseRoutes(Router router) {
        // Route de test
        router.get("/").handler(ctx -> {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end("{\"message\": \"Budget API démarrée avec succès!\", \"version\": \"1.0.0\"}");
        });
        
        // Route de santé
        router.get("/health").handler(ctx -> {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end("{\"status\": \"OK\", \"message\": \"API fonctionnelle\", \"timestamp\": \"" + 
                     java.time.Instant.now() + "\"}");
        });
        
        // Route de santé de la base de données
        router.get("/health/db").handler(ctx -> {
            if (DatabaseClient.isInitialized()) {
                JsonObject dbInfo = DatabaseClient.getConfig();
                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(dbInfo.encodePrettily());
            } else {
                ctx.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"status\": \"ERROR\", \"message\": \"DatabaseClient non initialisé\"}");
            }
        });
        
        // Route d'information
        router.get("/info").handler(ctx -> {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end("{\"name\": \"Budget API\", \"description\": \"API pour la gestion de budget\", " +
                     "\"endpoints\": [\"/api/users\", \"/api/budgets\", \"/api/expenses\"]}");
        });
    }
    
}
