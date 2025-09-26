/**
 * Auth Form - Gestion du formulaire d'authentification
 * Version 1.0 - Interface d'authentification pour BudgetPro
 */

class AuthForm {
    constructor() {
        this.currentTab = 'login';
        this.init();
    }

    /**
     * Initialise le formulaire d'authentification
     */
    init() {
        this.setupEventListeners();
        this.setupPasswordStrength();
        console.log('✅ Formulaire d\'authentification initialisé');
    }

    /**
     * Configure les écouteurs d'événements
     */
    setupEventListeners() {
        // Formulaire de connexion
        document.getElementById('loginForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleLogin();
        });

        // Formulaire d'inscription
        document.getElementById('registerForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleRegister();
        });

        // Validation en temps réel
        this.setupRealTimeValidation();
    }

    /**
     * Configure la validation en temps réel
     */
    setupRealTimeValidation() {
        // Email de connexion
        document.getElementById('loginEmail').addEventListener('blur', () => {
            this.validateEmail('loginEmail');
        });

        // Mot de passe de connexion
        document.getElementById('loginPassword').addEventListener('blur', () => {
            this.validatePassword('loginPassword');
        });

        // Nom d'inscription
        document.getElementById('registerName').addEventListener('blur', () => {
            this.validateName('registerName');
        });

        // Email d'inscription
        document.getElementById('registerEmail').addEventListener('blur', () => {
            this.validateEmail('registerEmail');
        });

        // Mot de passe d'inscription
        document.getElementById('registerPassword').addEventListener('input', () => {
            this.updatePasswordStrength();
        });

        document.getElementById('registerPassword').addEventListener('blur', () => {
            this.validatePassword('registerPassword');
        });

        // Confirmation de mot de passe
        document.getElementById('confirmPassword').addEventListener('blur', () => {
            this.validateConfirmPassword();
        });
    }

    /**
     * Configure l'indicateur de force du mot de passe
     */
    setupPasswordStrength() {
        const passwordInput = document.getElementById('registerPassword');
        const strengthIndicator = document.getElementById('passwordStrength');

        passwordInput.addEventListener('input', () => {
            const password = passwordInput.value;
            const strength = this.calculatePasswordStrength(password);
            
            strengthIndicator.textContent = strength.text;
            strengthIndicator.className = `password-strength ${strength.class}`;
        });
    }

    /**
     * Calcule la force du mot de passe
     */
    calculatePasswordStrength(password) {
        if (password.length < 6) {
            return { text: 'Mot de passe trop court', class: 'strength-weak' };
        }

        let score = 0;
        if (password.length >= 8) score++;
        if (/[a-z]/.test(password)) score++;
        if (/[A-Z]/.test(password)) score++;
        if (/[0-9]/.test(password)) score++;
        if (/[^A-Za-z0-9]/.test(password)) score++;

        if (score < 3) {
            return { text: 'Mot de passe faible', class: 'strength-weak' };
        } else if (score < 4) {
            return { text: 'Mot de passe moyen', class: 'strength-medium' };
        } else {
            return { text: 'Mot de passe fort', class: 'strength-strong' };
        }
    }

    /**
     * Met à jour l'indicateur de force du mot de passe
     */
    updatePasswordStrength() {
        const password = document.getElementById('registerPassword').value;
        const strength = this.calculatePasswordStrength(password);
        const indicator = document.getElementById('passwordStrength');
        
        indicator.textContent = strength.text;
        indicator.className = `password-strength ${strength.class}`;
    }

    /**
     * Bascule entre les onglets de connexion et d'inscription
     */
    switchTab(tab) {
        this.currentTab = tab;
        
        // Mettre à jour les onglets
        document.querySelectorAll('.auth-tab').forEach(t => t.classList.remove('active'));
        document.querySelector(`[onclick="switchTab('${tab}')"]`).classList.add('active');
        
        // Afficher/masquer les formulaires
        document.getElementById('loginForm').classList.toggle('form-hidden', tab !== 'login');
        document.getElementById('registerForm').classList.toggle('form-hidden', tab !== 'register');
        
        // Effacer les messages d'erreur
        this.clearMessages();
        this.clearErrors();
    }

    /**
     * Gère la connexion
     */
    async handleLogin() {
        const email = document.getElementById('loginEmail').value;
        const password = document.getElementById('loginPassword').value;

        // Validation
        if (!this.validateLoginForm(email, password)) {
            return;
        }

        this.showLoading(true);
        this.clearMessages();

        try {
            const result = await window.loginUser(email, password);
            
            if (result.success) {
                this.showSuccess('Connexion réussie ! Redirection...');
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 1500);
            } else {
                this.showError(result.error);
            }
        } catch (error) {
            this.showError('Erreur de connexion. Veuillez réessayer.');
            console.error('Erreur de connexion:', error);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Gère l'inscription
     */
    async handleRegister() {
        const name = document.getElementById('registerName').value;
        const email = document.getElementById('registerEmail').value;
        const password = document.getElementById('registerPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        // Validation
        if (!this.validateRegisterForm(name, email, password, confirmPassword)) {
            return;
        }

        this.showLoading(true);
        this.clearMessages();

        try {
            const result = await window.registerUser(email, name, password);
            
            if (result.success) {
                this.showSuccess('Inscription réussie ! Redirection...');
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 1500);
            } else {
                this.showError(result.error);
            }
        } catch (error) {
            this.showError('Erreur d\'inscription. Veuillez réessayer.');
            console.error('Erreur d\'inscription:', error);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Valide le formulaire de connexion
     */
    validateLoginForm(email, password) {
        let isValid = true;

        if (!this.validateEmail('loginEmail')) isValid = false;
        if (!this.validatePassword('loginPassword')) isValid = false;

        return isValid;
    }

    /**
     * Valide le formulaire d'inscription
     */
    validateRegisterForm(name, email, password, confirmPassword) {
        let isValid = true;

        if (!this.validateName('registerName')) isValid = false;
        if (!this.validateEmail('registerEmail')) isValid = false;
        if (!this.validatePassword('registerPassword')) isValid = false;
        if (!this.validateConfirmPassword()) isValid = false;

        return isValid;
    }

    /**
     * Valide un nom
     */
    validateName(fieldId) {
        const field = document.getElementById(fieldId);
        const value = field.value.trim();
        const errorElement = document.getElementById(fieldId + 'Error');

        if (value.length < 2) {
            this.showFieldError(field, errorElement, 'Le nom doit contenir au moins 2 caractères');
            return false;
        }

        this.clearFieldError(field, errorElement);
        return true;
    }

    /**
     * Valide un email
     */
    validateEmail(fieldId) {
        const field = document.getElementById(fieldId);
        const value = field.value.trim();
        const errorElement = document.getElementById(fieldId + 'Error');
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        if (!value) {
            this.showFieldError(field, errorElement, 'L\'email est requis');
            return false;
        }

        if (!emailRegex.test(value)) {
            this.showFieldError(field, errorElement, 'Format d\'email invalide');
            return false;
        }

        this.clearFieldError(field, errorElement);
        return true;
    }

    /**
     * Valide un mot de passe
     */
    validatePassword(fieldId) {
        const field = document.getElementById(fieldId);
        const value = field.value;
        const errorElement = document.getElementById(fieldId + 'Error');

        if (!value) {
            this.showFieldError(field, errorElement, 'Le mot de passe est requis');
            return false;
        }

        if (value.length < 6) {
            this.showFieldError(field, errorElement, 'Le mot de passe doit contenir au moins 6 caractères');
            return false;
        }

        this.clearFieldError(field, errorElement);
        return true;
    }

    /**
     * Valide la confirmation de mot de passe
     */
    validateConfirmPassword() {
        const password = document.getElementById('registerPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        const field = document.getElementById('confirmPassword');
        const errorElement = document.getElementById('confirmPasswordError');

        if (!confirmPassword) {
            this.showFieldError(field, errorElement, 'Veuillez confirmer votre mot de passe');
            return false;
        }

        if (password !== confirmPassword) {
            this.showFieldError(field, errorElement, 'Les mots de passe ne correspondent pas');
            return false;
        }

        this.clearFieldError(field, errorElement);
        return true;
    }

    /**
     * Affiche une erreur pour un champ
     */
    showFieldError(field, errorElement, message) {
        field.classList.add('error');
        errorElement.textContent = message;
        errorElement.classList.add('show');
    }

    /**
     * Efface l'erreur d'un champ
     */
    clearFieldError(field, errorElement) {
        field.classList.remove('error');
        errorElement.classList.remove('show');
    }

    /**
     * Efface toutes les erreurs
     */
    clearErrors() {
        document.querySelectorAll('.error').forEach(field => {
            field.classList.remove('error');
        });
        document.querySelectorAll('.error-message').forEach(error => {
            error.classList.remove('show');
        });
    }

    /**
     * Affiche un message de succès
     */
    showSuccess(message) {
        const successElement = document.getElementById('successMessage');
        successElement.textContent = message;
        successElement.classList.add('show');
    }

    /**
     * Affiche un message d'erreur
     */
    showError(message) {
        const errorElement = document.getElementById('errorMessage');
        errorElement.textContent = message;
        errorElement.classList.add('show');
    }

    /**
     * Efface tous les messages
     */
    clearMessages() {
        document.getElementById('successMessage').classList.remove('show');
        document.getElementById('errorMessage').classList.remove('show');
    }

    /**
     * Affiche/masque l'indicateur de chargement
     */
    showLoading(show) {
        const loadingElement = document.getElementById('loading');
        const loginButton = document.getElementById('loginButton');
        const registerButton = document.getElementById('registerButton');

        if (show) {
            loadingElement.classList.add('show');
            loginButton.disabled = true;
            registerButton.disabled = true;
        } else {
            loadingElement.classList.remove('show');
            loginButton.disabled = false;
            registerButton.disabled = false;
        }
    }
}

// Fonction globale pour basculer entre les onglets
function switchTab(tab) {
    if (window.authForm) {
        window.authForm.switchTab(tab);
    }
}

// Initialisation du formulaire d'authentification
document.addEventListener('DOMContentLoaded', () => {
    window.authForm = new AuthForm();
});
