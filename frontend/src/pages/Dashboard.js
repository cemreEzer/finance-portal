import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import { currencyApi, newsApi } from "../services/api";

/**
 * Ana Sayfa / Dashboard – Güncel kur özeti + son haberler.
 */
function Dashboard() {
    const { keycloak } = useKeycloak();
    const [currencies, setCurrencies] = useState([]);
    const [news, setNews] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        setLoading(true);
        try {
            const [currRes, newsRes] = await Promise.allSettled([
                currencyApi.getAll(),
                newsApi.getAll(0, 6),
            ]);

            if (currRes.status === "fulfilled" && currRes.value.data?.data) {
                // İlk 8 önemli kuru göster
                const important = ["USD", "EUR", "GBP", "CHF", "JPY", "SAR", "AUD", "CAD"];
                const filtered = currRes.value.data.data.filter((c) =>
                    important.includes(c.currencyCode)
                );
                setCurrencies(filtered.length > 0 ? filtered : currRes.value.data.data.slice(0, 8));
            }

            if (newsRes.status === "fulfilled" && newsRes.value.data?.data?.articles) {
                setNews(newsRes.value.data.data.articles);
            }
        } catch (err) {
            console.error("Dashboard verisi alınamadı:", err);
        } finally {
            setLoading(false);
        }
    };

    const formatPrice = (val) => {
        if (!val) return "—";
        return Number(val).toFixed(4);
    };

    if (loading) {
        return (
            <div className="page">
                <div className="loading-spinner">
                    <div className="spinner"></div>
                    <p>Veriler yükləniyor...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="page dashboard">
            {/* ── Hero Section ──────────────────────────────────────── */}
            <section className="hero">
                <div className="hero-content">
                    <h1>
                        Hoş Geldiniz
                        {keycloak.authenticated
                            ? `, ${keycloak.tokenParsed?.preferred_username}`
                            : ""}
                        ! 👋
                    </h1>
                    <p className="hero-subtitle">
                        Piyasa verilerine, haberlere ve portföyünüze tek bir yerden erişin.
                    </p>
                </div>
            </section>

            {/* ── Kur Kartları ──────────────────────────────────────── */}
            <section className="section">
                <div className="section-header">
                    <h2>💱 Güncel Döviz Kurları</h2>
                    <Link to="/piyasalar" className="btn btn-link">
                        Tümünü Gör →
                    </Link>
                </div>
                <div className="currency-cards">
                    {currencies.map((c) => (
                        <div className="currency-card" key={c.currencyCode}>
                            <div className="currency-code">{c.currencyCode}/TRY</div>
                            <div className="currency-name">{c.currencyName}</div>
                            <div className="currency-prices">
                                <div className="price-item buying">
                                    <span className="price-label">Alış</span>
                                    <span className="price-value">₺{formatPrice(c.forexBuying)}</span>
                                </div>
                                <div className="price-item selling">
                                    <span className="price-label">Satış</span>
                                    <span className="price-value">₺{formatPrice(c.forexSelling)}</span>
                                </div>
                            </div>
                            {c.unit > 1 && (
                                <div className="currency-unit">{c.unit} birim</div>
                            )}
                        </div>
                    ))}
                </div>
            </section>

            {/* ── Son Haberler ──────────────────────────────────────── */}
            <section className="section">
                <div className="section-header">
                    <h2>📰 Son Haberler</h2>
                    <Link to="/haberler" className="btn btn-link">
                        Tümünü Gör →
                    </Link>
                </div>
                <div className="news-grid">
                    {news.map((n) => (
                        <Link to={`/haberler/${n.id}`} className="news-card" key={n.id}>
                            {n.imageUrl && (
                                <div
                                    className="news-image"
                                    style={{ backgroundImage: `url(${n.imageUrl})` }}
                                />
                            )}
                            <div className="news-body">
                                <span className="news-category-tag">{n.category}</span>
                                <h3 className="news-title">{n.title}</h3>
                                <p className="news-summary">
                                    {n.summary?.substring(0, 120)}
                                    {n.summary?.length > 120 ? "..." : ""}
                                </p>
                                <div className="news-meta">
                                    <span className="news-source">{n.source}</span>
                                    <span className="news-date">
                                        {n.publishedAt
                                            ? new Date(n.publishedAt).toLocaleDateString("tr-TR")
                                            : ""}
                                    </span>
                                </div>
                            </div>
                        </Link>
                    ))}
                </div>
            </section>

            {/* ── Quick Actions ─────────────────────────────────────── */}
            <section className="section">
                <div className="quick-actions">
                    <Link to="/piyasalar" className="action-card">
                        <span className="action-icon">📊</span>
                        <h3>Piyasa Verileri</h3>
                        <p>Döviz, hisse, tahvil</p>
                    </Link>
                    <Link to="/haberler" className="action-card">
                        <span className="action-icon">📰</span>
                        <h3>Finans Haberleri</h3>
                        <p>Güncel haberler</p>
                    </Link>
                    {keycloak.authenticated ? (
                        <Link to="/portfoy" className="action-card">
                            <span className="action-icon">💼</span>
                            <h3>Portföyüm</h3>
                            <p>Yatırımlarınızı yönetin</p>
                        </Link>
                    ) : (
                        <div
                            className="action-card"
                            onClick={() => keycloak.login()}
                            style={{ cursor: "pointer" }}
                        >
                            <span className="action-icon">🔐</span>
                            <h3>Giriş Yap</h3>
                            <p>Portföy erişimi için</p>
                        </div>
                    )}
                </div>
            </section>
        </div>
    );
}

export default Dashboard;
