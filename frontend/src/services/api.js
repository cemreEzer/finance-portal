import axios from "axios";
import keycloak from "./keycloak";

/**
 * Axios instance – Backend API çağrıları için.
 * Keycloak token otomatik olarak header'a eklenir.
 */
const api = axios.create({
    baseURL: process.env.REACT_APP_API_BASE_URL || "http://localhost:8080",
    timeout: 15000,
    headers: {
        "Content-Type": "application/json",
    },
});

// ── Request Interceptor – JWT Token ──────────────────────────
api.interceptors.request.use(
    (config) => {
        if (keycloak.authenticated && keycloak.token) {
            config.headers.Authorization = `Bearer ${keycloak.token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// ── Response Interceptor – Token refresh on 401 ─────────────
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            try {
                await keycloak.updateToken(30);
                originalRequest.headers.Authorization = `Bearer ${keycloak.token}`;
                return api(originalRequest);
            } catch (refreshError) {
                keycloak.login();
                return Promise.reject(refreshError);
            }
        }
        return Promise.reject(error);
    }
);

/* ================================================================
   API Fonksiyonları
   ================================================================ */

// ── Döviz Kurları ────────────────────────────────────────────
export const currencyApi = {
    getAll: () => api.get("/api/market/currencies"),
    getByCode: (code) => api.get(`/api/market/currencies/${code}`),
    getHistory: (code, from, to) =>
        api.get(`/api/market/currencies/${code}/history`, { params: { from, to } }),
    compare: (codes, from, to) =>
        api.get("/api/market/currencies/compare", {
            params: { codes: codes.join(","), from, to },
        }),
    refresh: () => api.post("/api/market/currencies/refresh"),
};

// ── Haberler ─────────────────────────────────────────────────
export const newsApi = {
    getAll: (page = 0, size = 20) =>
        api.get("/api/news", { params: { page, size } }),
    getById: (id) => api.get(`/api/news/${id}`),
    getCategories: () => api.get("/api/news/categories"),
    getByCategory: (category, page = 0, size = 20) =>
        api.get(`/api/news/category/${category}`, { params: { page, size } }),
    search: (q, page = 0, size = 20) =>
        api.get("/api/news/search", { params: { q, page, size } }),
    refresh: () => api.post("/api/news/refresh"),
};

// ── Portföy ──────────────────────────────────────────────────
export const portfolioApi = {
    getAll: () => api.get("/api/portfolios"),
    getById: (id) => api.get(`/api/portfolios/${id}`),
    create: (data) => api.post("/api/portfolios", data),
    addItem: (portfolioId, data) =>
        api.post(`/api/portfolios/${portfolioId}/items`, data),
    deleteItem: (portfolioId, itemId) =>
        api.delete(`/api/portfolios/${portfolioId}/items/${itemId}`),
    getSummary: (id) => api.get(`/api/portfolios/${id}/summary`),
};

export default api;
