# ---------- Stage 1: build the dashboard ----------
FROM node:22-alpine AS build
WORKDIR /app

# Install libraries first (Docker remembers this step).
COPY package.json package-lock.json ./
RUN npm ci

# Copy the code and build it into plain files in /app/dist.
COPY . .
RUN npm run build

# ---------- Stage 2: nginx serves the files ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80