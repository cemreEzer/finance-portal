import React from "react";
import { useKeycloak } from "@react-keycloak/web";

/**
 * Kullanıcı profil sayfası – Keycloak bilgileri.
 */
function Profil() {
    const { keycloak } = useKeycloak();
    const user = keycloak.tokenParsed;

    return (
        <div className="page">
            <div className="page-header">
                <h1>👤 Profil</h1>
            </div>

            <div className="card profile-card">
                <div className="profile-avatar">
                    <span className="avatar-large">
                        {(user?.preferred_username || "K")[0].toUpperCase()}
                    </span>
                </div>

                <table className="profile-table">
                    <tbody>
                        <tr>
                            <td className="label">Kullanıcı Adı</td>
                            <td>{user?.preferred_username || "—"}</td>
                        </tr>
                        <tr>
                            <td className="label">E-posta</td>
                            <td>{user?.email || "—"}</td>
                        </tr>
                        <tr>
                            <td className="label">Ad Soyad</td>
                            <td>{user?.name || `${user?.given_name || ""} ${user?.family_name || ""}`.trim() || "—"}</td>
                        </tr>
                        <tr>
                            <td className="label">Roller</td>
                            <td>
                                {keycloak.realmAccess?.roles
                                    ?.filter((r) => !r.startsWith("default-") && r !== "offline_access" && r !== "uma_authorization")
                                    .map((r) => (
                                        <span key={r} className="role-badge">{r}</span>
                                    )) || "—"}
                            </td>
                        </tr>
                    </tbody>
                </table>

                <div className="profile-actions">
                    <button
                        className="btn btn-primary"
                        onClick={() => keycloak.accountManagement()}
                    >
                        ⚙️ Hesap Ayarları (Keycloak)
                    </button>
                    <button
                        className="btn btn-logout"
                        onClick={() => keycloak.logout()}
                    >
                        🚪 Çıkış Yap
                    </button>
                </div>
            </div>
        </div>
    );
}

export default Profil;
