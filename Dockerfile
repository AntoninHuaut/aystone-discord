FROM golang:1.25-alpine AS build
WORKDIR /app

RUN apk add --no-cache git

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -ldflags="-w -s" -o aystone-discord .

FROM alpine:3.23
WORKDIR /app

RUN apk add --no-cache ca-certificates tzdata && \
    addgroup -S aystonediscord && \
    adduser -S aystonediscord -G aystonediscord

COPY --from=build /app/aystone-discord ./aystone-discord

RUN chown -R aystonediscord:aystonediscord /app

USER aystonediscord

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD pidof aystone-discord || exit 1

ENTRYPOINT ["./aystone-discord"]