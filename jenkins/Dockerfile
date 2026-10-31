# Jenkins with everything the SiteWatch pipeline needs: Maven, Docker CLI and the Docker Compose plugin.
FROM jenkins/jenkins:lts-jdk17
USER root
RUN apt-get update \
 && apt-get install -y --no-install-recommends docker.io maven curl \
 && rm -rf /var/lib/apt/lists/* \
 && mkdir -p /usr/local/lib/docker/cli-plugins \
 && curl -fsSL "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-$(uname -m)" \
      -o /usr/local/lib/docker/cli-plugins/docker-compose \
 && chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
# Stays root so Jenkins can use the host Docker socket (fine for a lab, not for production).
