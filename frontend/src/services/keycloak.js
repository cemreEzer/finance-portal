import Keycloak from "keycloak-js";

/**
 * Keycloak yapılandırması – environment variable'lardan okunur.
 */
const keycloakConfig = {
    url: process.env.REACT_APP_KEYCLOAK_URL || "http://localhost:8443",
    realm: process.env.REACT_APP_KEYCLOAK_REALM || "finance-portal",
    clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || "finance-web",
};

const keycloak = new Keycloak(keycloakConfig);

export const keycloakInitOptions = {
    onLoad: "check-sso",
    silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
    pkceMethod: "S256",
};

export default keycloak;
