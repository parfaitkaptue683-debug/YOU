package com.you.budget.http;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * HttpController - Contrôleur principal pour gérer les réponses HTTP
 * Centralise la logique de gestion des réponses et des erreurs
 */
public class HttpController {
    
    /**
     * Envoie une réponse JSON réussie
     */
    public static void sendSuccess(RoutingContext ctx, String message, Object data) {
        JsonObject response = new JsonObject()
            .put("success", true)
            .put("message", message)
            .put("data", data);
        
        ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(response.encode());
    }
    
    /**
     * Envoie une réponse JSON réussie avec un message simple
     */
    public static void sendSuccess(RoutingContext ctx, String message) {
        sendSuccess(ctx, message, null);
    }
    
    /**
     * Envoie une réponse JSON de création réussie
     */
    public static void sendCreated(RoutingContext ctx, String message, Object data) {
        JsonObject response = new JsonObject()
            .put("success", true)
            .put("message", message)
            .put("data", data);
        
        ctx.response()
            .setStatusCode(201)
            .putHeader("Content-Type", "application/json")
            .end(response.encode());
    }
    
    /**
     * Envoie une réponse d'erreur
     */
    public static void sendError(RoutingContext ctx, int statusCode, String message) {
        JsonObject response = new JsonObject()
            .put("success", false)
            .put("message", message)
            .put("error", true);
        
        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("Content-Type", "application/json")
            .end(response.encode());
    }
    
    /**
     * Envoie une erreur 400 (Bad Request)
     */
    public static void sendBadRequest(RoutingContext ctx, String message) {
        sendError(ctx, 400, message);
    }
    
    /**
     * Envoie une erreur 404 (Not Found)
     */
    public static void sendNotFound(RoutingContext ctx, String message) {
        sendError(ctx, 404, message);
    }
    
    /**
     * Envoie une erreur 500 (Internal Server Error)
     */
    public static void sendInternalError(RoutingContext ctx, String message) {
        sendError(ctx, 500, message);
    }
    
    /**
     * Valide que le body de la requête contient du JSON
     */
    public static boolean validateJsonBody(RoutingContext ctx) {
        try {
            JsonObject body = ctx.body().asJsonObject();
            if (body == null) {
                sendBadRequest(ctx, "Body JSON requis");
                return false;
            }
            return true;
        } catch (Exception e) {
            sendBadRequest(ctx, "Format JSON invalide");
            return false;
        }
    }
    
    /**
     * Récupère un paramètre de route et valide qu'il existe
     */
    public static String getPathParam(RoutingContext ctx, String paramName) {
        String param = ctx.pathParam(paramName);
        if (param == null || param.isEmpty()) {
            sendBadRequest(ctx, "Paramètre " + paramName + " requis");
            return null;
        }
        return param;
    }
    
    /**
     * Récupère le body JSON de la requête
     */
    public static JsonObject getJsonBody(RoutingContext ctx) {
        return ctx.body().asJsonObject();
    }
    
    /**
     * Log une requête entrante
     */
    public static void logRequest(RoutingContext ctx) {
        String method = ctx.request().method().name();
        String path = ctx.request().path();
        String clientIP = ctx.request().remoteAddress().host();
        
        System.out.println("📥 " + method + " " + path + " - IP: " + clientIP);
    }
    
    /**
     * Log une réponse sortante
     */
    public static void logResponse(RoutingContext ctx, int statusCode) {
        String method = ctx.request().method().name();
        String path = ctx.request().path();
        
        String statusEmoji = statusCode >= 200 && statusCode < 300 ? "✅" : 
                           statusCode >= 400 && statusCode < 500 ? "⚠️" : "❌";
        
        System.out.println("📤 " + statusEmoji + " " + method + " " + path + " - " + statusCode);
    }
}
