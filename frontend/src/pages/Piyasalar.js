import React, { useState, useEffect } from "react";
import { currencyApi } from "../services/api";
import CurrencyChart from "../components/CurrencyChart";

/**
 * Piyasa Verileri sayfası – TCMB kur tablosu + tarihsel grafik.
 */
function Piyasalar() {
    const [currencies, setCurrencies] = useState([]);
    const [loading, setLoading] = useState(true);
    const [lastUpdate, setLastUpdate] = useState(null);
    const [filter, setFilter] = useState("");

    useEffect(() => {
        fetchCurrencies();
    }, []);

    const fetchCurrencies = async () => {
        setLoading(true);
        try {
            const res = await currencyApi.getAll();
            if (res.data?.data) {
                setCurrencies(res.data.data);
                if (res.data.data.length > 0) {
                    setLastUpdate(res.data.data[0].date);
                }
            }
        } catch (err) {
            console.error("Kur verisi alınamadı:", err);
        } finally {
            setLoading(false);
        }
    };

    const handleRefresh = async () => {
        setLoading(true);
        try {
            const res = await currencyApi.refresh();
            if (res.data?.data) {
                setCurrencies(res.data.data);
            }
            await fetchCurrencies();
        } catch (err) {
            console.error("Kur güncelleme başarısız:", err);
        } finally {
            setLoading(false);
        }
    };

    const formatPrice = (val) => {
        if (!val) return "—";
        return Number(val).toFixed(4);
    };

    const filtered = currencies.filter(
        (c) =>
            c.currencyCode?.toLowerCase().includes(filter.toLowerCase()) ||
            c.currencyName?.toLowerCase().includes(filter.toLowerCase())
    );

    return (
        <div className="page">
            <div className="page-header">
                <h1>📊 Piyasa Verileri</h1>
                <p>TCMB güncel döviz kurları ve tarihsel analiz</p>
            </div>

            {/* ── Kur Tablosu ───────────────────────────────────────── */}
            <section className="section">
                <div className="section-header">
                    <h2>💱 Döviz Kurları</h2>
                    <div className="section-actions">
                        {lastUpdate && (
                            <span className="update-badge">
                                📅 Son güncelleme: {lastUpdate}
                            </span>
                        )}
                        <button
                            className="btn btn-primary btn-sm"
                            onClick={handleRefresh}
                            disabled={loading}
                        >
                            🔄 Güncelle
                        </button>
                    </div>
                </div>

                {/* ── Filtre ──────────────────────────────────────────── */}
                <div className="table-filter">
                    <input
                        type="text"
                        placeholder="Döviz kodu veya adı ara (ör. USD, Euro)..."
                        value={filter}
                        onChange={(e) => setFilter(e.target.value)}
                    />
                </div>

                {loading ? (
                    <div className="loading-spinner">
                        <div className="spinner"></div>
                        <p>Kurlar yükleniyor...</p>
                    </div>
                ) : (
                    <div className="table-container">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Döviz Kodu</th>
                                    <th>Döviz Adı</th>
                                    <th>Birim</th>
                                    <th className="text-right">Döviz Alış</th>
                                    <th className="text-right">Döviz Satış</th>
                                    <th className="text-right">Banknot Alış</th>
                                    <th className="text-right">Banknot Satış</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.length > 0 ? (
                                    filtered.map((c) => (
                                        <tr key={`${c.currencyCode}-${c.id}`}>
                                            <td>
                                                <span className="currency-badge">{c.currencyCode}</span>
                                            </td>
                                            <td>{c.currencyName || "—"}</td>
                                            <td className="text-center">{c.unit}</td>
                                            <td className="text-right price-cell buying">
                                                ₺{formatPrice(c.forexBuying)}
                                            </td>
                                            <td className="text-right price-cell selling">
                                                ₺{formatPrice(c.forexSelling)}
                                            </td>
                                            <td className="text-right">
                                                ₺{formatPrice(c.banknoteBuying)}
                                            </td>
                                            <td className="text-right">
                                                ₺{formatPrice(c.banknoteSelling)}
                                            </td>
                                        </tr>
                                    ))
                                ) : (
                                    <tr>
                                        <td colSpan="7" className="text-center text-muted">
                                            Kur verisi bulunamadı.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            {/* ── Tarihsel Grafik ───────────────────────────────────── */}
            <section className="section">
                <h2>📈 Tarihsel Kur Analizi</h2>
                <CurrencyChart />
            </section>
        </div>
    );
}

export default Piyasalar;
