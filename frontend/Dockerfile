FROM node:20-alpine AS build
WORKDIR /app
ENV VITE_GOOGLE_CLIENT_ID=1045426516028-eq3vj3md30paejguu8kdatn7k7pugk76.apps.googleusercontent.com
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
