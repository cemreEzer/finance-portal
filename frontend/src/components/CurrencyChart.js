import React, { useState, useEffect, useMemo } from "react";
import {
    LineChart, Line, XAxis, YAxis, CartesianGrid,
    Tooltip, Legend, ResponsiveContainer,
} from "recharts";
import { currencyApi } from "../services/api";

const COLORS = [
    "#6366f1", "#f59e0b", "#10b981", "#ef4444",
    "#8b5cf6", "#ec4899", "#14b8a6", "#f97316",
];

/**
 * Tarihsel döviz kuru çizgi grafiği – Recharts.
 * Doküman isteri: "Tarihsel fiyat verilerini grafik olarak görebilmelidir"
 * ve "Aynı grafikte birden fazla enstrümanın performansı gösterilebilmelidir"
 */
function CurrencyChart() {
    const [codes, setCodes] = useState(["USD", "EUR"]);
    const [period, setPeriod] = useState("1M");
    const [chartData, setChartData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [inputCode, setInputCode] = useState("");

    const dateRange = useMemo(() => {
        const to = new Date();
        const from = new Date();
        switch (period) {
            case "1W": from.setDate(to.getDate() - 7); break;
            case "1M": from.setMonth(to.getMonth() - 1); break;
            case "3M": from.setMonth(to.getMonth() - 3); break;
            case "6M": from.setMonth(to.getMonth() - 6); break;
            case "1Y": from.setFullYear(to.getFullYear() - 1); break;
            default: from.setMonth(to.getMonth() - 1);
        }
        return {
            from: from.toISOString().split("T")[0],
            to: to.toISOString().split("T")[0],
        };
    }, [period]);

    useEffect(() => {
        if (codes.length === 0) return;
        fetchChartData();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [codes, period]);

    const fetchChartData = async () => {
        setLoading(true);
        try {
            const res = await currencyApi.compare(codes, dateRange.from, dateRange.to);
            const comparison = res.data?.data;
            if (!comparison) { setChartData([]); return; }

            // Tüm tarihleri birleştir
            const dateMap = {};
            Object.entries(comparison).forEach(([code, rates]) => {
                rates.forEach((r) => {
                    const date = r.date;
                    if (!dateMap[date]) dateMap[date] = { date };
                    dateMap[date][code] = r.forexSelling;
                });
            });

            const sorted = Object.values(dateMap).sort(
                (a, b) => new Date(a.date) - new Date(b.date)
            );
            setChartData(sorted);
        } catch (err) {
            console.error("Grafik verisi alınamadı:", err);
            setChartData([]);
        } finally {
            setLoading(false);
        }
    };

    const addCode = () => {
        const c = inputCode.trim().toUpperCase();
        if (c && !codes.includes(c) && codes.length < 5) {
            setCodes([...codes, c]);
            setInputCode("");
        }
    };

    const removeCode = (c) => setCodes(codes.filter((x) => x !== c));

    return (
        <div className="chart-container card">
            <div className="chart-header">
                <h3>📈 Tarihsel Kur Grafiği</h3>
                <div className="chart-controls">
                    <div className="period-buttons">
                        {["1W", "1M", "3M", "6M", "1Y"].map((p) => (
                            <button
                                key={p}
                                className={`btn btn-sm ${period === p ? "btn-active" : ""}`}
                                onClick={() => setPeriod(p)}
                            >
                                {p}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            <div className="chart-codes">
                {codes.map((c) => (
                    <span key={c} className="code-tag">
                        {c}
                        <button className="tag-remove" onClick={() => removeCode(c)}>×</button>
                    </span>
                ))}
                {codes.length < 5 && (
                    <div className="code-input-group">
                        <input
                            type="text"
                            placeholder="Döviz kodu ekle (ör. GBP)"
                            value={inputCode}
                            onChange={(e) => setInputCode(e.target.value)}
                            onKeyDown={(e) => e.key === "Enter" && addCode()}
                            maxLength={5}
                        />
                        <button className="btn btn-sm btn-add" onClick={addCode}>+</button>
                    </div>
                )}
            </div>

            {loading ? (
                <div className="chart-loading">Grafik yükleniyor...</div>
            ) : chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height={400}>
                    <LineChart data={chartData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                        <XAxis
                            dataKey="date"
                            tick={{ fill: "#9ca3af", fontSize: 12 }}
                            tickFormatter={(d) => {
                                const dt = new Date(d);
                                return `${dt.getDate()}/${dt.getMonth() + 1}`;
                            }}
                        />
                        <YAxis tick={{ fill: "#9ca3af", fontSize: 12 }} />
                        <Tooltip
                            contentStyle={{
                                background: "#1f2937",
                                border: "1px solid #374151",
                                borderRadius: "8px",
                                color: "#f3f4f6",
                            }}
                            formatter={(value, name) => [
                                `₺${Number(value).toFixed(4)}`,
                                name,
                            ]}
                        />
                        <Legend />
                        {codes.map((code, i) => (
                            <Line
                                key={code}
                                type="monotone"
                                dataKey={code}
                                name={code}
                                stroke={COLORS[i % COLORS.length]}
                                strokeWidth={2}
                                dot={false}
                                activeDot={{ r: 5 }}
                            />
                        ))}
                    </LineChart>
                </ResponsiveContainer>
            ) : (
                <div className="chart-empty">
                    Grafik verisi bulunamadı. Lütfen bir döviz kodu ekleyin.
                </div>
            )}
        </div>
    );
}

export default CurrencyChart;
