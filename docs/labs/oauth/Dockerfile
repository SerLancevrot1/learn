FROM node:22-alpine
WORKDIR /lab
RUN npm install --no-save --no-audit --no-fund jose@6.2.12
COPY api.mjs ./
USER node
CMD ["node", "api.mjs"]
