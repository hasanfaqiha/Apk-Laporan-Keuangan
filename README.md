# KeuanganKu - Modern Finance App with Clean Architecture

## 📱 Overview
Aplikasi pencatatan keuangan pribadi dengan desain modern mirip MYBCA, dibangun menggunakan **Clean Architecture** dan teknologi Android terbaru.

## ✨ Features

### Core Features
- **Dashboard Enhanced**: Tampilan total saldo, cash, bank, e-wallet terpisah dengan visualisasi kartu kredit
- **Transaction Management**: Catat pemasukan & pengeluaran dengan kategori lengkap
- **Bill Reminders**: Notifikasi tagihan jatuh tempo & terlambat
- **Analytics & Reports**: Grafik pengeluaran per kategori, tren bulanan, proyeksi
- **Budget Tracking**: Set budget per kategori dengan alert jika melebihi
- **Multi-Account Support**: Cash, Bank Account, E-Wallet, Credit Card, Investment

### UI/UX (MYBCA-inspired)
- **Modern Card Design**: Card-based layout dengan shadow & gradient
- **Color Palette**: BCA blue-inspired (#4F46E5 primary) dengan dark/light theme
- **Bottom Navigation**: 5 tab utama (Dashboard, Transaksi, Analisis, Tagihan, Settings)
- **Smooth Animations**: Transisi halus antar screen
- **Responsive Layout**: Support berbagai ukuran layar

### Security & Sync
- **Firebase Integration**: Sinkronisasi cloud aman untuk backup data
- **Room Database**: Local persistence untuk offline-first experience
- **App Check**: ReCAPTCHA protection untuk keamanan API
- **Secure Authentication**: Firebase Auth dengan email/password

## 🏗️ Architecture

```
app/src/main/java/com/example/
├── domain/                      # Domain Layer (Business Logic)
│   ├── model/                   # Entity classes
│   │   ├── Transaction.kt
│   │   ├── Category.kt
│   │   ├── Bill.kt
│   │   └── FinanceSummary.kt
│   ├── repository/              # Repository interfaces
│   │   └── FinanceRepository.kt
│   └── usecase/                 # Use cases (optional)
│
├── data/                        # Data Layer
│   ├── local/                   # Room Database
│   │   ├── Entities.kt
│   │   ├── FinanceDao.kt
│   │   └── FinanceDatabase.kt
│   ├── remote/                  # Firebase Services
│   │   └── FirebaseService.kt
│   └── repository_impl/         # Repository implementations
│       └── FinanceRepositoryImpl.kt
│
├── presentation/                # Presentation Layer
│   ├── ui/
│   │   ├── screens/             # Compose Screens
│   │   │   ├── DashboardScreen.kt
│   │   │   ├── TransactionsScreen.kt
│   │   │   ├── AnalysisScreen.kt
│   │   │   ├── BillsScreen.kt
│   │   │   └── SettingsScreen.kt
│   │   ├── components/          # Reusable UI Components
│   │   └── theme/               # Theme, Colors, Typography
│   ├── viewmodel/               # ViewModels
│   │   └── FinanceViewModel.kt
│   └── navigation/              # Navigation Graph
│
└── MainActivity.kt              # Entry Point
```

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: Clean Architecture + MVVM
- **Local Database**: Room
- **Cloud Backend**: Firebase (Firestore, Auth, App Check)
- **Dependency Injection**: Manual (bisa upgrade ke Hilt/Koin)
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Compose Navigation

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog atau lebih baru
- JDK 17+
- Firebase project dengan:
  - Firestore Database
  - Authentication
  - App Check (ReCAPTCHA)
  - google-services.json di folder app/

### Setup Steps

1. **Clone Repository**
```bash
git clone <repository-url>
cd KeuanganKu
```

2. **Configure Firebase**
- Download `google-services.json` dari Firebase Console
- Place di `app/google-services.json`
- Enable Authentication (Email/Password)
- Enable Firestore Database
- Setup App Check dengan ReCAPTCHA

3. **Environment Variables**
Create `.env` file di root project:
```env
FIREBASE_APP_CHECK_DEBUG_TOKEN=your_debug_token
```

4. **Build & Run**
```bash
./gradlew assembleDebug
```

## 📊 Database Schema

### Transactions
- id: String (PK)
- title: String
- amount: Double
- type: INCOME|EXPENSE|TRANSFER_IN|TRANSFER_OUT
- accountType: CASH|BANK_ACCOUNT|E_WALLET|CREDIT_CARD|INVESTMENT
- categoryId: String
- date: Long (timestamp)
- note: String
- isDeleted: Boolean
- createdAt: Long
- updatedAt: Long

### Categories
- id: String (PK)
- name: String
- icon: String
- color: String
- type: INCOME|EXPENSE
- isSystem: Boolean
- budgetLimit: Double?

### Bills
- id: String (PK)
- title: String
- amount: Double
- dueDate: Long
- isPaid: Boolean
- paidDate: Long?
- isRecurring: Boolean
- reminderDaysBefore: Int

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

## 🔒 Security Features

1. **Firebase App Check**: Mencegah akses API dari aplikasi tidak resmi
2. **Firestore Rules**: Row-level security berdasarkan user ID
3. **Local Encryption**: Room database encryption (opsional)
4. **Secure Storage**: Credentials di EncryptedSharedPreferences

## 📄 License

MIT License

---

**Version**: 2.0.0  
**Min SDK**: 24  
**Target SDK**: 36
