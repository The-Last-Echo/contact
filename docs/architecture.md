# Décisions techniques initiales

## Architecture

Le projet commence avec un seul module Android et des packages séparés par
responsabilité (`data`, `preferences`, `ui`, `ui.theme`). Cela garde les
frontières explicites sans introduire de modules Gradle avant qu’ils apportent
une réelle indépendance. Compose présente l’interface, un ViewModel expose son
état, et les dépôts isolent les API de stockage.

Le socle est validé avec AGP 8.13.2 et compileSdk 36. Certaines versions AndroidX
plus récentes proposées par lint imposent déjà compileSdk 37 et AGP 9.1 ; les
dépendances UI restent donc sur la dernière combinaison effectivement compilée
avec le SDK disponible. La mise à niveau du SDK et du plugin sera coordonnée
plutôt que de masquer ces exigences.

## Sources et données

Les fiches existantes sont lues à la demande depuis Android Contacts Provider.
L’application ne les copie pas dans Room et ne demande pas le droit de les
modifier. Room contient uniquement les métadonnées personnelles de l’application
(favoris, surnoms, futurs commentaires privés), associées à la clé de recherche
du contact Android. DataStore conserve les préférences d’apparence.

Seule `READ_CONTACTS` est déclarée. L’autorisation est demandée après l’action
explicite du bouton d’accès aux contacts. Aucun accès réseau, compte, service de
synchronisation ou SDK de mesure n’est prévu.

## Chiffrement au repos

Room est ouverte avec SQLCipher. Un secret aléatoire de 256 bits chiffre la base ;
Android Keystore protège ce secret au moyen d’une clé AES-GCM non exportable.
Le secret enveloppé réside dans le stockage privé exclu des sauvegardes Android.
Si le secret enveloppé ou la clé Keystore est perdu ou invalide, l’application
doit signaler l’impossibilité de déchiffrer les données : elle ne doit ni
recréer silencieusement une base vide ni basculer vers une base en clair.

Le chiffrement ne protège pas contre un appareil compromis pendant que
l’application est déverrouillée. La sauvegarde/restauration chiffrée et la
rotation de clé devront préserver cette frontière avant d’être ajoutées.

## Interface

Les captures de GymMane privilégient un fond très sombre, des surfaces en
couches et un accent chaud cuivre ; celles de Streak utilisent aussi un fond
sombre, avec des accents violets plus vifs, des cartes arrondies et des repères
visuels francs. Les deux projets annoncent une licence GPL-3.0 ; GymMane précise
en outre des conditions supplémentaires et des licences d’illustrations
distinctes. Aucun code, visuel ou autre ressource de ces projets n’est repris.

La direction proposée pour Contacts conserve leur contraste, leur hiérarchie
simple et leurs cartes aérées sans reprendre leur identité : fonds neutres
légèrement chauds, vert sauge discret et accent terre cuite, en clair comme en
sombre. La palette du prototype est une base de discussion, pas un choix figé.

## Première version

L’accueil affiche les favoris au-dessus de la liste complète, propose une
recherche locale intégrée à l’en-tête, permet de modifier les favoris et offre
le choix du thème système, clair ou sombre. L’accueil utilise une seule liste
verticale virtualisée avec des clés stables ; les favoris défilent horizontalement
dans des tuiles de dimensions constantes. Le filtrage local est effectué hors
du thread UI. Les appels,
l’édition des fiches, les photos et les autres sources ne sont pas encore
présentés comme des fonctions disponibles.
