// Entorno de desarrollo.
// La API vive en el mismo host que sirve la app, en el puerto del backend local
// (FERRO/SERVER_PORT, 8082 cuando 8080 está ocupado). Derivar la URL del hostname
// del navegador hace que la UI funcione igual desde localhost que desde otra
// máquina de la tailnet o de la LAN, sin hardcodear la IP de nadie.
const DEV_API_PORT = 8082;

export function devApiBaseUrl(hostname: string): string {
  const host = hostname.trim().length > 0 ? hostname.trim() : 'localhost';
  return `http://${host}:${DEV_API_PORT}/api/v1`;
}

export const environment = {
  production: false,
  apiBaseUrl: devApiBaseUrl(typeof location === 'undefined' ? 'localhost' : location.hostname),
};
