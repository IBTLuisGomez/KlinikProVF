# Build de producción del frontend (React + Vite + TypeScript).
# No verificado en este entorno (no hay Docker disponible aquí) — pendiente
# de correr `docker compose build web` del lado de Fer para confirmarlo.

FROM node:20-alpine AS build
WORKDIR /app

# ARG en tiempo de build: Vite incrusta VITE_API_BASE_URL en el bundle,
# así que debe conocerse antes de "npm run build" (no es una env var de runtime).
ARG VITE_API_BASE_URL=http://localhost:8081
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
