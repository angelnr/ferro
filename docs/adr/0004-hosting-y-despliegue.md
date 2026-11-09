# ADR-0004 · Hosting y despliegue: Vercel (frontend) + Oracle Always Free (backend)

**Estado:** Aceptada · 2026-11-09 · decidida con el cliente (ronda 2)

## Contexto

El cliente propuso «alojarla en Oracle y desplegarla con Vercel». Verificado
nov-2026: **Vercel no ejecuta Spring Boot** en la práctica (runtimes serverless sin
soporte Java estable; solo hay un runtime comunitario experimental inadecuado).
Oracle Always Free fue recortado en jun-2026 (ARM: 2 OCPU/12 GB antes 4/24), añade
regla de reclaim de instancias idle y requiere tarjeta. El cliente además dispone de
un servidor doméstico (Debian 13, Tailscale) con ~18 GB libres tras la limpieza.

## Decisión

- **Frontend (SPA estática + PWA): Vercel** — gratis, HTTPS, CDN, previews por PR;
  URL `ferro.vercel.app` (subdominios gratuitos, decisión del cliente).
- **Backend + BD + observabilidad: Oracle Cloud Always Free**, región home
  **eu-madrid-1** (RGPD, latencia ES): una VM `VM.Standard.A1.Flex` ARM de 2 OCPU /
  12 GB, 50 GB de boot (quedan 150 GB de block). Todo en un `docker compose prod`
  (api, postgres, prometheus, grafana, caddy).
- **DNS/TLS:** subdominio gratuito DuckDNS `ferro-api.duckdns.org` → IP de la VM;
  Caddy obtiene y renueva el certificado (ACME HTTP-01). OAuth Google/GitHub exige
  HTTPS: cubierto.
- **Anti-idle-reclaim:** UptimeRobot (gratis) golpea `/actuator/health/liveness`
  cada 5 min + tráfico propio diario (dogfooding). Riesgo residual aceptado y
  documentado; la VM es re-creable con el runbook en < 1 h.
- **Deploy:** GitHub Actions → imagen multi-arch (amd64/arm64) a GHCR → SSH
  `docker compose pull && up -d` (migraciones Flyway automáticas al arrancar).
- **Backups:** cron `pg_dump` diario (retención 7 local) + semanal cifrado a
  Oracle Object Storage (20 GB Always Free, retención 4). Prueba de restore real
  en M7 (RNF-11).

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Backend en Vercel | No soporta Spring Boot (verificado); reescribir el backend en Node solo por hosting contradice el objetivo CV de Java |
| Servidor doméstico + Cloudflare Tunnel | 0 € y sin tarjeta, pero: disco limitado, dependencia de la conexión doméstica (fibra con IP dinámica), y mezcla servicios personales con un demo pública; se reserva como entorno de staging/respaldo |
| Railway / Koyeb (~5 €/mes) | Buena DX, pero gasto mensual para un proyecto sin ingresos; Hetzner (~4 €/mes) queda como plan B documentado si Oracle recorta más o reclama la VM |
| Postgres managed (Neon/Supabase free) | Añade una dependencia externa y autosuspend/latencias frías en free tier; en la VM cabe de sobra y los backups ya están cubiertos; revisable si algún día se extrae el backend |

## Consecuencias

- **Positivas:** coste 0 €; el stack Oracle+Docker+Caddy+Grafana es una historia
  DevOps completa para el CV; región UE para RGPD.
- **Negativas:** single-VM sin HA (RNF-09 best-effort); dependencia del arbitrario
  de Oracle con el free tier (precedente jun-2026); gestión manual inicial de la VM.
- **Mitigaciones:** runbook recreación + backups off-site; UptimeRobot; plan B
  Hetzner escrito; todo el entorno prod es IaC (`infra/`) reproducible.
