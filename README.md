# Contacts

Application Android de gestion de contacts, pensée pour fonctionner hors ligne.
La première version affiche les contacts du téléphone et enregistre localement
les favoris et préférences d’apparence.

## Confidentialité

- Le manifeste ne déclare pas `INTERNET` ; seul l’accès en lecture aux contacts
  est demandé, au moment où l’utilisateur choisit de l’accorder.
- Les fiches restent dans Android Contacts Provider. Room ne conserve que des
  métadonnées propres à l’application, dans une base chiffrée avec SQLCipher.
- La clé de base est aléatoire et protégée par Android Keystore. Les fichiers de
  données ne sont pas inclus dans les sauvegardes Android.
- Aucun SDK d’analytics, publicité ou rapport de crash n’est intégré.
- `./gradlew check` exécute `verifyPrivacyBoundary`, qui vérifie le manifeste
  fusionné et les dépendances d’exécution.

## Construire

Ouvrir le projet dans Android Studio ou lancer `./gradlew check` puis
`./gradlew assembleDebug`.

## Architecture

Voir [docs/architecture.md](docs/architecture.md) pour les choix de stockage,
les limites de la première version et la direction visuelle provisoire.
