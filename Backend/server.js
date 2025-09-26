const { Client } = require('pg');

// Configuration de la connexion
const client = new Client({
  user: "postgres",       // ton utilisateur
  host: "localhost",      // car c'est en local
  database: "you",  // ta base créée
  password: "chooseyourhistory",       // ton mot de passe défini à l'installation
  port: 5432              // port par défaut
});

// Connexion
client.connect()
  .then(() => console.log("✅ Connecté à PostgreSQL"))
  .catch(err => console.error("❌ Erreur de connexion", err.stack));

// Exemple de requête
client.query('SELECT NOW()', (err, res) => {
  if (err) {
    console.error(err);
  } else {
    console.log("📅 Heure actuelle :", res.rows[0]);
  }
  client.end();
});
