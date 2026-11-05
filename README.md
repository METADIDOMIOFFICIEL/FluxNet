# FluxNet

FluxNet est une application Android de suivi de la consommation réseau et de gestion des applications et forfaits mobiles.

## Fonctionnalités

- Suivi de la consommation réseau et de la qualité de connexion.
- Consultation de l'utilisation réseau par application.
- Gestion des forfaits mobiles et comparaison des offres.
- Règles de contrôle réseau par application via un VPN local.
- Alertes et services de surveillance en arrière-plan.
- Espace communautaire pour consulter et publier des informations.

Certaines fonctions nécessitent des autorisations Android spécifiques et doivent être activées par l'utilisateur dans les paramètres de l'appareil.

## Prérequis

- Android Studio récent ou Gradle **8.14.3**.
- JDK **17** pour Gradle et Android Gradle Plugin.
- Android SDK Platform **36**.
- Un appareil ou émulateur Android 7.0 (API 24) ou ultérieur.

## Ouvrir et lancer l'application

1. Ouvrir le dossier du projet dans Android Studio.
2. Installer la plateforme Android SDK 36 si elle n'est pas déjà présente.
3. Synchroniser le projet Gradle.
4. Lancer la configuration `app` sur un appareil ou un émulateur.

Pour compiler l'APK debug avec une installation locale de Gradle 8.14.3 :

```sh
gradle :app:assembleDebug
```

L'APK est généré dans `app/build/outputs/apk/debug/`.

### Builder Metadidomi

Pour compiler l'application avec le builder officiel, consultez le dépôt [METADIDOMI/Metadidomi-Builder](https://github.com/METADIDOMIOFFICIEL/Metadidomi-Builder) et suivez ses instructions d'installation et d'utilisation. Le builder n'est pas inclus dans ce dépôt. Pour une publication sur Google Play, utilisez votre propre clé de signature de publication.

## Permissions et confidentialité

Selon les fonctions utilisées, Android peut demander l'accès aux statistiques d'utilisation, l'autorisation de créer une connexion VPN locale, l'affichage de notifications ou l'activation d'un administrateur de l'appareil. Ces accès sont gérés par Android et doivent être accordés explicitement par l'utilisateur.

Ne publiez jamais de fichier de signature, de mot de passe ou de configuration locale dans le dépôt.

## Licence

Le code source de FluxNet est distribué sous licence [Apache-2.0](./LICENSE), avec attribution à **METADIDOMI**. Lors de toute redistribution du code, conservez la mention de copyright « Copyright 2026 METADIDOMI » et joignez le fichier `LICENSE`; les fichiers modifiés doivent aussi signaler les changements. La licence autorise l'utilisation, la modification et la redistribution, y compris commerciale, et inclut une concession de brevets des contributeurs.

Cette licence ne remplace pas les licences propres aux dépendances tierces, polices, icônes ou autres ressources incluses. Vérifiez leurs conditions avant toute redistribution. Le nom et la marque FluxNet ne sont pas concédés par cette licence.
