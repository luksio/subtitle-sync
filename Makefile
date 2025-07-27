# Makefile for Subtitle Sync Application
# Project: subtitle-sync
# Language: Java with Maven

# Variables
APP_NAME = subtitle-sync
MAIN_CLASS = app.SubtitleSyncApp
MAVEN_OPTS = -Dmaven.test.failure.ignore=false
JAR_NAME = $(APP_NAME)-1.0-SNAPSHOT.jar
TARGET_DIR = target
CLASSES_DIR = $(TARGET_DIR)/classes
TEST_CLASSES_DIR = $(TARGET_DIR)/test-classes

# Colors for output
GREEN = \033[0;32m
YELLOW = \033[1;33m
RED = \033[0;31m
NC = \033[0m # No Color

.PHONY: help build clean compile test run package install dev-setup format check coverage deps-tree deps-update git-status docker-build docker-run

# Default target
help: ## Show this help message
	@echo "$(GREEN)Available targets for $(APP_NAME):$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-20s$(NC) %s\n", $$1, $$2}'
	@echo ""
	@echo "$(GREEN)Examples:$(NC)"
	@echo "  make build     # Compile and package the application"
	@echo "  make test      # Run all tests"
	@echo "  make run       # Start the GUI application"
	@echo "  make clean     # Clean all build artifacts"

# Build lifecycle
clean: ## Clean all build artifacts and target directory
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	mvn clean
	@echo "$(GREEN)✓ Clean completed$(NC)"

compile: ## Compile main sources
	@echo "$(YELLOW)Compiling sources...$(NC)"
	mvn compile
	@echo "$(GREEN)✓ Compilation completed$(NC)"

test-compile: ## Compile test sources
	@echo "$(YELLOW)Compiling test sources...$(NC)"
	mvn test-compile
	@echo "$(GREEN)✓ Test compilation completed$(NC)"

test: ## Run all tests (Spock)
	@echo "$(YELLOW)Running tests...$(NC)"
	mvn test
	@echo "$(GREEN)✓ Tests completed$(NC)"

test-verbose: ## Run tests with verbose output
	@echo "$(YELLOW)Running tests with verbose output...$(NC)"
	mvn test -X
	@echo "$(GREEN)✓ Verbose tests completed$(NC)"

package: ## Create JAR package
	@echo "$(YELLOW)Creating JAR package...$(NC)"
	mvn package -DskipTests
	@echo "$(GREEN)✓ Package created: $(TARGET_DIR)/$(JAR_NAME)$(NC)"

build: clean compile test package ## Full build: clean, compile, test, and package
	@echo "$(GREEN)✓ Full build completed successfully$(NC)"

install: ## Install to local Maven repository
	@echo "$(YELLOW)Installing to local repository...$(NC)"
	mvn install
	@echo "$(GREEN)✓ Installation completed$(NC)"

# Application execution
run: compile ## Run the GUI application
	@echo "$(YELLOW)Starting Subtitle Sync Application...$(NC)"
	mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dexec.args="$(ARGS)"

run-jar: package ## Run application from JAR file
	@echo "$(YELLOW)Running from JAR...$(NC)"
	java -jar $(TARGET_DIR)/$(JAR_NAME)

debug: compile ## Run application in debug mode (port 5005)
	@echo "$(YELLOW)Starting application in debug mode on port 5005...$(NC)"
	mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dexec.args="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

# Development tools
dev-setup: ## Setup development environment
	@echo "$(YELLOW)Setting up development environment...$(NC)"
	@if ! command -v mvn &> /dev/null; then \
		echo "$(RED)Maven not found. Please install Maven first.$(NC)"; \
		exit 1; \
	fi
	@if ! command -v java &> /dev/null; then \
		echo "$(RED)Java not found. Please install Java 24 or later.$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ Development environment ready$(NC)"
	@echo "Java version: $$(java -version 2>&1 | head -n 1)"
	@echo "Maven version: $$(mvn -version | head -n 1)"

# Dependencies management
deps-tree: ## Show dependency tree
	@echo "$(YELLOW)Showing dependency tree...$(NC)"
	mvn dependency:tree

deps-update: ## Update dependencies to latest versions
	@echo "$(YELLOW)Checking for dependency updates...$(NC)"
	mvn versions:display-dependency-updates

deps-analyze: ## Analyze dependencies for conflicts
	@echo "$(YELLOW)Analyzing dependencies...$(NC)"
	mvn dependency:analyze

# Archive and backup
archive: clean ## Create project archive
	@echo "$(YELLOW)Creating project archive...$(NC)"
	tar -czf $(APP_NAME)-$$(date +%Y%m%d-%H%M%S).tar.gz \
		--exclude='.git' \
		--exclude='target' \
		--exclude='*.tar.gz' \
		--exclude='.idea' \
		--exclude='*.iml' \
		.
	@echo "$(GREEN)✓ Archive created$(NC)"
