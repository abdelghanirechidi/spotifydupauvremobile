module MusicIce {
    sequence<string> listes;
    interface Music {
        bool ajoutMusique(string titre, string auteur, string fichierAudio);
        bool supprimerMusique(string titre);
        bool modifierMusique(string titre, string nouveauTitre, string nouvelAuteur, string nouveauFichierAudio);
        void envoyerMusique(string titre, string auteur, string chunk);
        idempotent void rechercherParTitre(string titre);
        idempotent void rechercherParAuteur(string auteur);
        bool play(string titre);
        bool stop();
	listes listerMusiques();
    };
};
