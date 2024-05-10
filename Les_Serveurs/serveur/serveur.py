import sys
import Ice
import MusicIce
import sqlite3
import os
import base64
import vlc
from queue import Queue
from threading import Thread

class MusicI(MusicIce.Music):

    # Fonction d'initialisation
    def __init__(self):
        super().__init__()
        self.queue = Queue()
        self.worker_thread = Thread(target=self.process_queue)
        self.worker_thread.start()
        self.connection = None
        self.cursor = None
        self.instance = vlc.Instance()
        self.player = self.instance.media_player_new()

    # Fonction permettant de se connecter à la base de données
    def process_queue(self):
        try:
            self.connection = sqlite3.connect('music_database.db')
            self.cursor = self.connection.cursor()
            self.create_music_table()
            while True:
                task = self.queue.get()
                if task is None:
                    break
                task()
                self.connection.commit()
        except sqlite3.Error as e:
            print("Erreur lors de l'initialisation de la base de données :", e)
        finally:
            if self.cursor:
                self.cursor.close()
            if self.connection:
                self.connection.close()

    # Fonction permettant de se connecter à la base de données
    def create_music_table(self):
        self.cursor.execute('''CREATE TABLE IF NOT EXISTS musics 
                               (id INTEGER PRIMARY KEY AUTOINCREMENT, 
                                title TEXT, 
                                author TEXT, 
                                audio_file TEXT)''')
        
    # Fonction permettant d'ajouter une musique
    def ajoutMusique(self, titre, auteur, fichierAudio, current=None):
        print(f"Ajout de musique : {titre} par {auteur}, fichier audio : {fichierAudio}")
        def task():
            try:
                self.cursor.execute("SELECT COUNT(*) FROM musics WHERE title = ? AND author = ? AND audio_file = ?",
                                    (titre, auteur, fichierAudio))
                count = self.cursor.fetchone()[0]
                if count == 0:
                    self.cursor.execute("INSERT INTO musics (title, author, audio_file) VALUES (?, ?, ?)", 
                                        (titre, auteur, fichierAudio))
                else:
                    print("La musique existe déjà dans la base de données.")
            except sqlite3.Error as e:
                print("Erreur lors de l'ajout de la musique :", e)
        self.execute_task(task)
        return True


    # Fonction permettant de supprimer une musique
    def supprimerMusique(self, titre, current=None):
        print(f"Suppression de musique : {titre}")
        def task():
            try:
                # Récupérer le chemin du fichier depuis la base de données
                self.cursor.execute("SELECT * FROM musics WHERE LOWER(title) = LOWER(?)", (titre,))
                result = self.cursor.fetchone()
                if result:
                    chemin = result[3]
                    # Supprimer la musique de la base de données
                    self.cursor.execute("DELETE FROM musics WHERE LOWER(title) = LOWER(?)", (titre,))
                    self.connection.commit()

                    # Supprimer le fichier physique
                    if os.path.exists(chemin):
                        os.remove(chemin)
                        print("Fichier physique supprimé :", chemin)
                    else:
                        print("Le fichier physique n'existe pas :", chemin)
                else:
                    print("La musique n'existe pas dans la base de données.")
            except sqlite3.Error as e:
                print("Erreur lors de la suppression de la musique :", e)
        self.execute_task(task)
        return True

    
    # Fonction permettant de modifier une musique dans la base de données
    def modifierMusique(self, titre, nouveauTitre, nouvelAuteur, nouveauFichierAudio, current=None):
        print(f"Modification de musique : {titre} -> {nouveauTitre}")
        def task():
            try:
                self.cursor.execute("SELECT * FROM musics WHERE LOWER(title) = LOWER(?)", (titre,))
                result = self.cursor.fetchone()
                ancienCheminFichier = result[3]
                nouveauCheminFichier = "../disque_dur_serveur/" + nouveauFichierAudio
                if os.path.exists(ancienCheminFichier):
                    try:
                        os.rename(ancienCheminFichier, nouveauCheminFichier)
                        self.cursor.execute("UPDATE musics SET title = ?, author = ?, audio_file = ? WHERE title = ?",
                                            (nouveauTitre, nouvelAuteur, nouveauCheminFichier, titre))
                        self.connection.commit()
                        print("Musique modifiée avec succès !")
                        return True
                    except sqlite3.Error as e:
                        print("Erreur lors de la modification de la musique dans la base de données :", e)
                        return False
                    except OSError as e:
                        print("Erreur lors de la modification du fichier audio :", e)
                        return False
                else:
                    print("Le fichier audio correspondant à la musique n'a pas été trouvé.")
                    return False

            except sqlite3.Error as e:
                print("Erreur lors de la suppression de la musique :", e)
        self.execute_task(task)

    
    # Fonction permettant d'envoyer une musique
    def envoyerMusique(self, nomFichier, auteur,  chunk, current=None):
        chemin_sauvegarde = "../disque_dur_serveur/" + nomFichier + "_" + auteur + ".mp3"
        with open(chemin_sauvegarde, "ab") as fichier:
            fichier.write(base64.b64decode(chunk))
            #fichier.write(chunk.encode("latin1"))
        self.ajoutMusique(nomFichier, auteur, "../disque_dur_serveur/" + nomFichier + "_" + auteur + ".mp3")
        print(f"Chunk de fichier audio {nomFichier} reçu et sauvegardé avec succès.")

    # Fonction permettant de rechercher une musique par son titre
    def rechercherParTitre(self, titre, current=None):
        def task():
            try:
                self.cursor.execute("SELECT * FROM musics WHERE title = ?", (titre,))
                result = self.cursor.fetchall()
                for item in result:
                    print(f"{item[1]} - {item[2]} - {item[3]}")
            except sqlite3.Error as e:
                print("Erreur lors de la recherche par titre :", e)

        self.execute_task(task)

    # Fonction permettant de rechercher une musique par son auteur
    def rechercherParAuteur(self, auteur, current=None):
        def task():
            try:
                self.cursor.execute("SELECT * FROM musics WHERE author = ?", (auteur,))
                result = self.cursor.fetchall()
                for item in result:
                    print(f"{item[1]} - {item[2]} - {item[3]}")
            except sqlite3.Error as e:
                print("Erreur lors de la recherche par auteur :", e)

        self.execute_task(task)

    # Fonction permettant de jouer une musique
    def play(self, titre, current=None):
        print(f"Lecture de la musique : {titre}")
        def task():
            try:
                self.stop()
                # Recherche de la musique avec une correspondance insensible à la casse
                self.cursor.execute("SELECT * FROM musics WHERE LOWER(title) LIKE ?", ('%' + titre.lower() + '%',))
                result = self.cursor.fetchone()

                if result:
                    id, title, author, audio_file = result
                    print(f"Musique trouvée : {title} - {author} - {audio_file}")

                    # Charger le flux audio
                    media = self.instance.media_new(audio_file)

                    # Configurer le flux audio
                    media.add_option(f":sout=#transcode{{acodec=mp3,ab=128,channels=2,samplerate=44100}}:http{{mux=mp3,dst=:8080/stream.mp3}}")
                    self.player.set_media(media)

                    # Démarrer la lecture
                    self.player.play()

                else:
                    # Convertir le titre transcrit en un format compatible avec la recherche dans la base de données
                    titre_formate = titre.lower().replace(" ", "")

                    # Recherche dans la base de données avec le titre formaté
                    self.cursor.execute("SELECT * FROM musics WHERE REPLACE(LOWER(title), ' ', '') LIKE ?", ('%' + titre_formate + '%',))
                    result = self.cursor.fetchone()

                    if result:
                        id, title, author, audio_file = result
                        print(f"Musique trouvée : {title} - {author} - {audio_file}")

                        # Charger le flux audio
                        media = self.instance.media_new(audio_file)

                        # Configurer le flux audio
                        media.add_option(f":sout=#transcode{{acodec=mp3,ab=128,channels=2,samplerate=44100}}:http{{mux=mp3,dst=:8080/stream.mp3}}")
                        self.player.set_media(media)

                        # Démarrer la lecture
                        self.player.play()

                    else:
                        print(f"Aucune musique trouvée avec le titre : {titre}")

            except sqlite3.Error as e:
                print("Erreur lors de la lecture de la musique :", e)
        self.execute_task(task)
        return True



    # Fonction permettant d'arrêter la lecture de la musique
    def stop(self, current=None):
        print("Arrêt de la lecture de la musique")
        try:
            self.player.stop()
            return True
        except Exception as e:
            print("Erreur lors de l'arrêt de la lecture de la musique :", e)
            return False

    # Fonction permettant de retourner la liste des musiques disponibles
    def listerMusiques(self, current=None):
        musiques = []
        try:
            # Connexion à la base de données
            connection = sqlite3.connect('music_database.db')
            cursor = connection.cursor()

            # Exécution de la requête pour obtenir la liste des musiques
            cursor.execute("SELECT title FROM musics")
            result = cursor.fetchall()

            # Ajout de chaque titre de musique à la liste
            for item in result:
                musiques.append(item[0])

        except sqlite3.Error as e:
            print("Erreur lors de la récupération de la liste des musiques :", e)

        finally:
            # Fermeture de la connexion à la base de données
            if cursor:
                cursor.close()
            if connection:
                connection.close()

        return musiques


    # Fonction permettant d'ajouter une tache dans la queue
    def execute_task(self, task):
        self.queue.put(task)

with Ice.initialize(sys.argv) as communicator:
    adapter = communicator.createObjectAdapterWithEndpoints("MusicServiceAdapter", "tcp -p 10000")
    object = MusicI()
    proxy = adapter.addWithUUID(object)
    #print(proxy)
    adapter.add(object, communicator.stringToIdentity("MusicService"))
    adapter.activate()
    communicator.waitForShutdown()
    object.queue.put(None)
    object.worker_thread.join()