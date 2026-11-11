FROM node:24-alpine AS build
WORKDIR /app/frontend
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
COPY packages /app/packages
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund
COPY frontend/ .
RUN npm run build

FROM nginx:1.28-alpine
COPY frontend/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY frontend/docker-entrypoint.sh /docker-entrypoint-custom.sh
RUN chmod +x /docker-entrypoint-custom.sh
COPY --from=build /app/frontend/dist /usr/share/nginx/html
EXPOSE 80
ENTRYPOINT ["/docker-entrypoint-custom.sh"]
