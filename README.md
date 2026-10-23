# UPI Shop (Android)

A 3-item shop (₹1 each) with a cart and checkout using the **Adyen UPI Component, Android SDK 5.20.0, Advanced flow**.
Kotlin + Jetpack Compose. Package name `com.demo.upishop`.

## How a payment works

1. The cart screen's **Pay** button calls the backend's `/api/paymentMethods` and shows `UPIComponent`.
2. The Component lists the UPI apps it finds on the phone (Smart Intent). If it finds none, it lists every app from `/paymentMethods`.
3. The shopper picks an app and taps Pay → `onSubmit` → backend `/api/payments` returns an **await** action.
4. `component.handleAction()` opens the UPI app, then **polls Adyen itself** until the payment is final.
5. `onAdditionalDetails` → backend `/api/paymentsDetails` → final `resultCode` on the Result screen.

No webhooks are needed for the app to show the result.

## Get the APK (no Android Studio needed)

The workflow in `.github/workflows/build-apk.yml` builds the APK on GitHub.

1. Create a GitHub repo and push this folder to it.
2. Optional: in the repo's **Settings → Secrets and variables → Actions → Variables**, add `BACKEND_URL` = your Vercel URL. It becomes the app's default. You can also skip this and enter the URL in the app.
3. The **Build APK** workflow runs on every push (or run it from the **Actions** tab). It takes about 5 minutes.
4. On your phone, open the repo's **Releases → Latest APK** and tap `UpiShop.apk`. Android will ask you to allow installs from your browser.

## Or build locally

Open the folder in Android Studio and run, or from a terminal with JDK 17 and the Android SDK:

```
./gradlew :app:assembleRelease -PbackendUrl=https://your-project.vercel.app
```

The APK is at `app/build/outputs/apk/release/app-release.apk`.

## First launch

If no backend URL was baked in, tap the **gear icon** and enter your Vercel URL (it must start with `https://`).
The app loads the client key and items from `/api/config`, so **no keys are in the APK**.

## Notes

- **Signing:** the release APK is signed with the debug key unless you set `UPISHOP_KEYSTORE_PATH`, `UPISHOP_KEYSTORE_PASSWORD`, `UPISHOP_KEY_ALIAS` and `UPISHOP_KEY_PASSWORD`. It installs fine for a demo but can't go to the Play Store.
- **Test phone:** use a real phone with a UPI app (GPay, PhonePe, Paytm or BHIM) linked to a bank account. Live payments are real ₹1 debits.
- **Return URL:** `adyencheckout://com.demo.upishop`. It's already in the manifest.
- **Package name:** to change it, edit `namespace` and `applicationId` in `app/build.gradle.kts` and move the Kotlin package folder.
