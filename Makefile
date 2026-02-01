.PHONY: help build up down logs ps restart clean test init-topics

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-15s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: ## Build all Docker images
	docker-compose build

up: ## Start all services
	docker-compose up -d
	@echo "Waiting for services to be ready..."
	@sleep 10
	@make init-topics

down: ## Stop all services
	docker-compose down

logs: ## View logs from all services
	docker-compose logs -f

ps: ## Show status of all services
	docker-compose ps

restart: ## Restart all services
	docker-compose restart

clean: ## Stop services and remove volumes (⚠️ deletes data)
	docker-compose down -v
	docker system prune -f

rebuild: ## Complete clean rebuild (stops, removes volumes, rebuilds without cache, starts)
	@echo "Performing complete clean rebuild..."
	docker-compose down -v
	@echo "Removing old EDRS images..."
	@docker images --format "{{.Repository}}:{{.Tag}}" | grep "edrs-" | xargs -r docker rmi -f || true
	docker system prune -f
	docker-compose build --no-cache
	docker-compose up -d
	@echo "Waiting for services to start..."
	@sleep 10
	@make health

test: ## Run tests
	mvn test

init-topics: ## Initialize Kafka topics
	@chmod +x scripts/init-kafka-topics.sh
	@./scripts/init-kafka-topics.sh

health: ## Check health of all services
	@echo "Checking service health..."
	@curl -s http://localhost:8080/actuator/health | jq . || echo "Reservation Service: Not ready"
	@curl -s http://localhost:8081/actuator/health | jq . || echo "Inventory Service: Not ready"
	@curl -s http://localhost:8082/actuator/health | jq . || echo "Notification Service: Not ready"
	@curl -s http://localhost:8083/actuator/health | jq . || echo "Logging Service: Not ready"
	@curl -s http://localhost:8084/actuator/health | jq . || echo "Persistence Service: Not ready"

build-service: ## Build a specific service (usage: make build-service SERVICE=reservation-service)
	docker-compose build $(SERVICE)

logs-service: ## View logs for a specific service (usage: make logs-service SERVICE=reservation-service)
	docker-compose logs -f $(SERVICE)

restart-service: ## Restart a specific service (usage: make restart-service SERVICE=reservation-service)
	docker-compose restart $(SERVICE)
