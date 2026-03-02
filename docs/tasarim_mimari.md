# Finans Portalı – Mimari Tasarım Dokümanı

> **Sürüm:** 1.0  
> **Tarih:** 2 Mart 2026  
> **Hazırlayan:** Finans Portalı Ekibi

---

## 1. Genel Bakış

Finans Portalı, kullanıcılara finansal piyasa verilerini, haberleri ve kendi portföylerini yönetebilecekleri, temel teknik analizler yapabilecekleri bir web ve mobil uygulamadır. Uygulama, konteyner tabanlı, mikroservis-uyumlu bir mimari üzerinde çalışacak şekilde tasarlanmıştır.

### 1.1 Kapsam

| Modül | Açıklama |
|-------|----------|
| **Haber Modülü** | Açık kaynaklardan çekilen güncel finans haberleri, kategorilendirme ve detay sayfaları |
| **Piyasa Verileri** | TCMB kurları, hisse senetleri, tahvil/bono, VIOP, yatırım fonları |
| **Tarihsel Veri & Analiz** | Grafik tabanlı tarihsel veri görüntüleme, hareketli ortalama, trend analizi |
| **Portföy Yönetimi** | Enstrüman seçimi, alış bilgisi, kâr/zarar hesaplama, dağılım grafikleri |
| **Kullanıcı Yönetimi** | Keycloak ile SSO, rol tabanlı erişim, 2FA desteği |

---

## 2. Mimari Diyagram

```
┌────────────────────────────────────────────────────────────────────┐
│                        KULLANICI KATMANI                          │
│  ┌──────────────────┐           ┌────────────────────┐            │
│  │  React Web App   │           │  React Native App  │            │
│  │  (Port 3000)     │           │  (Mobil)           │            │
│  └────────┬─────────┘           └─────────┬──────────┘            │
│           │          REST / JWT            │                      │
└───────────┼───────────────────────────────┼───────────────────────┘
            │                               │
┌───────────▼───────────────────────────────▼───────────────────────┐
│                       API GATEWAY / BACKEND                       │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │              Spring Boot 3.x  (Java 17+)                  │   │
│  │  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐ │   │
│  │  │ Controller │→ │  Service   │→ │  Repository (JPA)    │ │   │
│  │  └────────────┘  └────────────┘  └──────────┬───────────┘ │   │
│  │                                             │             │   │
│  │  ┌──────────────┐  ┌──────────────────────┐ │             │   │
│  │  │  Scheduler   │  │  OpenTelemetry SDK   │ │             │   │
│  │  │  (Veri Çekme)│  │  (Trace + Metrics)   │ │             │   │
│  │  └──────────────┘  └──────────────────────┘ │             │   │
│  └─────────────────────────────────────────────┼─────────────┘   │
│                        Port 8080               │                 │
└────────────────────────────────────────────────┼─────────────────┘
                                                 │
┌────────────────────────────────────────────────┼─────────────────┐
│                    VERİ & ALTYAPI KATMANI       │                 │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────▼──────────────┐  │
│  │   Keycloak   │  │  OpenSearch  │  │     PostgreSQL         │  │
│  │  (Port 8443) │  │  (Port 9200) │  │     (Port 5432)        │  │
│  │  + OpenLDAP  │  │  + Dashboards│  │     + Flyway Migration │  │
│  └──────────────┘  └──────────────┘  └────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. Teknoloji Yığını

### 3.1 Backend

| Özellik | Teknoloji |
|---------|-----------|
| Dil / Versiyon | Java 17+ |
| Framework | Spring Boot 3.x |
| API Stili | RESTful (JSON) |
| ORM | Spring Data JPA / Hibernate |
| Migration | Flyway |
| Güvenlik | Spring Security + Keycloak Adapter (OAuth2 / OIDC) |
| Zamanlayıcı | Spring `@Scheduled` |
| Cache | Spring Cache (In-memory, opsiyonel Redis) |
| Loglama | Log4j2 (JSON Layout) |
| Gözlemlenebilirlik | OpenTelemetry Java Agent |
| Build Aracı | Maven |

### 3.2 Web Frontend

| Özellik | Teknoloji |
|---------|-----------|
| Framework | ReactJS (18+) |
| Routing | React Router v6 |
| State Yönetimi | Context API (opsiyonel Redux) |
| HTTP Client | Axios |
| Grafik | Recharts / Chart.js |
| Auth | Keycloak JS Adapter (`@react-keycloak/web`) |
| CSS | Vanilla CSS / CSS Modules |

### 3.3 Mobil Uygulama

| Özellik | Teknoloji |
|---------|-----------|
| Framework | React Native |
| Auth | Keycloak OIDC akışı |
| API İletişimi | REST (Axios / Fetch) |

### 3.4 Veritabanı

| Özellik | Teknoloji |
|---------|-----------|
| RDBMS | PostgreSQL 15+ |
| ORM | Hibernate |
| Migration | Flyway |
| Normalleştirme | 3NF seviyesinde tablo tasarımı |

---

## 4. Konteyner Mimarisi (Docker)

Tüm bileşenler ayrı konteynerlarda çalışır ve `docker-compose.yml` ile orkestre edilir.

| Servis | İmaj | Port |
|--------|------|------|
| `backend` | Custom (Dockerfile – Maven build) | 8080 |
| `frontend` | Custom (Dockerfile – Node/Nginx) | 3000 |
| `postgres` | `postgres:15-alpine` | 5432 |
| `keycloak` | `quay.io/keycloak/keycloak:24.0` | 8443 |
| `opensearch` | `opensearchproject/opensearch:2` | 9200 |
| `opensearch-dashboards` | `opensearchproject/opensearch-dashboards:2` | 5601 |

### 4.1 Ağ Yapısı

```
finance-portal-network (bridge)
├── backend ←→ postgres
├── backend ←→ keycloak
├── backend ←→ opensearch
├── frontend ←→ backend (API proxy)
└── opensearch ←→ opensearch-dashboards
```

---

## 5. Loglama Mimarisi

```
Spring Boot App
    │
    ▼
Log4j2 (JSON Layout)
    │
    ├──→ Console Appender (stdout)
    ├──→ File Appender (logs/app.log)
    └──→ OpenSearch Appender (HTTP / Kafka opsiyonel)
           │
           ▼
      OpenSearch Cluster
           │
           ▼
   OpenSearch Dashboards (Görselleştirme)
```

### 5.1 JSON Log Formatı

```json
{
  "timestamp": "2026-03-02T21:30:00.000+03:00",
  "level": "INFO",
  "serviceName": "finance-portal-backend",
  "logger": "com.finans.service.HaberService",
  "message": "Haberler başarıyla çekildi",
  "traceId": "abc123def456",
  "spanId": "789ghi",
  "exception": null
}
```

---

## 6. Gözlemlenebilirlik (OpenTelemetry)

### 6.1 Toplanan Metrikler

- **İstek sayısı** (request count per endpoint)
- **Yanıt süreleri** (response time – p50, p95, p99)
- **Hata oranları** (HTTP 4xx / 5xx ratio)
- **JVM metrikleri** (heap, GC, thread count)

### 6.2 Trace Akışı

```
[React Frontend] → [Spring Boot Controller] → [Service] → [Repository/DB]
     span-1              span-2                span-3        span-4
```

### 6.3 Dashboard Örnekleri

| Dashboard | İçerik |
|-----------|--------|
| API Response Time | Endpoint bazlı ortalama/p95 yanıt süresi |
| Error Rate | Zaman dilimlerine göre hata oranı |
| Request Volume | Saniye/dakika başına istek sayısı |
| Service Health | Aktif instance, uptime, healthcheck durumu |

---

## 7. Güvenlik ve Kimlik Yönetimi

### 7.1 Keycloak Konfigürasyonu

- **Realm:** `finance-portal`
- **Client'lar:**
  - `finance-web` (public, PKCE)
  - `finance-mobile` (public, PKCE)
  - `finance-backend` (confidential, service account)
- **Protokol:** OpenID Connect (OIDC)
- **Token Tipi:** JWT (Access Token + Refresh Token)

### 7.2 Rol Modeli

| Rol | Yetki |
|-----|-------|
| `ROLE_USER` | Portföy yönetimi, haber okuma, piyasa verileri |
| `ROLE_ADMIN` | Kullanıcı yönetimi, sistem ayarları, tüm veriler |

### 7.3 2FA (İki Faktörlü Kimlik Doğrulama)

- Keycloak üzerinden OTP (Google Authenticator / FreeOTP)
- İlk girişte kullanıcıya 2FA kurulumu önerilir
- "Beni hatırla" (Remember Me) cookie ile desteklenir

### 7.4 LDAP Entegrasyonu

Keycloak arkasında **OpenLDAP** dizin sunucusu:
- Kullanıcı bilgileri LDAP'a senkronize
- Grup bazlı rol atama

---

## 8. Veritabanı Şeması (Özet)

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│    users     │     │   portfolios     │     │ portfolio_items  │
│──────────────│     │──────────────────│     │──────────────────│
│ id (PK)      │─1:N─│ id (PK)          │─1:N─│ id (PK)          │
│ keycloak_id  │     │ user_id (FK)     │     │ portfolio_id(FK) │
│ email        │     │ name             │     │ instrument_type  │
│ full_name    │     │ created_at       │     │ symbol           │
│ role         │     │ updated_at       │     │ quantity         │
└──────────────┘     └──────────────────┘     │ purchase_price   │
                                              │ purchase_date    │
┌──────────────────┐                          └──────────────────┘
│  news_articles   │     ┌──────────────────┐
│──────────────────│     │  market_data     │
│ id (PK)          │     │──────────────────│
│ title            │     │ id (PK)          │
│ summary          │     │ symbol           │
│ source           │     │ instrument_type  │
│ category         │     │ price            │
│ published_at     │     │ currency         │
│ url              │     │ timestamp        │
└──────────────────┘     │ source           │
                         └──────────────────┘
┌──────────────────────┐
│  historical_prices   │
│──────────────────────│
│ id (PK)              │
│ symbol               │
│ instrument_type      │
│ date                 │
│ open_price           │
│ close_price          │
│ high_price           │
│ low_price            │
│ volume               │
└──────────────────────┘
```

---

## 9. API Endpoint Listesi (Özet)

### 9.1 Haber Modülü

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/api/news` | Haber listesi (sayfalı) |
| GET | `/api/news/{id}` | Haber detay |
| GET | `/api/news/categories` | Kategori listesi |
| GET | `/api/news?category={cat}` | Kategoriye göre filtreleme |

### 9.2 Piyasa Verileri

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/api/market/currencies` | Güncel döviz kurları |
| GET | `/api/market/stocks` | Hisse senedi verileri |
| GET | `/api/market/bonds` | Tahvil/bono bilgileri |
| GET | `/api/market/viop` | VIOP enstrümanları |
| GET | `/api/market/funds` | Yatırım fonları |

### 9.3 Tarihsel Veri & Analiz

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/api/historical/{symbol}?from=&to=` | Tarihsel fiyatlar |
| GET | `/api/analysis/ma/{symbol}?period=` | Hareketli ortalama |
| GET | `/api/analysis/trend/{symbol}` | Trend analizi |

### 9.4 Portföy Yönetimi

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/api/portfolios` | Kullanıcının portföyleri |
| POST | `/api/portfolios` | Yeni portföy oluştur |
| GET | `/api/portfolios/{id}` | Portföy detay |
| POST | `/api/portfolios/{id}/items` | Enstrüman ekle |
| DELETE | `/api/portfolios/{id}/items/{itemId}` | Enstrüman sil |
| GET | `/api/portfolios/{id}/summary` | Kâr/zarar & dağılım |

### 9.5 Kullanıcı

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/api/users/me` | Oturumdaki kullanıcı profili |
| PUT | `/api/users/me` | Profil güncelleme |

---

## 10. CI/CD Pipeline (Tasarım)

```
┌──────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Push │───▶│  Build   │───▶│  Test    │───▶│  Docker  │───▶│  Deploy  │
│ (Git)│    │ (Maven/  │    │ (JUnit / │    │  Build   │    │ (Compose │
│      │    │  npm)    │    │  Jest)   │    │  & Push  │    │  / K8s)  │
└──────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
```

- **Araç:** GitHub Actions veya GitLab CI
- **Aşamalar:** Build → Unit Test → Integration Test → Docker Image Build → Deploy

---

## 11. Performans Hedefleri

| Metrik | Hedef |
|--------|-------|
| API yanıt süresi (p95) | < 2 saniye |
| Sayfa yükleme süresi | < 3 saniye |
| Eş zamanlı kullanıcı | ≥ 50 |
| Uptime | %99.5 |

---

## 12. Test Stratejisi

| Tip | Araçlar | Kapsam |
|-----|---------|--------|
| Unit Test | JUnit 5, Mockito | Service & Repository katmanları |
| Integration Test | Spring Boot Test, Testcontainers | API endpoint'leri, DB |
| Frontend Test | Jest, React Testing Library | Bileşen render ve etkileşim |
| E2E Test | Cypress (opsiyonel) | Kritik kullanıcı akışları |
