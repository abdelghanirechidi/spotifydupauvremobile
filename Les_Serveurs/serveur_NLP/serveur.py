from fastapi import FastAPI
from pydantic import BaseModel

#uvicorn serveur:app --host 0.0.0.0 --port 5000 --reload

app = FastAPI()

class Transcript(BaseModel):
    transcript: str

action_synonyms = {
    'jouer': ['joue', 'lance', 'jouer', 'stream', 'met'],
    'supprimer': ['supprime', 'enlève', 'efface', 'retire', 'enleve']
}

def analyze_request(transcript: str):
    if transcript:
        action, music_name = detect_action_and_music(transcript.lower())
        if action:
            if music_name:
                return f"action: {action}, Music: {music_name}"
            else:
                return f"action: {action}, but no music name found"
        else:
            return "Action not detected"
    else:
        return "Transcript data missing"

def detect_action_and_music(request):
    for action, synonyms in action_synonyms.items():
        for synonym in synonyms:
            if synonym in request:
                music_name = extract_music_name(request, synonym)
                return action, music_name
    return None, None

def extract_music_name(request, action_keyword):
    # Liste des mots-clés pour séparer le nom de l'artiste
    artist_keywords = [" de ", " par ", " fait ", " feat "]

    # Recherche de l'index de l'action dans la requête
    index_action = request.find(action_keyword)
    if index_action != -1:
        # Recherche des indices des mots-clés après l'index de l'action
        indices_keywords = [request.rfind(keyword, 0, index_action) for keyword in artist_keywords]
        indices_keywords = [index for index in indices_keywords if index != -1]

        # Si des indices sont trouvés, le nom de la musique va jusqu'au dernier index
        if indices_keywords:
            index_music_end = max(indices_keywords)
            artist_name = request[index_music_end:].strip()
            music_name = request[index_action + len(action_keyword):index_music_end].strip()
        # Sinon, il n'y a pas de mot-clé d'artiste, donc on utilise tout le reste de la requête comme nom de musique
        else:
            # Recherche de la dernière occurrence du mot-clé "de" dans la requête
            last_de_index = request.rfind(" de ")
            if last_de_index != -1:
                artist_name = request[last_de_index + len(" de "):].strip()
                music_name = request[index_action + len(action_keyword):last_de_index].strip()
            else:
                music_name = request[index_action + len(action_keyword):].strip()
                artist_name = "Inconnu"

        return music_name, artist_name

    return None, None

@app.post("/analyze")
async def handle_analyze_request(transcript_data: Transcript):
    transcript = transcript_data.transcript
    print("Received request:", transcript_data)  # Ajoutez cette ligne pour vérifier si le serveur reçoit la demande
    response = analyze_request(transcript)
    print("Sending response:", response)  # Ajoutez cette ligne pour vérifier si le serveur envoie une réponse
    return {"response": response}
