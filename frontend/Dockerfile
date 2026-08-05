# ===== Build stage: build React (Vite) bằng Bun =====
FROM oven/bun:1 AS build
WORKDIR /app

# Cài dependency trước (cache theo lockfile).
# Playwright chỉ dùng cho script chụp màn hình (shoot.mjs) — chặn tải browser hàng trăm MB khi build image.
COPY package.json bun.lock ./
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
RUN bun install --frozen-lockfile

# Build ra thư mục dist/
COPY . .
RUN bun run build

# ===== Runtime stage: nginx phục vụ static + proxy /api sang backend =====
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
