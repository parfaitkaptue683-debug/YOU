package com.you.budget.database;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DatabaseClient - Gestionnaire centralisé de la connexion PostgreSQL
 * Utilise JDBCPool pour une compatibilité universelle avec différentes bases de données
 * Singleton pattern pour réutiliser le pool de connexions
 */
public class DatabaseClient {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseClient.class);
    private static JDBCPool jdbcPool;
    private static boolean initialized = false;

    private DatabaseClient() { // Singleton - constructeur privé
    }

    /**
     * Initialise le pool de connexions PostgreSQL
     * @param vertx Instance Vert.x
     * @param config Configuration de la base de données
     */
    public static void initialize(Vertx vertx, JsonObject config) {
        if (jdbcPool != null) {
            logger.warn("DatabaseClient est déjà initialisé!");
            return;
        }

        try {
            logger.info("🔍 Configuration: {}", config.encodePrettily());

            // Valider la configuration
            validateConfig(config);

            // Configuration JDBC pour PostgreSQL
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
                config.getString("DB_HOST"),
                config.getInteger("DB_PORT"),
                config.getString("DB_NAME"));

            logger.info("🔗 URL JDBC: {}", jdbcUrl);

            // Configuration du pool de connexions
            PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(config.getInteger("DB_MAX_POOL_SIZE", 10))
                .setMaxWaitQueueSize(config.getInteger("DB_MAX_WAIT_QUEUE_SIZE", 20))
                .setName("budget-app-pool");

            // Créer le pool JDBC
            jdbcPool = JDBCPool.pool(vertx,
                new io.vertx.jdbcclient.JDBCConnectOptions()
                    .setJdbcUrl(jdbcUrl)
                    .setUser(config.getString("DB_USER"))
                    .setPassword(config.getString("DB_PASSWORD")),
                poolOptions);

            logger.info("Pool JDBC PostgreSQL initialise avec succes!");
            logger.info("Configuration du pool:");
            logger.info("   - Taille max: {}", poolOptions.getMaxSize());
            logger.info("   - Queue max: {}", poolOptions.getMaxWaitQueueSize());

            // Tester la connexion
            testDatabaseConnection();
            
            initialized = true;
        } catch (Exception e) {
            logger.error("❌ Échec de l'initialisation DatabaseClient: {}", e.getMessage());
            throw new RuntimeException("Échec de l'initialisation de la base de données", e);
        }
    }

    /**
     * Obtient l'instance du pool de connexions
     * @return JDBCPool Le pool de connexions JDBC
     * @throws IllegalStateException Si le client n'est pas initialisé
     */
    public static JDBCPool getInstance() {
        if (jdbcPool == null) {
            logger.error("❌ DatabaseClient non initialisé! Appelez initialize() d'abord.");
            throw new IllegalStateException("❌ DatabaseClient non initialisé! Appelez initialize() d'abord.");
        }
        return jdbcPool;
    }

    /**
     * Vérifie si le client est initialisé
     * @return boolean true si initialisé
     */
    public static boolean isInitialized() {
        return initialized && jdbcPool != null;
    }

    /**
     * Valide la configuration de la base de données
     * @param config Configuration à valider
     * @throws IllegalArgumentException Si la configuration est invalide
     */
    private static void validateConfig(JsonObject config) {
        logger.info("🔍 Vérification de la configuration: {}", config.encodePrettily());

        if (!config.containsKey("DB_HOST") || !config.containsKey("DB_PORT")
                || !config.containsKey("DB_NAME") || !config.containsKey("DB_USER")
                || !config.containsKey("DB_PASSWORD")) {
            logger.error("❌ Paramètres de configuration de base de données manquants!");
            throw new IllegalArgumentException("❌ Paramètres de configuration de base de données manquants!");
        }
    }

    /**
     * Teste la connexion à la base de données
     */
    private static void testDatabaseConnection() {
        jdbcPool.query("SELECT NOW() as current_time, version() as db_version")
            .execute()
            .onSuccess(result -> {
                if (result.size() > 0) {
                    var row = result.iterator().next();
                    logger.info("🕐 Heure actuelle DB: {}", row.getString("current_time"));
                    logger.info("📋 Version PostgreSQL: {}", row.getString("db_version"));
                    logger.info("✅ Test de connexion à la base de données réussi!");
                }
            })
            .onFailure(err -> {
                logger.error("❌ Test de connexion à la base de données échoué: {}", err.getMessage());
            });
    }

    /**
     * Ferme le pool de connexions
     */
    public static void close() {
        if (jdbcPool != null) {
            logger.info("🔌 Fermeture du pool JDBC PostgreSQL...");
            jdbcPool.close()
                .onSuccess(v -> {
                    logger.info("✅ Pool JDBC fermé");
                    jdbcPool = null;
                    initialized = false;
                })
                .onFailure(err -> {
                    logger.error("❌ Erreur lors de la fermeture du pool: {}", err.getMessage());
                });
        }
    }

    /**
     * Obtient les informations de configuration
     * @return JsonObject Informations de configuration
     */
    public static JsonObject getConfig() {
        return new JsonObject()
            .put("initialized", initialized)
            .put("pool_created", jdbcPool != null);
    }
}