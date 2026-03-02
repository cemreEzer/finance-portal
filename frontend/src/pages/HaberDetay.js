import React, { useState, useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import { newsApi } from "../services/api";

/**
 * Haber detay sayfası – Başlık, tarih, kaynak, içerik.
 * Doküman isteri: "Haber detay sayfası olmalı"
 */
function HaberDetay() {
    const { id } = useParams();
    const [article, setArticle] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchArticle();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    const fetchArticle = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await newsApi.getById(id);
            if (res.data?.success && res.data?.data) {
                setArticle(res.data.data);
            } else {
                setError(res.data?.message || "Haber bulunamadı");
            }
        } catch (err) {
            setError("Haber yüklenirken bir hata oluştu.");
            console.error("Haber detay hatası:", err);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="page">
                <div className="loading-spinner">
                    <div className="spinner"></div>
                    <p>Haber yükləniyor...</p>
                </div>
            </div>
        );
    }

    if (error || !article) {
        return (
            <div className="page">
                <div className="empty-state">
                    <h2>😔 {error || "Haber bulunamadı"}</h2>
                    <Link to="/haberler" className="btn btn-primary">
                        ← Haberlere Dön
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="page article-page">
            <div className="article-container">
                {/* ── Breadcrumb ──────────────────────────────────────── */}
                <nav className="breadcrumb">
                    <Link to="/">Ana Sayfa</Link>
                    <span className="sep">/</span>
                    <Link to="/haberler">Haberler</Link>
                    <span className="sep">/</span>
                    <Link to={`/haberler?category=${article.category}`}>
                        {article.category}
                    </Link>
                </nav>

                {/* ── Başlık ──────────────────────────────────────────── */}
                <header className="article-header">
                    <span className="news-category-tag">{article.category}</span>
                    <h1>{article.title}</h1>
                    <div className="article-meta">
                        <span className="meta-item">📰 {article.source}</span>
                        <span className="meta-item">
                            📅{" "}
                            {article.publishedAt
                                ? new Date(article.publishedAt).toLocaleDateString("tr-TR", {
                                    year: "numeric",
                                    month: "long",
                                    day: "numeric",
                                    hour: "2-digit",
                                    minute: "2-digit",
                                })
                                : "Tarih bilinmiyor"}
                        </span>
                    </div>
                </header>

                {/* ── Görsel ──────────────────────────────────────────── */}
                {article.imageUrl && (
                    <div className="article-image">
                        <img src={article.imageUrl} alt={article.title} />
                    </div>
                )}

                {/* ── İçerik ──────────────────────────────────────────── */}
                <div className="article-content">
                    {article.content ? (
                        <p>{article.content}</p>
                    ) : article.summary ? (
                        <p>{article.summary}</p>
                    ) : (
                        <p className="text-muted">İçerik mevcut değil.</p>
                    )}
                </div>

                {/* ── Kaynak Link ─────────────────────────────────────── */}
                {article.url && (
                    <div className="article-source-link">
                        <a
                            href={article.url}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="btn btn-primary"
                        >
                            🔗 Kaynağa Git
                        </a>
                    </div>
                )}

                {/* ── Geri ────────────────────────────────────────────── */}
                <div className="article-footer">
                    <Link to="/haberler" className="btn btn-secondary">
                        ← Haberlere Dön
                    </Link>
                </div>
            </div>
        </div>
    );
}

export default HaberDetay;
