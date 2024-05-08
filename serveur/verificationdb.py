import sqlite3

# Connexion à la base de données
connection = sqlite3.connect('music_database.db')
cursor = connection.cursor()

# Exécution de la requête SELECT pour récupérer les enregistrements ajoutés
cursor.execute("SELECT * FROM musics")

# Récupération des résultats de la requête
rows = cursor.fetchall()

# Affichage des enregistrements
for row in rows:
    print(row)

# Fermeture du curseur et de la connexion
cursor.close()
connection.close()
