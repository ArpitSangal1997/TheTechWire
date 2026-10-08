const hostname = typeof window === 'undefined' ? '' : window.location.hostname;
const tunnelApiHost = hostname.endsWith('.devtunnels.ms')
  ? hostname.replace(/-4200(?=\.|$)/, '-8080')
  : null;

export const environment = {
  production: false,
  apiUrl: tunnelApiHost
    ? `https://${tunnelApiHost}/api`
    : 'http://localhost:8080/api'
};
