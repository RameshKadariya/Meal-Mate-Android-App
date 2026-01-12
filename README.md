# 🍽️ MealMate - Smart Meal Planning & Recipe Management

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)
![Language](https://img.shields.io/badge/Language-Java-orange.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**A comprehensive Android application for meal planning, recipe management, and smart grocery shopping**

[Features](#-features) • [Screenshots](#-screenshots) • [Architecture](#-architecture) • [Installation](#-installation) • [Technologies](#-technologies-used)

</div>

---

## 📖 Overview

MealMate is a feature-rich Android application designed to simplify meal planning and recipe management. Built with modern Android development practices, it integrates Firebase for real-time data synchronization, Google Maps for location services, and follows Material Design 3 guidelines for an intuitive user experience.

The application addresses common challenges in meal preparation by providing tools for recipe discovery, weekly meal planning, automated shopping list generation, and grocery store location services - all in one seamless platform.

---

## ✨ Features

### 🔐 User Authentication & Profile Management
- **Firebase Authentication** with email/password and Google Sign-In
- **Personalized User Profiles** with customizable preferences
- **Secure Session Management** with automatic token refresh
- **Profile Picture Support** with cloud storage integration

### 📚 Recipe Management
- **Browse & Search Recipes** with advanced filtering options
- **Detailed Recipe View** with ingredients, instructions, and nutritional info
- **Add Custom Recipes** with image upload and categorization
- **Edit & Delete Recipes** with real-time synchronization
- **Recipe Categories** (Breakfast, Lunch, Dinner, Desserts, Snacks)
- **Featured & Popular Recipes** with dynamic recommendations
- **Shake to Discover** - Shake your device to get random recipe suggestions

### 📅 Meal Planning
- **Weekly Meal Planner** with calendar view
- **Drag & Drop Interface** for easy meal scheduling
- **Meal Prep Reminders** with customizable notifications
- **Breakfast, Lunch & Dinner Slots** for each day
- **Quick Meal Assignment** from saved recipes
- **Meal History Tracking** for repeat planning

### 🛒 Smart Shopping List
- **Auto-Generated Lists** from planned meals
- **Manual Item Addition** with quantity and unit selection
- **Swipe to Delete** gesture for quick item removal
- **Check-off Items** as you shop
- **Categorized Items** (Produce, Dairy, Meat, etc.)
- **Share Lists** via SMS or messaging apps
- **Persistent Storage** with cloud backup

### 🗺️ Grocery Store Locator
- **Interactive Google Maps** integration
- **Find Nearby Stores** using GPS location
- **Store Details** with address, phone, and hours
- **Custom Store Markers** with distance calculation
- **Add/Edit Stores** with location picker
- **Store Favorites** for quick access
- **Navigation Integration** with Google Maps app

### 🔔 Notifications & Reminders
- **Meal Prep Alerts** at scheduled times
- **Shopping Reminders** before grocery trips
- **Recipe Suggestions** based on meal plans
- **Boot-Completed Receiver** for persistent reminders
- **Customizable Notification Sounds** and vibration patterns

### 🎨 User Interface & Experience
- **Material Design 3** components and theming
- **Smooth Animations** with Lottie
- **Responsive Layouts** for all screen sizes
- **Dark Mode Support** (system-based)
- **Intuitive Navigation** with bottom navigation bar
- **Swipe Gestures** for common actions
- **Loading States** with skeleton screens
- **Error Handling** with user-friendly messages

---

## 📱 Screenshots

> - Splash Screen & Login
> - Recipe Browser & Details
> - Meal Planner Calendar
> - Shopping List
> - Store Locator Map
> - User Profile

---

## 🏗️ Architecture

### Design Pattern
MealMate follows the **Model-View-Controller (MVC)** architecture pattern with a repository layer for data abstraction.

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  (Activities, Adapters, Dialogs, Custom Views)              │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    Business Logic Layer                      │
│         (Helpers, Managers, Utilities)                       │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                      Data Layer                              │
│  (Repositories, Models, Firebase, Local Storage)            │
└─────────────────────────────────────────────────────────────┘
```

### Project Structure
```
app/src/main/java/com/raka/mealmate/
│
├── 📱 Activities/              # UI Screens
│   ├── SplashScreen.java       # App entry point
│   ├── LoginActivity.java      # User authentication
│   ├── RegisterActivity.java   # New user registration
│   ├── MainActivity.java       # Dashboard & navigation
│   ├── RecipeBrowserActivity.java
│   ├── RecipeDetailActivity.java
│   ├── MealPlannerActivity.java
│   ├── ShoppingListActivity.java
│   ├── StoresMapActivity.java
│   ├── AddRecipeActivity.java
│   ├── AddEditStoreActivity.java
│   └── ProfileActivity.java
│
├── 🔄 adapters/                # RecyclerView Adapters
│   ├── RecipeAdapter.java
│   ├── MealAdapter.java
│   ├── ShoppingListAdapter.java
│   ├── StoreCardAdapter.java
│   ├── IngredientAdapter.java
│   ├── InstructionAdapter.java
│   ├── CategoryAdapter.java
│   └── FeaturedRecipeAdapter.java
│
├── 📦 models/                  # Data Models
│   ├── Recipe.java
│   ├── MealPlan.java
│   ├── ShoppingItem.java
│   ├── Store.java
│   ├── UserProfile.java
│   ├── Ingredient.java
│   └── Category.java
│
├── 🗄️ repositories/           # Data Access Layer
│   └── MealPlanRepository.java
│
├── 🛠️ helpers/                # Utility Classes
│   ├── NotificationHelper.java
│   ├── ShakeDetector.java
│   ├── StoreManager.java
│   └── SwipeToGestureCallback.java
│
├── 💬 dialogs/                # Custom Dialogs
│   └── MealPlanDialog.java
│
└── 📡 receivers/              # Broadcast Receivers
    └── MealPrepReceiver.java
```

### Key Components

#### Data Flow
1. **User Interaction** → Activity receives input
2. **Activity** → Calls repository or helper methods
3. **Repository** → Communicates with Firebase
4. **Firebase** → Returns data via callbacks
5. **Activity** → Updates UI through adapters
6. **Adapter** → Renders data in RecyclerView

#### Firebase Integration
- **Authentication**: Email/password and Google OAuth
- **Realtime Database**: Cloud data storage with offline persistence
- **Database Structure**:
```
mealmate/
├── users/
│   └── {userId}/
│       ├── profile/
│       │   ├── name
│       │   ├── email
│       │   └── photoUrl
│       ├── recipes/
│       │   └── {recipeId}/
│       ├── mealPlans/
│       │   └── {planId}/
│       └── shoppingList/
│           └── {itemId}/
└── stores/
    └── {storeId}/
```

---

## 🚀 Installation

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK** 8 or higher
- **Android SDK** with API level 35
- **Google Maps API Key** ([Get it here](https://console.cloud.google.com/google/maps-apis))
- **Firebase Project** ([Create one here](https://console.firebase.google.com/))

### Setup Instructions

#### 1. Clone the Repository
```bash
git clone https://github.com/RameshKadariya/Meal-Mate-Android-App.git
cd Meal-Mate-Android-App
```

#### 2. Configure Google Maps API Key

Create or edit `local.properties` in the root directory:
```properties
sdk.dir=/path/to/your/Android/Sdk
MAPS_API_KEY=your_google_maps_api_key_here
```

**To get your API key:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable **Maps SDK for Android**
4. Create credentials → API Key
5. Restrict the key to Android apps (recommended)
6. Add your app's SHA-1 fingerprint

#### 3. Setup Firebase

**Step 1: Create Firebase Project**
1. Visit [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" and follow the wizard
3. Add an Android app with package name: `com.raka.mealmate`

**Step 2: Download Configuration**
1. Download `google-services.json`
2. Place it in the `app/` directory

**Step 3: Enable Services**
- **Authentication**:
  - Go to Authentication → Sign-in method
  - Enable Email/Password
  - Enable Google Sign-In
  - Add your SHA-1 fingerprint for Google Sign-In
  
- **Realtime Database**:
  - Go to Realtime Database → Create Database
  - Start in test mode (configure rules later)
  - Note your database URL

**Step 4: Configure Security Rules** (Important for production)
```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid",
        ".write": "$uid === auth.uid"
      }
    },
    "stores": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

#### 4. Get SHA-1 Fingerprint (for Google Sign-In)

**Debug SHA-1:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Release SHA-1:**
```bash
keytool -list -v -keystore /path/to/your/keystore.jks -alias your_alias
```

Add the SHA-1 to Firebase Console → Project Settings → Your App

#### 5. Build the Project

**Using Android Studio:**
1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Click Run ▶️ or press `Shift + F10`

**Using Command Line:**
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing configuration)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

#### 6. Run the App
- Connect an Android device or start an emulator
- Run the app from Android Studio
- Create an account or sign in with Google
- Grant necessary permissions (Location, Notifications)

---

## 🛠️ Technologies Used

### Core Technologies
| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 8 | Primary programming language |
| **Android SDK** | API 24-35 | Android framework |
| **Gradle** | 8.6.1 | Build automation |
| **Material Design 3** | 1.12.0 | UI components |

### Firebase Services
| Service | Purpose |
|---------|---------|
| **Firebase Authentication** | User authentication & authorization |
| **Firebase Realtime Database** | Cloud data storage & sync |
| **Firebase BOM** | Dependency version management |

### Google Services
| Service | Purpose |
|---------|---------|
| **Google Maps SDK** | Interactive maps & location |
| **Play Services Location** | GPS & location services |
| **Play Services Auth** | Google Sign-In |
| **Maps Utils** | Map clustering & utilities |

### UI & Media Libraries
| Library | Version | Purpose |
|---------|---------|---------|
| **Lottie** | 6.3.0 | JSON-based animations |
| **Glide** | 4.16.0 | Image loading & caching |
| **CircleImageView** | 3.1.0 | Circular profile images |
| **Blurry** | 4.0.1 | Image blur effects |
| **CardView** | 1.0.0 | Card-based layouts |

### Networking
| Library | Version | Purpose |
|---------|---------|---------|
| **Retrofit** | 2.9.0 | HTTP client |
| **Gson Converter** | 2.9.0 | JSON serialization |

### Testing
| Library | Purpose |
|---------|---------|
| **JUnit** | Unit testing |
| **Espresso** | UI testing |
| **AndroidX Test** | Testing framework |

### Build Configuration
```gradle
minSdk: 24 (Android 7.0 Nougat)
targetSdk: 35 (Android 15)
compileSdk: 35
Java Version: 1.8
ViewBinding: Enabled
MultiDex: Enabled
```

---

## 🔐 Security & Privacy

### API Key Protection
- API keys stored in `local.properties` (gitignored)
- Keys injected at build time via Gradle
- No hardcoded credentials in source code

### Firebase Security
- Authentication required for all database operations
- User-specific data isolated by UID
- Security rules enforce read/write permissions
- SSL/TLS encryption for all network traffic

### Permissions
The app requests the following permissions:
- `INTERNET` - Network communication
- `ACCESS_FINE_LOCATION` - GPS for store locator
- `ACCESS_COARSE_LOCATION` - Approximate location
- `READ_CONTACTS` - Share recipes with contacts
- `SEND_SMS` - Share shopping lists via SMS
- `SCHEDULE_EXACT_ALARM` - Meal prep reminders
- `RECEIVE_BOOT_COMPLETED` - Restore reminders after reboot

### ProGuard
Release builds use ProGuard for:
- Code obfuscation
- Unused code removal
- Optimization

---

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
```bash
./gradlew jacocoTestReport
```

---

## 📦 Building for Release

### 1. Generate Signing Key
```bash
keytool -genkey -v -keystore mealmate-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mealmate
```

### 2. Configure Signing in `app/build.gradle.kts`
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("path/to/mealmate-release.jks")
            storePassword = "your_store_password"
            keyAlias = "mealmate"
            keyPassword = "your_key_password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 3. Build Release APK
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

---

## 🚧 Known Issues & Limitations

- Recipe images require internet connection
- Google Maps requires valid API key
- Notifications may not work on some custom ROMs
- Shake detection sensitivity varies by device

---

## 🔮 Future Enhancements

- [ ] Migration to Kotlin
- [ ] MVVM architecture with ViewModel
- [ ] Room database for offline support
- [ ] Jetpack Compose UI
- [ ] Barcode scanner for ingredients
- [ ] Nutrition tracking & calorie counter
- [ ] Recipe sharing with other users
- [ ] Voice-guided cooking mode
- [ ] Multi-language support
- [ ] Tablet optimization
- [ ] Wear OS companion app

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License - Copyright (c) 2026 Ramesh Kadariya
```

---

## 👤 Author

**Ramesh Kadariya**

- 🌐 Website: [https://www.rameshkadariya.com.np](https://www.rameshkadariya.com.np)
- 📧 Email: [rameshkadariya4444@gmail.com](mailto:rameshkadariya4444@gmail.com)
- 💼 GitHub: [@RameshKadariya](https://github.com/RameshKadariya)

---

## 🙏 Acknowledgments

- **Recipe Data**: [TheMealDB API](https://www.themealdb.com/)
- **Icons & Illustrations**: Material Design Icons
- **Animations**: LottieFiles community
- **Inspiration**: Modern meal planning applications

---

## 📞 Contact & Support

For questions, suggestions, or collaboration opportunities:

- **Email**: rameshkadariya4444@gmail.com
- **Website**: https://www.rameshkadariya.com.np
- **GitHub Issues**: [Report a bug or request a feature](https://github.com/RameshKadariya/Meal-Mate-Android-App/issues)

---

## 🌟 Show Your Support

If you find this project helpful or interesting, please consider:
- ⭐ Starring the repository
- 🍴 Forking for your own projects
- 📢 Sharing with others
- 🐛 Reporting bugs or suggesting features

---

<div align="center">

**Built with ❤️ by Ramesh Kadariya**

*This is a portfolio project demonstrating Android development skills including Firebase integration, Google Maps SDK, Material Design, and modern Android architecture patterns.*

</div>
