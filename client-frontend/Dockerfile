FROM node:16-alpine
RUN npm install -g http-server
WORKDIR /app
COPY package*.json ./
RUN npm ci --legacy-peer-deps --registry=https://registry.npmjs.org/
COPY . .
RUN npm run build
CMD [ "http-server", "dist" ]
