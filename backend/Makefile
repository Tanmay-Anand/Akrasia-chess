.PHONY: run build test infra-up infra-down infra-reset

run:
	./mvnw spring-boot:run

build:
	./mvnw clean install -DskipTests

test:
	./mvnw test

infra-up:
	cd infra/dev-postgresql && docker compose up -d

infra-down:
	cd infra/dev-postgresql && docker compose down

infra-reset:
	cd infra/dev-postgresql && docker compose down -v
