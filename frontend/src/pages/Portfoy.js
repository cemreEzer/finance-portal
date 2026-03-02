import React from "react";
import { useKeycloak } from "@react-keycloak/web";

/**
 * Portföy Yönetimi sayfası (Keycloak authentication gerekli).
 * Tam implementasyon bir sonraki aşamada yapılacak.
 */
function Portfoy() {
    const { keycloak } = useKeycloak();
    const user = keycloak.tokenParsed;

    return (
        <div className="page">
            <div className="page-header">
                <h1>💼 Portföy Yönetimi</h1>
                <p>
                    Merhaba{" "}
                    <strong>{user?.preferred_username || "Kullanıcı"}</strong>,
                    yatırımlarınızı buradan yönetebilirsiniz.
                </p>
            </div>

            <section className="section">
                <div className="card portfolio-summary">
                    <h2>Portföy Özeti</h2>
                    <div className="portfolio-stats">
                        <div className="stat-card">
                            <span className="stat-value">₺0.00</span>
                            <span className="stat-label">Toplam Değer</span>
                        </div>
                        <div className="stat-card positive">
                            <span className="stat-value">₺0.00</span>
                            <span className="stat-label">Toplam Kâr/Zarar</span>
                        </div>
                        <div className="stat-card">
                            <span className="stat-value">0</span>
                            <span className="stat-label">Enstrüman Sayısı</span>
                        </div>
                    </div>
                </div>
            </section>

            <section className="section">
                <div className="section-header">
                    <h2>📋 Enstrümanlar</h2>
                    <button className="btn btn-primary">+ Enstrüman Ekle</button>
                </div>

                <div className="empty-state">
                    <span className="empty-icon">📊</span>
                    <h3>Portföyünüz henüz boş</h3>
                    <p>
                        Döviz, hisse senedi veya yatırım fonu ekleyerek portföyünüzü
                        oluşturmaya başlayın.
                    </p>
                    <button className="btn btn-primary">İlk Enstrümanı Ekle</button>
                </div>
            </section>
        </div>
    );
}

export default Portfoy;
