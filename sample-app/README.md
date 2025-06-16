# Zscaler SDK Sample App - Android  

## Overview  
This repository provides a sample Android application to demonstrate the integration and usage of the Zscaler SDK for Mobile Apps.

---

## Prerequisites  
Before you start, ensure you have the following:  

- **GitHub Personal Access Token**: Required for downloading the SDK.  
- **Android Studio**: Latest version recommended.  
- **Gradle Installed**: Ensure your setup uses the latest supported version.  
- **A Test Device or Emulator**: To run the sample app.  

---

## Steps to Run the Sample App  

### Step 1: Clone the Repository  
Use the following command to clone the repository to your local machine:  
```bash
git clone https://github.com/zscaler/zscaler-sdk-android.git
```

### Step 2: Open the Sample App in Android Studio
Navigate to the folder containing the sample app:
```
cd zscaler-sdk-android/sample-app
```
Open the project in Android Studio by selecting the build.gradle or settings.gradle file.

### Step 3: Configure gradle.properties for GitHub Credentials
Ensure your GitHub credentials are added to the gradle.properties file (at ~/.gradle/gradle.properties):

### Step 4: Sync Gradle Dependencies
In Android Studio, click "Sync Now" when prompted to resolve project dependencies.
Wait for the sync process to complete.

### Step 5: Build and Run the Application
Select a target device or emulator from the Android Studio toolbar.
Click the Run button ▶️ to compile and launch the sample app.

### Step 6: Test the Sample App
Explore the app to see how the Zscaler SDK is integrated to secure network communications.Use the app's pre-built demonstration features for insight into the SDK’s functionality.

### Notes
Credentials Configuration: Make sure to add the App Key and Access Token. For more details, please refer https://help.zscaler.com/zsdk/zsdk-integration-guide-android


### SDK Version:
The sample app uses the latest version of the Zscaler SDK (latest.release). Update manually in build.gradle if required.