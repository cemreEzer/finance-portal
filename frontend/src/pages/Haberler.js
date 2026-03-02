import React, { useState, useEffect } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { newsApi } from "../services/api";

/**
 * Haberler sayfası – Kategorilere göre filtreleme, arama ve sayfalama.
 */
function Haberler() {
    const [searchParams, setSearchParams] = useSearchParams();
    const [articles, setArticles] = useState([]);
    const [categories, setCategories] = useState([]);
    const [selectedCategory, setSelectedCategory] = useState(
        searchParams.get("category") || ""
    );
    const [searchQuery, setSearchQuery] = useState("");
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchCategories();
    }, []);

    useEffect(() => {
        fetchNews();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedCategory, page]);

    const fetchCategories = async () => {
        try {
            const res = await newsApi.getCategories();
            if (res.data?.data) setCategories(res.data.data);
        } catch (err) {
            console.error("Kategoriler alınamadı:", err);
        }
    };

    const fetchNews = async () => {
        setLoading(true);
        try {
            let res;
            if (searchQuery.trim()) {
                res = await newsApi.search(searchQuery, page, 12);
            } else if (selectedCategory) {
                res = await newsApi.getByCategory(selectedCategory, page, 12);
            } else {
                res = await newsApi.getAll(page, 12);
            }

            if (res.data?.data) {
                setArticles(res.data.data.articles || []);
                setTotalPages(res.data.data.totalPages || 0);
            }
        } catch (err) {
            console.error("Haberler alınamadı:", err);
            setArticles([]);
        } finally {
            setLoading(false);
        }
    };

    const handleCategoryChange = (cat) => {
        setSelectedCategory(cat);
        setPage(0);
        setSearchQuery("");
        if (cat) {
            setSearchParams({ category: cat });
        } else {
            setSearchParams({});
        }
    };

    const handleSearch = (e) => {
        e.preventDefault();
        setPage(0);
        setSelectedCategory("");
        fetchNews();
    };

    return (
        <div className="page">
            <div className="page-header">
                <h1>📰 Finans Haberleri</h1>
                <p>Güncel ekonomi ve finans haberleri</p>
            </div>

            {/* ── Arama ─────────────────────────────────────────────── */}
            <form className="search-bar" onSubmit={handleSearch}>
                <input
                    type="text"
                    placeholder="Haberlerde ara..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                />
                <button type="submit" className="btn btn-primary">
                    🔍 Ara
                </button>
            </form>

            {/* ── Kategori Filtreleri ────────────────────────────────── */}
            <div className="category-filter">
                <button
                    className={`filter-btn ${!selectedCategory ? "active" : ""}`}
                    onClick={() => handleCategoryChange("")}
                >
                    Tümü
                </button>
                {categories.map((cat) => (
                    <button
                        key={cat.name}
                        className={`filter-btn ${selectedCategory === cat.name ? "active" : ""}`}
                        onClick={() => handleCategoryChange(cat.name)}
                    >
                        {cat.displayName}
                    </button>
                ))}
            </div>

            {/* ── Haber Listesi ─────────────────────────────────────── */}
            {loading ? (
                <div className="loading-spinner">
                    <div className="spinner"></div>
                    <p>Haberler yükleniyor...</p>
                </div>
            ) : articles.length > 0 ? (
                <>
                    <div className="news-grid news-grid-full">
                        {articles.map((article) => (
                            <Link
                                to={`/haberler/${article.id}`}
                                className="news-card"
                                key={article.id}
                            >
                                {article.imageUrl && (
                                    <div
                                        className="news-image"
                                        style={{ backgroundImage: `url(${article.imageUrl})` }}
                                    />
                                )}
                                <div className="news-body">
                                    <span className="news-category-tag">{article.category}</span>
                                    <h3 className="news-title">{article.title}</h3>
                                    <p className="news-summary">
                                        {article.summary?.substring(0, 150)}
                                        {article.summary?.length > 150 ? "..." : ""}
                                    </p>
                                    <div className="news-meta">
                                        <span className="news-source">{article.source}</span>
                                        <span className="news-date">
                                            {article.publishedAt
                                                ? new Date(article.publishedAt).toLocaleDateString("tr-TR")
                                                : ""}
                                        </span>
                                    </div>
                                </div>
                            </Link>
                        ))}
                    </div>

                    {/* ── Sayfalama ───────────────────────────────────────── */}
                    {totalPages > 1 && (
                        <div className="pagination">
                            <button
                                className="btn btn-sm"
                                disabled={page === 0}
                                onClick={() => setPage(page - 1)}
                            >
                                ← Önceki
                            </button>
                            <span className="page-info">
                                Sayfa {page + 1} / {totalPages}
                            </span>
                            <button
                                className="btn btn-sm"
                                disabled={page >= totalPages - 1}
                                onClick={() => setPage(page + 1)}
                            >
                                Sonraki →
                            </button>
                        </div>
                    )}
                </>
            ) : (
                <div className="empty-state">
                    <p>📭 Haber bulunamadı.</p>
                </div>
            )}
        </div>
    );
}

export default Haberler;
