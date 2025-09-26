/**
 * Auth.js - Gestion de l'authentification utilisateur
 * Version 1.0 - Interface d'authentification pour BudgetPro
 */

// Configuration de l'API d'authentification
const AUTH_CONFIG = {
    baseUrl: 'http://localhost:8080',
    endpoints: {
        users: '/api/users'
    }
};

// Classe pour gérer l'authentification
class AuthAPI {
    
    /**
     * Effectue une requête HTTP vers l'API d'authentification
     */
    static async request(endpoint, method = 'GET', data = null) {
        const url = `${AUTH_CONFIG.baseUrl}${endpoint}`;
        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/json',
            }
        };
        
        if (data) {
            options.body = JSON.stringify(data);
        }
        
        try {
            const response = await fetch(url, options);
            const result = await response.json();
            
            if (!response.ok) {
                throw new Error(result.message || `Erreur HTTP ${response.status}`);
            }
            
            return result;
        } catch (error) {
            console.error('Erreur Auth API:', error);
            throw error;
        }
    }
    
    // === UTILISATEURS ===
    
    /**
     * Inscription d'un nouvel utilisateur
     */
    static async registerUser(email, name, password) {
        return this.request(`${AUTH_CONFIG.endpoints.users}/register`, 'POST', {
            email, name, password
        });
    }
    
    /**
     * Connexion d'un utilisateur
     */
    static async loginUser(email, password) {
        return this.request(`${AUTH_CONFIG.endpoints.users}/login`, 'POST', {
            email, password
        });
    }
    
    /**
     * Récupération du profil utilisateur
     */
    static async getUserProfile(userId) {
        return this.request(`${AUTH_CONFIG.endpoints.users}/profile/${userId}`);
    }
    
    /**
     * Mise à jour du profil utilisateur
     */
    static async updateUserProfile(userId, userData) {
        return this.request(`${AUTH_CONFIG.endpoints.users}/${userId}`, 'PUT', userData);
    }
    
    /**
     * Suppression du compte utilisateur
     */
    static async deleteUser(userId) {
        return this.request(`${AUTH_CONFIG.endpoints.users}/${userId}`, 'DELETE');
    }
}

// Gestionnaire d'authentification global
class AuthManager {
    constructor() {
        this.currentUser = null;
        this.isAuthenticated = false;
        this.init();
    }
    
    /**
     * Initialise le gestionnaire d'authentification
     */
    init() {
        // Vérifier s'il y a une session sauvegardée
        this.loadSession();
    }
    
    /**
     * Charge la session depuis le localStorage
     */
    loadSession() {
        try {
            const savedUser = localStorage.getItem('budget_user');
            if (savedUser) {
                this.currentUser = JSON.parse(savedUser);
                this.isAuthenticated = true;
                console.log('✅ Session utilisateur restaurée:', this.currentUser.name);
            }
        } catch (error) {
            console.warn('⚠️ Impossible de restaurer la session:', error);
            this.clearSession();
        }
    }
    
    /**
     * Sauvegarde la session dans le localStorage
     */
    saveSession(user) {
        try {
            localStorage.setItem('budget_user', JSON.stringify(user));
            this.currentUser = user;
            this.isAuthenticated = true;
            console.log('✅ Session utilisateur sauvegardée');
        } catch (error) {
            console.error('❌ Impossible de sauvegarder la session:', error);
        }
    }
    
    /**
     * Efface la session
     */
    clearSession() {
        localStorage.removeItem('budget_user');
        this.currentUser = null;
        this.isAuthenticated = false;
        console.log('✅ Session utilisateur effacée');
    }
    
    /**
     * Inscription d'un nouvel utilisateur
     */
    async register(email, name, password) {
        try {
            const response = await AuthAPI.registerUser(email, name, password);
            
            if (response.success && response.data) {
                this.saveSession(response.data);
                return { success: true, user: response.data };
            }
            
            return { success: false, error: 'Erreur lors de l\'inscription' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }
    
    /**
     * Connexion d'un utilisateur
     */
    async login(email, password) {
        try {
            const response = await AuthAPI.loginUser(email, password);
            
            if (response.success && response.data) {
                this.saveSession(response.data);
                return { success: true, user: response.data };
            }
            
            return { success: false, error: 'Email ou mot de passe incorrect' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }
    
    /**
     * Déconnexion de l'utilisateur
     */
    logout() {
        this.clearSession();
        // Rediriger vers la page de connexion ou recharger
        window.location.reload();
    }
    
    /**
     * Vérifie si l'utilisateur est connecté
     */
    isLoggedIn() {
        return this.isAuthenticated && this.currentUser !== null;
    }
    
    /**
     * Récupère l'utilisateur actuel
     */
    getCurrentUser() {
        return this.currentUser;
    }
    
    /**
     * Récupère l'ID de l'utilisateur actuel
     */
    getCurrentUserId() {
        return this.currentUser ? this.currentUser.id : null;
    }
}

// Instance globale du gestionnaire d'authentification
window.authManager = new AuthManager();

// Fonctions globales pour l'authentification
window.registerUser = async function(email, name, password) {
    return await window.authManager.register(email, name, password);
};

window.loginUser = async function(email, password) {
    return await window.authManager.login(email, password);
};

window.logoutUser = function() {
    window.authManager.logout();
};

window.isUserLoggedIn = function() {
    return window.authManager.isLoggedIn();
};

window.getCurrentUser = function() {
    return window.authManager.getCurrentUser();
};

window.getCurrentUserId = function() {
    return window.authManager.getCurrentUserId();
};
