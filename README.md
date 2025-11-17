# MobileBrowser 🌐

A modern, feature-rich Android mobile browser built with Kotlin and Jetpack Compose, powered by Mozilla's GeckoView rendering engine.

## ✨ Features

### Core Browsing
- **GeckoView Engine** - Fast and secure web rendering powered by Mozilla's Gecko engine
- **Custom Search Bar** - Intuitive search and URL navigation
- **Tabbed Browsing** - Multiple tab support for efficient multitasking
- **Homepage** - Customizable homepage with quick access to favorite sites

### Privacy & Productivity
- **Bookmarks** - Save and organize your favorite websites
- **History** - Track and manage your browsing history
- **Download Manager** - Built-in download management system
- **Settings** - Comprehensive configuration options

### Advanced Features
- **Sign-In System** - Secure user authentication
- **ML Integration** - Machine learning capabilities for enhanced browsing experience
- **Modern UI** - Material Design 3 with Jetpack Compose

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 8 or higher
- Android SDK with minimum API level 24 (Android 7.0)
- Kotlin 1.9+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/segunOluwatayo/MobileBrowser.git
   cd MobileBrowser
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory and select it

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click the "Run" button in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

## 🏗️ Project Structure

```
MobileBrowser/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/mobilebrowser/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🛠️ Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Rendering Engine**: Mozilla GeckoView
- **Architecture**: MVVM (Model-View-ViewModel)
- **Build System**: Gradle (Kotlin DSL)
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)

## 📦 Key Dependencies

- AndroidX Core KTX
- AndroidX Lifecycle Runtime
- Jetpack Compose (BOM, Material3, UI)
- Mozilla GeckoView
- AndroidX Activity Compose

## 🔧 Configuration

### Application ID
```kotlin
com.example.mobilebrowser
```

### Version
- **Version Code**: 1
- **Version Name**: 1.0

### Compile SDK
API Level 35

## 🌟 Feature Branches

The project is organized with feature branches for modular development:

- `feature/geckoview-setup` - Core browser engine integration
- `feature/Homepage` - Homepage functionality
- `feature/custom-search-bar` - Search and URL input
- `feature/bookmarks` - Bookmark management
- `feature/history` - Browsing history
- `feature/download` - Download functionality
- `feature/Downloadredo` - Enhanced download features
- `feature/Settings` - App settings and preferences
- `feature/signin` - User authentication
- `feature/mlintegration` - Machine learning features

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 Development Workflow

1. Create a feature branch from `main`
2. Implement your feature with appropriate tests
3. Ensure code follows Kotlin coding conventions
4. Submit a pull request with a clear description

## 🔐 Privacy & Security

MobileBrowser is built with privacy in mind:
- No tracking by default
- Secure browsing with GeckoView's built-in security features
- Local data storage with user control

## 📄 License

This project is currently unlicensed. Please contact the repository owner for usage rights.

## 👤 Author

**Segun Oluwatayo**
- GitHub: [@segunOluwatayo](https://github.com/segunOluwatayo)

## 🐛 Known Issues

This project is currently in active development. Check the [Issues](https://github.com/segunOluwatayo/MobileBrowser/issues) page for known bugs and feature requests.

## 📮 Support

For support, please open an issue in the GitHub repository.

## 🗺️ Roadmap

- [ ] Merge all feature branches into main
- [ ] Implement comprehensive unit and UI tests
- [ ] Add screenshot gallery
- [ ] Create detailed documentation
- [ ] Set up CI/CD pipeline
- [ ] Publish to Google Play Store
- [ ] Add multi-language support
- [ ] Implement dark mode
- [ ] Add extension support

## 🙏 Acknowledgments

- Mozilla for the excellent GeckoView engine
- The Android and Jetpack Compose communities
- All contributors who help improve this project

---

**Note**: This project is under active development. Features mentioned may be in various stages of completion across different branches.