SHELL := /usr/bin/env bash
.DEFAULT_GOAL := help

.PHONY: help doctor hooks bootstrap install fmt lint test verify verify-fast verify-backend verify-frontend secrets docs e2e baseline clean

help: ## Muestra esta ayuda
	@awk 'BEGIN {FS = ":.*##"; printf "\nFerro · comandos del harness\n\n"} /^[a-zA-Z_-]+:.*?##/ {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@printf "\nEvidencia final: make verify (pega el bloque FERRO VERIFY en tu respuesta).\n\n"

doctor: ## Diagnóstico del entorno (Docker, hooks, cachés, red)
	@scripts/doctor.sh

hooks: ## Instala los git hooks del repo (.githooks)
	@chmod +x .githooks/* scripts/*.sh scripts/check-docs.py
	@git config core.hooksPath .githooks
	@echo "Hooks instalados: core.hooksPath=.githooks"

bootstrap: doctor hooks ## Prepara el repo para trabajar (doctor + hooks)
	@echo "Listo. Siguiente: make verify"

install: ## Descarga dependencias del frontend (Docker/Node 24)
	@scripts/frontend.sh install

fmt: ## Aplica formato (Maven Spotless + Prettier)
	@scripts/backend.sh spotless:apply
	@scripts/frontend.sh run format

lint: ## Solo linters y formato, sin tests
	@scripts/backend.sh spotless:check checkstyle:check
	@scripts/frontend.sh run format:check
	@scripts/frontend.sh run lint

test: ## Tests rápidos (sin empaquetar ni cobertura)
	@scripts/backend.sh test
	@scripts/frontend.sh run test

verify: ## Verificación completa y evidencia (backend + frontend + secretos + docs)
	@scripts/verify.sh

verify-fast: ## Verificación rápida; NO es evidencia final
	@scripts/verify.sh --fast

verify-backend: ## Verificación solo del backend
	@scripts/verify.sh --backend

verify-frontend: ## Verificación solo del frontend
	@scripts/verify.sh --frontend

secrets: ## Escanea el historial de git con gitleaks
	@docker run --rm -v "$(CURDIR):/repo" ghcr.io/gitleaks/gitleaks:latest git /repo --redact --verbose --exit-code=1

docs: ## Comprueba enlaces relativos de README y docs
	@python3 scripts/check-docs.py

e2e: ## E2E con Playwright (se configura en M1; hoy falla a propósito)
	@echo "E2E no configurado todavía (M1). No hay victoria que declarar aquí." >&2
	@exit 2

baseline: ## Actualiza el baseline de tests tras añadir tests (revisa el diff)
	@scripts/update-baseline.sh

clean: ## Borra artefactos locales (target, dist, coverage, .harness)
	@rm -rf backend/target frontend/dist frontend/coverage frontend/.angular .harness
