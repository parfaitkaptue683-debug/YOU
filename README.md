# BudgetPro - Application de Gestion de Budget Personnel

## 📋 Description

BudgetPro est une application web moderne pour la gestion de budget personnel avec une architecture frontend/backend séparée. L'application permet aux utilisateurs de créer des budgets mensuels, suivre leurs dépenses et analyser leurs habitudes financières.

## 🏗️ Architecture

### Frontend
- **HTML/CSS/JavaScript** - Interface utilisateur moderne et responsive
- **auth.html** - Page d'authentification (inscription/connexion)
- **index.html** - Tableau de bord principal
- **auth.js** - Gestionnaire d'authentification
- **auth-form.js** - Logique du formulaire d'authentification
- **budget-app.js** - Application principale de gestion de budget

### Backend
- **Java + Vert.x** - Framework asynchrone pour l'API REST
- **PostgreSQL** - Base de données pour la persistance
- **Architecture en couches** - Repository, Service, Controller

## 🚀 Fonctionnalités

### Authentification
- ✅ Inscription d'utilisateur avec validation
- ✅ Connexion sécurisée
- ✅ Gestion de session avec localStorage
- ✅ Redirection automatique selon l'état de connexion

### Gestion de Budget
- ✅ Création de budget mensuel avec 3 catégories :
  - **Loisirs** - Divertissement et sorties
  - **Essentiels** - Dépenses nécessaires (logement, nourriture, etc.)
  - **Épargne** - Mise de côté
- ✅ Validation des règles métier :
  - Budget total ≤ Revenus
  - Épargne ≤ (Revenus - Essentiels)
- ✅ Suivi des dépenses par catégorie
- ✅ Calcul automatique des pourcentages et soldes

### Gestion des Dépenses
- ✅ Ajout de dépenses avec catégorisation
- ✅ Historique des dépenses
- ✅ Recherche et filtrage
- ✅ Statistiques et analyses

## 📁 Structure des Fichiers

```
YOU/                    # Racine du projet
├── README.md          # Documentation du projet
├── desktop/           # Frontend
│   ├── auth.html      # Page d'authentification
│   ├── index.html     # Tableau de bord principal
│   ├── auth.js        # Gestionnaire d'authentification
│   ├── auth-form.js   # Logique du formulaire d'auth
│   ├── budget-app.js  # Application principale
│   └── style.css      # Styles CSS
└── Backend/           # Backend Java
├── src/main/java/com/you/budget/
│   ├── MainVerticle.java           # Point d'entrée
│   ├── http/
│   │   ├── HttpVerticle.java       # Routes HTTP
│   │   └── HttpController.java     # Utilitaires HTTP
│   ├── database/
│   │   └── DatabaseClient.java     # Gestionnaire DB
│   ├── user/                       # Domaine utilisateur
│   │   ├── User.java
│   │   ├── UserRepository.java
│   │   ├── UserService.java
│   │   └── UserController.java
│   ├── budget/                     # Domaine budget
│   │   ├── Budget.java
│   │   ├── BudgetRepository.java
│   │   ├── BudgetService.java
│   │   └── BudgetController.java
│   └── expense/                    # Domaine dépense
│       ├── Expense.java
│       ├── ExpenseRepository.java
│       ├── ExpenseService.java
│       └── ExpenseController.java
├── src/main/resources/
│   └── logback.xml                 # Configuration logging
└── pom.xml                         # Configuration Maven
```

## 🔧 Installation et Démarrage

### Prérequis
- Java 11+
- Maven 3.6+
- PostgreSQL 12+
- Navigateur web moderne

### Base de données
1. Créer une base de données PostgreSQL nommée `you`
2. Configurer les tables (voir scripts SQL dans le backend)

### Backend
```bash
cd Backend
mvn clean install
mvn exec:java -Dexec.mainClass="com.you.budget.MainVerticle"
```

### Frontend
1. Ouvrir `desktop/auth.html` dans un navigateur
2. S'inscrire ou se connecter
3. Accéder au tableau de bord via `desktop/index.html`

## 🔐 Flux d'Authentification

1. **Première visite** → `desktop/auth.html`
2. **Inscription/Connexion** → Validation côté backend
3. **Succès** → Redirection vers `desktop/index.html`
4. **Accès direct à index.html** → Vérification de session
5. **Session valide** → Accès autorisé
6. **Session invalide** → Redirection vers `desktop/auth.html`

## 📊 API Endpoints

### Authentification (`/api/users`)
- `POST /register` - Inscription
- `POST /login` - Connexion
- `GET /profile/:id` - Profil utilisateur
- `PUT /:id` - Mise à jour profil
- `DELETE /:id` - Suppression compte

### Budgets (`/api/budgets`)
- `POST /` - Créer budget
- `GET /current/:userId` - Budget actuel
- `PUT /:id` - Mettre à jour budget
- `POST /:id/expenses` - Ajouter dépense

### Dépenses (`/api/expenses`)
- `POST /` - Créer dépense
- `GET /user/:userId` - Dépenses utilisateur
- `GET /budget/:budgetId` - Dépenses budget
- `GET /summary/:userId` - Résumé dépenses
- `GET /search?q=query` - Recherche dépenses

## 🎨 Interface Utilisateur

### Page d'Authentification (`desktop/auth.html`)
- Design moderne avec onglets
- Validation en temps réel
- Indicateur de force du mot de passe
- Messages d'erreur contextuels
- Animations fluides

### Tableau de Bord (`desktop/index.html`)
- Cartes de budget avec indicateurs visuels
- Graphiques de dépenses
- Formulaire d'ajout de dépenses
- Informations utilisateur avec déconnexion
- Interface responsive

## 🔒 Sécurité

- Validation côté client et serveur
- Gestion de session sécurisée
- Protection contre les injections SQL
- Validation des données d'entrée
- Gestion d'erreurs appropriée

## 🚧 Améliorations Futures

- [ ] Chiffrement des mots de passe
- [ ] Authentification JWT
- [ ] Notifications push
- [ ] Export PDF des rapports
- [ ] Mode hors ligne
- [ ] Application mobile
- [ ] Multi-devises
- [ ] Catégories personnalisées

## 📝 Notes de Développement

- Architecture modulaire par domaine métier
- Séparation claire frontend/backend
- Code asynchrone avec CompletableFuture
- Logging structuré avec SLF4J/Logback
- Gestion d'erreurs centralisée
- Interface utilisateur moderne et accessible
