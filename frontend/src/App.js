import React from "react";
import {
    BrowserRouter as Router,
    Routes,
    Route,
} from "react-router-dom";
import { ReactKeycloakProvider, useKeycloak } from "@react-keycloak/web";
import keycloak, { keycloakInitOptions } from "./services/keycloak";

import Navbar from "./components/Navbar";
import Dashboard from "./pages/Dashboard";
import Haberler from "./pages/Haberler";
import HaberDetay from "./pages/HaberDetay";
import Piyasalar from "./pages/Piyasalar";
import Portfoy from "./pages/Portfoy";
import Profil from "./pages/Profil";

import "./index.css";

/* ================================================================
   Private Route – Keycloak Authentication
   ================================================================ */
function PrivateRoute({ children }) {
    const { keycloak: kc } = useKeycloak();
    if (!kc.authenticated) {
        kc.login();
        return (
            <div className="loading-spinner">
                <div className="spinner"></div>
                <p>Giriş yapılıyor...</p>
            </div>
        );
    }
    return children;
}

/* ================================================================
   App Routes
   ================================================================ */
function AppRoutes() {
    return (
        <>
            <Navbar />
            <main className="main-content">
                <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/haberler" element={<Haberler />} />
                    <Route path="/haberler/:id" element={<HaberDetay />} />
                    <Route path="/piyasalar" element={<Piyasalar />} />
                    <Route
                        path="/portfoy"
                        element={
                            <PrivateRoute>
                                <Portfoy />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="/profil"
                        element={
                            <PrivateRoute>
                                <Profil />
                            </PrivateRoute>
                        }
                    />
                    <Route
                        path="*"
                        element={
                            <div className="page">
                                <div className="empty-state">
                                    <h1>404</h1>
                                    <p>Sayfa bulunamadı</p>
                                </div>
                            </div>
                        }
                    />
                </Routes>
            </main>
            <footer className="footer">
                <p>© 2026 Finans Portalı – Tüm hakları saklıdır.</p>
            </footer>
        </>
    );
}

/* ================================================================
   App Root
   ================================================================ */
function App() {
    return (
        <ReactKeycloakProvider
            authClient={keycloak}
            initOptions={keycloakInitOptions}
            LoadingComponent={
                <div className="app-loading">
                    <div className="spinner"></div>
                    <h2>Finans Portalı</h2>
                    <p>Yükleniyor...</p>
                </div>
            }
        >
            <Router>
                <AppRoutes />
            </Router>
        </ReactKeycloakProvider>
    );
}

export default App;
