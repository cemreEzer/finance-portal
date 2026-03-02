import React from "react";
import { Link, useLocation } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";

/**
 * Navigation Bar – Keycloak entegrasyonlu.
 */
function Navbar() {
    const { keycloak } = useKeycloak();
    const location = useLocation();

    const isActive = (path) => location.pathname === path;

    return (
        <nav className="navbar">
            <div className="navbar-brand">
                <Link to="/">
                    <span className="brand-icon">💹</span>
                    <span className="brand-text">Finans Portalı</span>
                </Link>
            </div>

            <ul className="navbar-links">
                <li>
                    <Link to="/" className={isActive("/") ? "active" : ""}>
                        🏠 Ana Sayfa
                    </Link>
                </li>
                <li>
                    <Link to="/haberler" className={isActive("/haberler") ? "active" : ""}>
                        📰 Haberler
                    </Link>
                </li>
                <li>
                    <Link
                        to="/piyasalar"
                        className={isActive("/piyasalar") ? "active" : ""}
                    >
                        📊 Piyasalar
                    </Link>
                </li>
                {keycloak.authenticated && (
                    <li>
                        <Link
                            to="/portfoy"
                            className={isActive("/portfoy") ? "active" : ""}
                        >
                            💼 Portföy
                        </Link>
                    </li>
                )}
            </ul>

            <div className="navbar-auth">
                {keycloak.authenticated ? (
                    <div className="user-menu">
                        <Link to="/profil" className="user-info">
                            <span className="user-avatar">
                                {(
                                    keycloak.tokenParsed?.preferred_username || "K"
                                )[0].toUpperCase()}
                            </span>
                            <span className="user-name">
                                {keycloak.tokenParsed?.preferred_username || "Kullanıcı"}
                            </span>
                        </Link>
                        <button className="btn btn-logout" onClick={() => keycloak.logout()}>
                            Çıkış
                        </button>
                    </div>
                ) : (
                    <button className="btn btn-login" onClick={() => keycloak.login()}>
                        Giriş Yap
                    </button>
                )}
            </div>
        </nav>
    );
}

export default Navbar;
