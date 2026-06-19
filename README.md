This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

### Firebase and local data setup

The project is prepared for:

- Firebase Authentication via `dev.gitlive:firebase-auth`
- Firestore via `dev.gitlive:firebase-firestore`
- Room KMP as the offline-first local database foundation
- Kizitonwose Calendar Compose Multiplatform for calendar UI

Android Firebase configuration:

1. Create or connect a Firebase project in the Firebase console.
2. Add an Android app with package name `com.garam.whenwheremeet`.
3. Download `google-services.json`.
4. Place it at `androidApp/google-services.json`.

The Android Google Services Gradle plugin is applied only when that file exists, so local builds without Firebase credentials do not fail.

iOS Firebase configuration:

1. Add an iOS app in Firebase with the bundle identifier used by the Xcode target.
2. Download `GoogleService-Info.plist`.
3. Place it at `iosApp/iosApp/GoogleService-Info.plist`.
4. The Xcode project links the Firebase iOS SDK Swift Package products `FirebaseCore`, `FirebaseAuth`, and `FirebaseFirestore`.

Do not commit Firebase configuration files or secrets. They are ignored by `.gitignore`.

Firebase project:

- Project ID: `whenwheremeet`
- Console: `https://console.firebase.google.com/project/whenwheremeet/overview`
- Android app ID: `1:570265302176:android:6f46e8285b921c112885c5`
- iOS app ID: `1:570265302176:ios:3df1355363c4ba132885c5`
- Firestore database ID: `default`
- Firestore location: `asia-northeast3`

Before testing room creation, create the Cloud Firestore database for the project:

1. Open `https://console.cloud.google.com/datastore/setup?project=whenwheremeet`.
2. Select Cloud Firestore Native mode.
3. Use the project location `asia-northeast3`.
4. Deploy the repository rules with `firebase deploy --only firestore:rules`.

Firestore rules are defined in `firestore.rules`, and `firebase.json` targets the named Firestore database `default`.

Authentication setup:

- Android Google sign-in uses Firebase Auth with Google Sign-In and `androidApp/google-services.json`.
- iOS Google and Apple sign-in use Firebase Auth OAuth providers from the shared iOS implementation.
- Enable `Google`, `Apple`, and `Anonymous` providers in Firebase Console > Authentication > Sign-in method.
- For iOS, link Firebase iOS SDKs required by GitLive Firebase Auth in Xcode and add the Sign in with Apple capability to the app target.
- If switching iOS Google sign-in to the native GoogleSignIn SDK later, make sure `GoogleService-Info.plist` includes `REVERSED_CLIENT_ID` and add that value as a URL scheme in `Info.plist`.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
