/**
 * BudgetPro - Application de Gestion de Budget Personnel
 * Version 2.0 - Interface Moderne avec Animations
 */

class BudgetApp {
    constructor() {
        this.budgetData = {
            loisirs: {
                prevu: 200,
                actuel: 150,
                pourcentage: 0,
                historique: []
            },
            essentiels: {
                prevu: 1200,
                actuel: 1100,
                pourcentage: 0,
                historique: []
            },
            epargne: {
                prevu: 300,
                actuel: 200,
                pourcentage: 0,
                historique: []
            },
            revenu: 2000,
            joursRestants: 12,
            notifications: []
        };

        this.charts = {};
        this.init();
    }

    init() {
        this.calculatePercentages();
        this.updateUI();
        this.initializeCharts();
        this.setupEventListeners();
        this.loadNotifications();
        this.startAnimations();
        
        // Valider la cohérence des données au chargement
        setTimeout(() => {
            this.displayValidations();
        }, 1000);
    }

    calculatePercentages() {
        this.budgetData.loisirs.pourcentage = Math.min(100, (this.budgetData.loisirs.actuel / this.budgetData.loisirs.prevu) * 100);
        this.budgetData.essentiels.pourcentage = Math.min(100, (this.budgetData.essentiels.actuel / this.budgetData.essentiels.prevu) * 100);
        this.budgetData.epargne.pourcentage = Math.min(100, (this.budgetData.epargne.actuel / this.budgetData.epargne.prevu) * 100);
    }

    updateUI() {
        // Mise à jour des cartes avec animations
        this.updateCard('loisirs', this.budgetData.loisirs);
        this.updateCard('essentiels', this.budgetData.essentiels);
        this.updateCard('epargne', this.budgetData.epargne);
        
        // Mise à jour du revenu total
        document.getElementById('revenu-total').textContent = this.budgetData.revenu;
        document.getElementById('jours-restants').textContent = `${this.budgetData.joursRestants} jours restants`;
    }

    updateCard(type, data) {
        const elements = {
            loisirs: {
                actuel: 'loisirs-actuels',
                prevu: 'loisirs-prevus',
                progress: 'loisirs-progress',
                percent: 'loisirs-percent',
                status: 'loisirs-status'
            },
            essentiels: {
                actuel: 'essentiels-actuels',
                prevu: 'essentiels-prevus',
                progress: 'essentiels-progress',
                percent: 'essentiels-percent',
                status: 'essentiels-status'
            },
            epargne: {
                actuel: 'epargne-actuelle',
                prevu: 'epargne-prevue',
                progress: 'epargne-progress',
                percent: 'epargne-percent',
                status: 'epargne-status'
            }
        };

        const el = elements[type];
        if (!el) return;

        // Animation des valeurs numériques
        this.animateNumber(document.getElementById(el.actuel), data.actuel);
        this.animateNumber(document.getElementById(el.prevu), data.prevu);
        
        // Animation de la barre de progression
        this.animateProgress(document.getElementById(el.progress), data.pourcentage);
        
        // Mise à jour du pourcentage et du statut
        document.getElementById(el.percent).textContent = `${Math.round(data.pourcentage)}%`;
        document.getElementById(el.status).textContent = this.getStatusText(data.pourcentage);
    }

    animateNumber(element, targetValue) {
        const startValue = parseInt(element.textContent) || 0;
        const duration = 1000;
        const startTime = performance.now();

        const animate = (currentTime) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            // Easing function (ease-out)
            const easeOut = 1 - Math.pow(1 - progress, 3);
            const currentValue = Math.round(startValue + (targetValue - startValue) * easeOut);
            
            element.textContent = currentValue;
            
            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);
    }

    animateProgress(element, targetPercentage) {
        const startWidth = parseFloat(element.style.width) || 0;
        const duration = 1500;
        const startTime = performance.now();

        const animate = (currentTime) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            // Easing function (ease-out)
            const easeOut = 1 - Math.pow(1 - progress, 3);
            const currentWidth = startWidth + (targetPercentage - startWidth) * easeOut;
            
            element.style.width = `${currentWidth}%`;
            
            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);
    }

    getStatusText(percentage) {
        if (percentage >= 100) return 'Dépassé';
        if (percentage >= 90) return 'Attention';
        if (percentage >= 75) return 'Élevé';
        if (percentage >= 50) return 'Modéré';
        return 'Faible';
    }

    initializeCharts() {
        // Graphique des dépenses (Doughnut)
        const expensesCtx = document.getElementById('expensesChart').getContext('2d');
        this.charts.expenses = new Chart(expensesCtx, {
            type: 'doughnut',
            data: {
                labels: ['Loisirs', 'Essentiels', 'Épargne', 'Restant'],
                datasets: [{
                    data: [
                        this.budgetData.loisirs.actuel,
                        this.budgetData.essentiels.actuel,
                        this.budgetData.epargne.actuel,
                        this.budgetData.revenu - (this.budgetData.loisirs.actuel + this.budgetData.essentiels.actuel + this.budgetData.epargne.actuel)
                    ],
                    backgroundColor: [
                        '#f093fb',
                        '#f5576c',
                        '#4facfe',
                        '#667eea'
                    ],
                    borderWidth: 0,
                    hoverOffset: 10
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 15,
                            usePointStyle: true,
                            font: {
                                size: 12
                            }
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const percentage = ((context.parsed / total) * 100).toFixed(1);
                                return `${context.label}: €${context.parsed} (${percentage}%)`;
                            }
                        }
                    }
                },
                animation: {
                    animateRotate: true,
                    animateScale: true,
                    duration: 1500
                }
            }
        });

        // Graphique des prévisions (Bar)
        const forecastCtx = document.getElementById('forecastChart').getContext('2d');
        this.charts.forecast = new Chart(forecastCtx, {
            type: 'bar',
            data: {
                labels: ['Loisirs', 'Essentiels'],
                datasets: [
                    {
                        label: 'Budget Prévisionnel',
                        data: [this.budgetData.loisirs.prevu, this.budgetData.essentiels.prevu],
                        backgroundColor: 'rgba(102, 126, 234, 0.8)',
                        borderRadius: 8,
                        borderSkipped: false
                    },
                    {
                        label: 'Dépenses Actuelles',
                        data: [this.budgetData.loisirs.actuel, this.budgetData.essentiels.actuel],
                        backgroundColor: 'rgba(118, 75, 162, 0.8)',
                        borderRadius: 8,
                        borderSkipped: false
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: 'rgba(0, 0, 0, 0.1)'
                        },
                        ticks: {
                            font: {
                                size: 11
                            }
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        },
                        ticks: {
                            font: {
                                size: 11
                            }
                        }
                    }
                },
                plugins: {
                    legend: {
                        position: 'top',
                        labels: {
                            padding: 15,
                            usePointStyle: true,
                            font: {
                                size: 12
                            }
                        }
                    }
                },
                animation: {
                    duration: 1500,
                    easing: 'easeInOutQuart'
                }
            }
        });
    }

    setupEventListeners() {
        // Clic sur les cartes pour plus de détails
        document.querySelectorAll('.card').forEach(card => {
            card.addEventListener('click', (e) => {
                this.showCardDetails(card);
            });
        });

        // Hover effects pour les notifications
        document.querySelectorAll('.notification').forEach(notification => {
            notification.addEventListener('click', (e) => {
                this.handleNotificationClick(notification);
            });
        });
    }

    handleNotificationClick(notification) {
        // Animation de clic
        notification.style.transform = 'scale(0.98)';
        setTimeout(() => {
            notification.style.transform = '';
        }, 150);

        // Marquer comme lu (optionnel)
        notification.style.opacity = '0.7';
        console.log('Notification cliquée:', notification.querySelector('h4').textContent);
    }

    loadNotifications() {
        // Charger les notifications existantes
        this.budgetData.notifications = [
            {
                type: 'warning',
                title: '75% de votre budget Loisirs atteint',
                message: 'C\'est le moment de ralentir un peu pour éviter le dépassement.',
                time: 'Il y a 2 heures',
                read: false
            },
            {
                type: 'success',
                title: '50% de votre budget Loisirs utilisé',
                message: 'Super ! Continuez à garder un œil sur vos prochaines dépenses.',
                time: 'Hier à 14:30',
                read: false
            },
            {
                type: 'danger',
                title: 'Dépassement de votre budget Essentiels',
                message: 'Il est conseillé d\'ajuster vos prochains choix pour garder un équilibre.',
                time: '12 Octobre 2023',
                read: false
            }
        ];
    }

    addNotification(type, title, message, time = 'À l\'instant') {
        const notification = {
            type,
            title,
            message,
            time,
            read: false
        };

        this.budgetData.notifications.unshift(notification);

        // Ajouter à l'interface
        this.renderNotification(notification);
    }

    renderNotification(notification) {
        const notificationsList = document.getElementById('notifications-list');
        const notificationElement = document.createElement('div');
        notificationElement.className = `notification ${notification.type}`;
        
        let icon = '✅';
        if (notification.type === 'warning') icon = '⚠️';
        if (notification.type === 'danger') icon = '🚨';
        
        notificationElement.innerHTML = `
            <div class="notification-icon">${icon}</div>
            <div class="notification-info">
                <h4>${notification.title}</h4>
                <p>${notification.message}</p>
                <div class="notification-time">${notification.time}</div>
            </div>
        `;

        // Animation d'entrée
        notificationElement.style.opacity = '0';
        notificationElement.style.transform = 'translateX(-30px)';
        
        notificationsList.insertBefore(notificationElement, notificationsList.firstChild);
        
        // Animation
        setTimeout(() => {
            notificationElement.style.transition = 'all 0.3s ease';
            notificationElement.style.opacity = '1';
            notificationElement.style.transform = 'translateX(0)';
        }, 100);
    }

    startAnimations() {
        // Animation des cartes au chargement
        const cards = document.querySelectorAll('.card');
        cards.forEach((card, index) => {
            card.style.animationDelay = `${index * 0.1}s`;
        });

        // Animation des graphiques
        setTimeout(() => {
            if (this.charts.expenses) {
                this.charts.expenses.update('active');
            }
            if (this.charts.forecast) {
                this.charts.forecast.update('active');
            }
        }, 1000);
    }

    // Méthodes utilitaires
    updateBudgetData(newData) {
        this.budgetData = { ...this.budgetData, ...newData };
        this.calculatePercentages();
        this.updateUI();
        this.updateCharts();
    }

    updateCharts() {
        if (this.charts.expenses) {
            this.charts.expenses.data.datasets[0].data = [
                this.budgetData.loisirs.actuel,
                this.budgetData.essentiels.actuel,
                this.budgetData.epargne.actuel,
                this.budgetData.revenu - (this.budgetData.loisirs.actuel + this.budgetData.essentiels.actuel + this.budgetData.epargne.actuel)
            ];
            this.charts.expenses.update();
        }

        if (this.charts.forecast) {
            this.charts.forecast.data.datasets[0].data = [this.budgetData.loisirs.prevu, this.budgetData.essentiels.prevu];
            this.charts.forecast.data.datasets[1].data = [this.budgetData.loisirs.actuel, this.budgetData.essentiels.actuel];
            this.charts.forecast.update();
        }
    }

    // Méthode pour valider la cohérence des données budgétaires
    validateBudgetCoherence() {
        const totalBudget = this.budgetData.loisirs.prevu + this.budgetData.essentiels.prevu + this.budgetData.epargne.prevu;
        const revenu = this.budgetData.revenu;
        
        const errors = [];
        const warnings = [];

        // Vérification 1: Le budget total ne doit pas dépasser les revenus
        if (totalBudget > revenu) {
            errors.push({
                type: 'error',
                title: 'Budget total dépassé',
                message: `Le budget total (€${totalBudget}) dépasse vos revenus (€${revenu}). Réduction nécessaire de €${totalBudget - revenu}.`
            });
        }

        // Vérification 2: L'épargne ne doit pas être supérieure à la différence revenus - budget essentiels
        const budgetEssentiels = this.budgetData.essentiels.prevu;
        const budgetLoisirs = this.budgetData.loisirs.prevu;
        const budgetEpargne = this.budgetData.epargne.prevu;
        
        const revenuDisponibleApresEssentiels = revenu - budgetEssentiels;
        
        if (budgetEpargne > revenuDisponibleApresEssentiels) {
            errors.push({
                type: 'error',
                title: 'Épargne excessive',
                message: `L'épargne prévue (€${budgetEpargne}) dépasse le revenu disponible après essentiels (€${revenuDisponibleApresEssentiels}).`
            });
        }

        // Vérification 3: Les dépenses actuelles ne doivent pas dépasser les budgets prévus
        if (this.budgetData.loisirs.actuel > this.budgetData.loisirs.prevu) {
            warnings.push({
                type: 'warning',
                title: 'Dépassement budget Loisirs',
                message: `Dépenses actuelles (€${this.budgetData.loisirs.actuel}) > Budget prévu (€${this.budgetData.loisirs.prevu})`
            });
        }

        if (this.budgetData.essentiels.actuel > this.budgetData.essentiels.prevu) {
            warnings.push({
                type: 'warning',
                title: 'Dépassement budget Essentiels',
                message: `Dépenses actuelles (€${this.budgetData.essentiels.actuel}) > Budget prévu (€${this.budgetData.essentiels.prevu})`
            });
        }

        // Vérification 4: Recommandations d'équilibre
        const pourcentageEssentiels = (budgetEssentiels / revenu) * 100;
        const pourcentageLoisirs = (budgetLoisirs / revenu) * 100;
        const pourcentageEpargne = (budgetEpargne / revenu) * 100;

        if (pourcentageEssentiels > 70) {
            warnings.push({
                type: 'info',
                title: 'Budget Essentiels élevé',
                message: `Les essentiels représentent ${pourcentageEssentiels.toFixed(1)}% de vos revenus. Recommandation: < 70%`
            });
        }

        if (pourcentageEpargne < 10) {
            warnings.push({
                type: 'info',
                title: 'Épargne faible',
                message: `L'épargne ne représente que ${pourcentageEpargne.toFixed(1)}% de vos revenus. Recommandation: > 10%`
            });
        }

        return { errors, warnings };
    }

    // Méthode pour afficher les validations
    displayValidations() {
        const validations = this.validateBudgetCoherence();
        
        // Afficher les erreurs critiques
        validations.errors.forEach(error => {
            this.addNotification('danger', error.title, error.message);
        });

        // Afficher les avertissements
        validations.warnings.forEach(warning => {
            const type = warning.type === 'info' ? 'success' : 'warning';
            this.addNotification(type, warning.title, warning.message);
        });

        return validations.errors.length === 0;
    }

    // Méthode pour suggérer des ajustements automatiques
    suggestBudgetAdjustments() {
        const totalBudget = this.budgetData.loisirs.prevu + this.budgetData.essentiels.prevu + this.budgetData.epargne.prevu;
        const revenu = this.budgetData.revenu;
        
        if (totalBudget > revenu) {
            const reductionNecessaire = totalBudget - revenu;
            
            // Calculer les réductions proportionnelles
            const reductionLoisirs = (this.budgetData.loisirs.prevu / totalBudget) * reductionNecessaire;
            const reductionEssentiels = (this.budgetData.essentiels.prevu / totalBudget) * reductionNecessaire;
            const reductionEpargne = (this.budgetData.epargne.prevu / totalBudget) * reductionNecessaire;

            return {
                loisirs: Math.max(0, this.budgetData.loisirs.prevu - reductionLoisirs),
                essentiels: Math.max(0, this.budgetData.essentiels.prevu - reductionEssentiels),
                epargne: Math.max(0, this.budgetData.epargne.prevu - reductionEpargne)
            };
        }

        return null;
    }

    // Méthode pour ajouter une dépense
    addExpense(category, amount, description = '') {
        if (this.budgetData[category]) {
            // Vérifier si l'ajout de cette dépense dépassera le budget
            const nouveauTotal = this.budgetData[category].actuel + amount;
            if (nouveauTotal > this.budgetData[category].prevu) {
                const depassement = nouveauTotal - this.budgetData[category].prevu;
                this.addNotification('warning', 'Dépassement imminent', 
                    `Cette dépense de €${amount} dépassera votre budget ${category} de €${depassement}.`);
            }

            this.budgetData[category].actuel += amount;
            this.budgetData[category].historique.push({
                amount,
                description,
                date: new Date().toISOString(),
                type: 'expense'
            });
            
            this.calculatePercentages();
            this.updateUI();
            this.updateCharts();
            
            // Vérifier les alertes
            this.checkAlerts(category);
            
            return true;
        }
        return false;
    }

    checkAlerts(category) {
        const data = this.budgetData[category];
        const percentage = data.pourcentage;
        
        if (percentage >= 100) {
            this.addNotification('danger', `Dépassement du budget ${category}`, `Vous avez dépassé votre budget de ${category} de ${Math.round(percentage - 100)}%`);
        } else if (percentage >= 90) {
            this.addNotification('warning', `90% du budget ${category} atteint`, `Attention, vous approchez de la limite de votre budget ${category}`);
        } else if (percentage >= 75) {
            this.addNotification('warning', `75% du budget ${category} atteint`, `Vous avez utilisé les trois quarts de votre budget ${category}`);
        }
    }

    // Méthode pour exporter les données
    exportData() {
        const dataStr = JSON.stringify(this.budgetData, null, 2);
        const dataBlob = new Blob([dataStr], { type: 'application/json' });
        const url = URL.createObjectURL(dataBlob);
        
        const link = document.createElement('a');
        link.href = url;
        link.download = `budget-data-${new Date().toISOString().split('T')[0]}.json`;
        link.click();
        
        URL.revokeObjectURL(url);
    }

    // Méthode pour importer des données
    importData(file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const data = JSON.parse(e.target.result);
                this.updateBudgetData(data);
                console.log('Données importées avec succès');
            } catch (error) {
                console.error('Erreur lors de l\'importation:', error);
            }
        };
        reader.readAsText(file);
    }
}

// Initialisation de l'application
document.addEventListener('DOMContentLoaded', () => {
    window.budgetApp = new BudgetApp();
    
    // Exemple d'ajout d'une notification après 3 secondes
    setTimeout(() => {
        window.budgetApp.addNotification(
            'warning',
            '90% Budget Essentiels atteint',
            'Il reste peu de marge : surveillez bien vos choix.',
            'À l\'instant'
        );
    }, 3000);
});

// Fonctions globales pour les interactions
function addExpense(category, amount, description) {
    if (window.budgetApp) {
        return window.budgetApp.addExpense(category, amount, description);
    }
    return false;
}

function exportBudgetData() {
    if (window.budgetApp) {
        window.budgetApp.exportData();
    }
}

function importBudgetData() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    input.onchange = (e) => {
        if (e.target.files[0] && window.budgetApp) {
            window.budgetApp.importData(e.target.files[0]);
        }
    };
    input.click();
}

// Fonctions pour les modales
function showAddExpenseModal() {
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>Ajouter une Dépense</h3>
                <button class="modal-close">&times;</button>
            </div>
            <div class="modal-body">
                <form id="addExpenseForm">
                    <div class="form-group">
                        <label for="expenseCategory">Catégorie:</label>
                        <select id="expenseCategory" required>
                            <option value="loisirs">Loisirs</option>
                            <option value="essentiels">Essentiels</option>
                            <option value="epargne">Épargne</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="expenseAmount">Montant (€):</label>
                        <input type="number" id="expenseAmount" step="0.01" min="0" required>
                    </div>
                    <div class="form-group">
                        <label for="expenseDescription">Description:</label>
                        <input type="text" id="expenseDescription" placeholder="Ex: Restaurant, Transport...">
                    </div>
                    <div class="form-actions">
                        <button type="button" class="btn btn-danger" onclick="closeModal(this)">Annuler</button>
                        <button type="submit" class="btn btn-primary">Ajouter</button>
                    </div>
                </form>
            </div>
        </div>
    `;

    addModalStyles();
    document.body.appendChild(modal);

    // Gestion du formulaire
    modal.querySelector('#addExpenseForm').addEventListener('submit', (e) => {
        e.preventDefault();
        const category = document.getElementById('expenseCategory').value;
        const amount = parseFloat(document.getElementById('expenseAmount').value);
        const description = document.getElementById('expenseDescription').value;

        // Validation stricte avant ajout
        if (window.budgetApp) {
            const nouveauTotal = window.budgetApp.budgetData[category].actuel + amount;
            const budgetPrevu = window.budgetApp.budgetData[category].prevu;
            
            if (nouveauTotal > budgetPrevu) {
                const depassement = nouveauTotal - budgetPrevu;
                showErrorMessage(`Cette dépense dépasserait votre budget ${category} de €${depassement.toFixed(2)}. Réduction nécessaire.`);
                return false;
            }
            
            // Si tout est OK, procéder à l'ajout
            if (window.budgetApp.addExpense(category, amount, description)) {
                closeModal(modal.querySelector('.modal-close'));
                showSuccessMessage('Dépense ajoutée avec succès !');
            } else {
                showErrorMessage('Erreur lors de l\'ajout de la dépense');
            }
        }
    });

    setupModalClose(modal);
}

function showAddIncomeModal() {
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>Ajouter un Revenu</h3>
                <button class="modal-close">&times;</button>
            </div>
            <div class="modal-body">
                <form id="addIncomeForm">
                    <div class="form-group">
                        <label for="incomeAmount">Montant (€):</label>
                        <input type="number" id="incomeAmount" step="0.01" min="0" required>
                    </div>
                    <div class="form-group">
                        <label for="incomeDescription">Description:</label>
                        <input type="text" id="incomeDescription" placeholder="Ex: Salaire, Prime...">
                    </div>
                    <div class="form-actions">
                        <button type="button" class="btn btn-danger" onclick="closeModal(this)">Annuler</button>
                        <button type="submit" class="btn btn-success">Ajouter</button>
                    </div>
                </form>
            </div>
        </div>
    `;

    addModalStyles();
    document.body.appendChild(modal);

    // Gestion du formulaire
    modal.querySelector('#addIncomeForm').addEventListener('submit', (e) => {
        e.preventDefault();
        const amount = parseFloat(document.getElementById('incomeAmount').value);
        const description = document.getElementById('incomeDescription').value;

        if (window.budgetApp) {
            window.budgetApp.budgetData.revenu += amount;
            window.budgetApp.updateUI();
            window.budgetApp.updateCharts();
            closeModal(modal.querySelector('.modal-close'));
            showSuccessMessage('Revenu ajouté avec succès !');
        }
    });

    setupModalClose(modal);
}

function showBudgetSettingsModal() {
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h3>Paramètres du Budget</h3>
                <button class="modal-close">&times;</button>
            </div>
            <div class="modal-body">
                <form id="budgetSettingsForm">
                    <div class="form-group">
                        <label for="totalIncome">Revenu Total (€):</label>
                        <input type="number" id="totalIncome" step="0.01" min="0" value="${window.budgetApp ? window.budgetApp.budgetData.revenu : 2500}" required>
                    </div>
                    <div class="form-group">
                        <label for="essentielsBudget">Budget Essentiels (€):</label>
                        <input type="number" id="essentielsBudget" step="0.01" min="0" value="${window.budgetApp ? window.budgetApp.budgetData.essentiels.prevu : 1200}" required>
                        <small class="form-help">Recommandé: 50-70% du revenu</small>
                    </div>
                    <div class="form-group">
                        <label for="loisirsBudget">Budget Loisirs (€):</label>
                        <input type="number" id="loisirsBudget" step="0.01" min="0" value="${window.budgetApp ? window.budgetApp.budgetData.loisirs.prevu : 300}" required>
                        <small class="form-help">Recommandé: 10-20% du revenu</small>
                    </div>
                    <div class="form-group">
                        <label for="epargneBudget">Budget Épargne (€):</label>
                        <input type="number" id="epargneBudget" step="0.01" min="0" value="${window.budgetApp ? window.budgetApp.budgetData.epargne.prevu : 500}" required>
                        <small class="form-help">Recommandé: 10-20% du revenu</small>
                    </div>
                    
                    <!-- Affichage des calculs en temps réel -->
                    <div class="budget-summary">
                        <h4>Résumé du Budget</h4>
                        <div class="summary-item">
                            <span>Total des budgets:</span>
                            <span id="totalBudget">€0</span>
                        </div>
                        <div class="summary-item">
                            <span>Revenu disponible:</span>
                            <span id="availableIncome">€0</span>
                        </div>
                        <div class="summary-item">
                            <span>Solde restant:</span>
                            <span id="remainingBalance">€0</span>
                        </div>
                        <div class="validation-messages" id="validationMessages"></div>
                    </div>
                    
                    <div class="form-actions">
                        <button type="button" class="btn btn-danger" onclick="closeModal(this)">Annuler</button>
                        <button type="button" class="btn btn-secondary" onclick="autoAdjustBudget()">Ajustement Auto</button>
                        <button type="submit" class="btn btn-warning">Sauvegarder</button>
                    </div>
                </form>
            </div>
        </div>
    `;

    addModalStyles();
    document.body.appendChild(modal);

    // Fonction pour calculer et afficher le résumé en temps réel
    function updateBudgetSummary() {
        const revenu = parseFloat(document.getElementById('totalIncome').value) || 0;
        const essentiels = parseFloat(document.getElementById('essentielsBudget').value) || 0;
        const loisirs = parseFloat(document.getElementById('loisirsBudget').value) || 0;
        const epargne = parseFloat(document.getElementById('epargneBudget').value) || 0;
        
        const totalBudget = essentiels + loisirs + epargne;
        const soldeRestant = revenu - totalBudget;
        
        document.getElementById('totalBudget').textContent = `€${totalBudget.toFixed(2)}`;
        document.getElementById('availableIncome').textContent = `€${revenu.toFixed(2)}`;
        document.getElementById('remainingBalance').textContent = `€${soldeRestant.toFixed(2)}`;
        
        // Validation en temps réel
        const validationDiv = document.getElementById('validationMessages');
        validationDiv.innerHTML = '';
        
        if (totalBudget > revenu) {
            const depassement = totalBudget - revenu;
            validationDiv.innerHTML += `<div class="validation-error">⚠️ Budget dépassé de €${depassement.toFixed(2)}</div>`;
        } else if (soldeRestant < 0) {
            validationDiv.innerHTML += `<div class="validation-warning">⚠️ Budget très serré</div>`;
        } else {
            validationDiv.innerHTML += `<div class="validation-success">✅ Budget équilibré</div>`;
        }
        
        // Validation de l'épargne
        const revenuDisponibleApresEssentiels = revenu - essentiels;
        if (epargne > revenuDisponibleApresEssentiels) {
            validationDiv.innerHTML += `<div class="validation-error">⚠️ Épargne excessive (max: €${revenuDisponibleApresEssentiels.toFixed(2)})</div>`;
        }
    }

    // Ajouter les événements de validation en temps réel
    ['totalIncome', 'essentielsBudget', 'loisirsBudget', 'epargneBudget'].forEach(id => {
        document.getElementById(id).addEventListener('input', updateBudgetSummary);
    });

    // Calcul initial
    updateBudgetSummary();

    // Gestion du formulaire
    modal.querySelector('#budgetSettingsForm').addEventListener('submit', (e) => {
        e.preventDefault();
        const loisirsBudget = parseFloat(document.getElementById('loisirsBudget').value);
        const essentielsBudget = parseFloat(document.getElementById('essentielsBudget').value);
        const epargneBudget = parseFloat(document.getElementById('epargneBudget').value);
        const totalIncome = parseFloat(document.getElementById('totalIncome').value);

        // Validation stricte avant sauvegarde
        const totalBudget = essentielsBudget + loisirsBudget + epargneBudget;
        const revenuDisponibleApresEssentiels = totalIncome - essentielsBudget;
        
        let canSave = true;
        let errorMessage = '';

        // Vérification 1: Budget total ne doit pas dépasser les revenus
        if (totalBudget > totalIncome) {
            canSave = false;
            const depassement = totalBudget - totalIncome;
            errorMessage = `Budget total (€${totalBudget.toFixed(2)}) dépasse vos revenus (€${totalIncome.toFixed(2)}) de €${depassement.toFixed(2)}.`;
        }
        
        // Vérification 2: Épargne ne doit pas dépasser le revenu disponible après essentiels
        else if (epargneBudget > revenuDisponibleApresEssentiels) {
            canSave = false;
            errorMessage = `L'épargne (€${epargneBudget.toFixed(2)}) dépasse le revenu disponible après essentiels (€${revenuDisponibleApresEssentiels.toFixed(2)}).`;
        }

        if (!canSave) {
            // Afficher l'erreur et proposer l'ajustement automatique
            const errorDiv = document.createElement('div');
            errorDiv.className = 'validation-error';
            errorDiv.style.marginTop = '15px';
            errorDiv.innerHTML = `
                <strong>❌ Sauvegarde impossible</strong><br>
                ${errorMessage}<br><br>
                <button type="button" class="btn btn-secondary" onclick="autoAdjustBudget()" style="margin-top: 10px;">
                    🔧 Appliquer l'ajustement automatique
                </button>
            `;
            
            // Supprimer l'ancien message d'erreur s'il existe
            const existingError = document.querySelector('.validation-error');
            if (existingError) {
                existingError.remove();
            }
            
            document.getElementById('validationMessages').appendChild(errorDiv);
            
            // Empêcher la sauvegarde
            return false;
        }

        // Si tout est OK, procéder à la sauvegarde
        if (window.budgetApp) {
            window.budgetApp.budgetData.loisirs.prevu = loisirsBudget;
            window.budgetApp.budgetData.essentiels.prevu = essentielsBudget;
            window.budgetApp.budgetData.epargne.prevu = epargneBudget;
            window.budgetApp.budgetData.revenu = totalIncome;
            
            window.budgetApp.calculatePercentages();
            window.budgetApp.updateUI();
            window.budgetApp.updateCharts();
            
            closeModal(modal.querySelector('.modal-close'));
            showSuccessMessage('Paramètres sauvegardés avec succès !');
        }
    });

    setupModalClose(modal);
}

// Fonction pour l'ajustement automatique
function autoAdjustBudget() {
    if (window.budgetApp) {
        const revenu = parseFloat(document.getElementById('totalIncome').value) || 0;
        const essentielsActuel = parseFloat(document.getElementById('essentielsBudget').value) || 0;
        const loisirsActuel = parseFloat(document.getElementById('loisirsBudget').value) || 0;
        const epargneActuel = parseFloat(document.getElementById('epargneBudget').value) || 0;
        
        const totalBudgetActuel = essentielsActuel + loisirsActuel + epargneActuel;
        
        let suggestions = {};
        
        // Cas 1: Budget total dépasse les revenus
        if (totalBudgetActuel > revenu) {
            const reductionNecessaire = totalBudgetActuel - revenu;
            
            // Calculer les réductions proportionnelles
            const reductionLoisirs = (loisirsActuel / totalBudgetActuel) * reductionNecessaire;
            const reductionEssentiels = (essentielsActuel / totalBudgetActuel) * reductionNecessaire;
            const reductionEpargne = (epargneActuel / totalBudgetActuel) * reductionNecessaire;

            suggestions = {
                loisirs: Math.max(0, loisirsActuel - reductionLoisirs),
                essentiels: Math.max(0, essentielsActuel - reductionEssentiels),
                epargne: Math.max(0, epargneActuel - reductionEpargne)
            };
        }
        
        // Cas 2: Épargne dépasse le revenu disponible après essentiels
        else if (epargneActuel > (revenu - essentielsActuel)) {
            suggestions = {
                loisirs: loisirsActuel,
                essentiels: essentielsActuel,
                epargne: Math.max(0, revenu - essentielsActuel)
            };
        }
        
        // Cas 3: Aucun ajustement nécessaire
        else {
            showErrorMessage('Aucun ajustement nécessaire - Budget déjà équilibré');
            return;
        }

        // Appliquer les suggestions
        document.getElementById('loisirsBudget').value = suggestions.loisirs.toFixed(2);
        document.getElementById('essentielsBudget').value = suggestions.essentiels.toFixed(2);
        document.getElementById('epargneBudget').value = suggestions.epargne.toFixed(2);
        
        // Déclencher la mise à jour du résumé
        document.getElementById('loisirsBudget').dispatchEvent(new Event('input'));
        
        // Supprimer le message d'erreur précédent
        const existingError = document.querySelector('.validation-error');
        if (existingError) {
            existingError.remove();
        }
        
        // Afficher un message de succès
        const successDiv = document.createElement('div');
        successDiv.className = 'validation-success';
        successDiv.style.marginTop = '15px';
        successDiv.innerHTML = `
            <strong>✅ Ajustement automatique appliqué</strong><br>
            Les budgets ont été ajustés pour respecter la cohérence budgétaire.
        `;
        
        document.getElementById('validationMessages').appendChild(successDiv);
        
        // Supprimer le message de succès après 3 secondes
        setTimeout(() => {
            if (successDiv.parentNode) {
                successDiv.parentNode.removeChild(successDiv);
            }
        }, 3000);
        
        showSuccessMessage('Ajustement automatique appliqué ! Vous pouvez maintenant sauvegarder.');
    }
}

function addModalStyles() {
    if (document.getElementById('modal-styles')) return;
    
    const style = document.createElement('style');
    style.id = 'modal-styles';
    style.textContent = `
        .modal-overlay {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 1000;
            animation: fadeIn 0.3s ease;
        }
        
        .modal-content {
            background: white;
            border-radius: 16px;
            padding: 30px;
            max-width: 500px;
            width: 90%;
            max-height: 80vh;
            overflow-y: auto;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
            animation: slideInUp 0.3s ease;
        }
        
        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            padding-bottom: 15px;
            border-bottom: 1px solid #e0e0e0;
        }
        
        .modal-close {
            background: none;
            border: none;
            font-size: 24px;
            cursor: pointer;
            color: #666;
            transition: color 0.3s ease;
        }
        
        .modal-close:hover {
            color: #f5576c;
        }
        
        .form-group {
            margin-bottom: 20px;
        }
        
        .form-group label {
            display: block;
            margin-bottom: 8px;
            font-weight: 600;
            color: #333;
        }
        
        .form-group input,
        .form-group select {
            width: 100%;
            padding: 12px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            font-size: 16px;
            transition: border-color 0.3s ease;
        }
        
        .form-group input:focus,
        .form-group select:focus {
            outline: none;
            border-color: #667eea;
        }
        
        .form-actions {
            display: flex;
            gap: 15px;
            justify-content: flex-end;
            margin-top: 30px;
        }
        
        .form-help {
            display: block;
            margin-top: 5px;
            font-size: 12px;
            color: #666;
            font-style: italic;
        }
        
        .budget-summary {
            background: #f8f9fa;
            border-radius: 8px;
            padding: 15px;
            margin: 20px 0;
        }
        
        .budget-summary h4 {
            margin: 0 0 15px 0;
            color: #333;
            font-size: 16px;
        }
        
        .summary-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
            padding: 5px 0;
            border-bottom: 1px solid #e0e0e0;
        }
        
        .summary-item:last-child {
            border-bottom: none;
            font-weight: bold;
            font-size: 16px;
        }
        
        .validation-messages {
            margin-top: 15px;
        }
        
        .validation-error {
            background: #f8d7da;
            color: #721c24;
            padding: 8px 12px;
            border-radius: 6px;
            margin-bottom: 8px;
            border-left: 4px solid #dc3545;
        }
        
        .validation-warning {
            background: #fff3cd;
            color: #856404;
            padding: 8px 12px;
            border-radius: 6px;
            margin-bottom: 8px;
            border-left: 4px solid #ffc107;
        }
        
        .validation-success {
            background: #d4edda;
            color: #155724;
            padding: 8px 12px;
            border-radius: 6px;
            margin-bottom: 8px;
            border-left: 4px solid #28a745;
        }
        
        .btn-secondary {
            background: var(--gradient-primary);
        }
        
        @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }
        
        @keyframes slideInUp {
            from { 
                opacity: 0;
                transform: translateY(30px);
            }
            to { 
                opacity: 1;
                transform: translateY(0);
            }
        }
    `;
    document.head.appendChild(style);
}

function setupModalClose(modal) {
    const closeBtn = modal.querySelector('.modal-close');
    closeBtn.addEventListener('click', () => closeModal(closeBtn));
    
    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal(closeBtn);
    });
}

function closeModal(closeBtn) {
    const modal = closeBtn.closest('.modal-overlay');
    modal.style.animation = 'fadeOut 0.3s ease';
    setTimeout(() => {
        document.body.removeChild(modal);
    }, 300);
}

function showSuccessMessage(message) {
    showToast(message, 'success');
}

function showErrorMessage(message) {
    showToast(message, 'error');
}

function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    
    const style = document.createElement('style');
    style.textContent = `
        .toast {
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 15px 20px;
            border-radius: 8px;
            color: white;
            font-weight: 600;
            z-index: 1001;
            animation: slideInRight 0.3s ease;
        }
        
        .toast-success {
            background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
        }
        
        .toast-error {
            background: linear-gradient(135deg, #f5576c 0%, #f093fb 100%);
        }
        
        @keyframes slideInRight {
            from { 
                opacity: 0;
                transform: translateX(100%);
            }
            to { 
                opacity: 1;
                transform: translateX(0);
            }
        }
    `;
    
    document.head.appendChild(style);
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideOutRight 0.3s ease';
        setTimeout(() => {
            document.body.removeChild(toast);
            document.head.removeChild(style);
        }, 300);
    }, 3000);
}
