# DBILDUNGS-IAM-KEYCLOAK-PROVIDERS

This repository is designed for creating custom **providers** for our **dbildungs-iam-keycloak** instance. It serves as the foundation for extending Keycloak's functionality to meet our specific requirements, such as custom protocol mappers, event listeners, and other extensions.

## Lokales Setup

Voraussetzung: [Maven](https://maven.apache.org/) (z.B. via `brew install maven`) und Docker.

Um lokale Änderungen an den Providern in einem über `dbildungs-iam-server/compose.yaml` gestarteten Keycloak zu testen:

1. Provider-JAR bauen:
    ```bash
    mvn clean package
    ```
    Ergebnis: `target/keycloak-providers-1.8.jar` (durch das `maven-shade-plugin` bereits als Fat-JAR mit allen Abhängigkeiten gebündelt).

2. JAR ins Keycloak-Image-Projekt kopieren (ersetzt die dort liegende, ältere Version):
    ```bash
    cp target/keycloak-providers-1.8.jar ../dbildungs-iam-keycloak/src/providers/keycloak-providers-1.8.jar
    ```

3. Lokales Dev-Image von `dbildungs-iam-keycloak` neu bauen (kopiert `src/providers/` in `/opt/keycloak/providers/` und führt `kc.sh build` aus):
    ```bash
    cd ../dbildungs-iam-keycloak
    chmod +x build-dev.sh
    ./build-dev.sh
    ```
    Das taggt das Image als `ghcr.io/dbildungsplattform/dbildungs-iam-keycloak:latest` — denselben Namen, den `dbildungs-iam-server/compose.yaml` referenziert. Docker Compose pullt nicht neu, solange lokal ein Image mit diesem Tag existiert.

4. Keycloak-Container in `dbildungs-iam-server` neu erstellen (ein reiner Restart reicht nicht, der Container muss neu erzeugt werden, um das neue Image zu verwenden):
    ```bash
    cd ../dbildungs-iam-server
    docker compose rm -sf keycloak
    docker compose --profile third-party up -d keycloak
    ```

Die Admin-Konsole ist danach unter [http://localhost:8080/admin/master/console/](http://localhost:8080/admin/master/console/) erreichbar (Login `admin`/`admin`).
