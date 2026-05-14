Suraksha-Setu 🛡️

An Android Women Safety & Emergency Assistance Application built using Kotlin.

📌 Overview

Suraksha-Setu is a smart safety application designed to help users send emergency alerts quickly during dangerous situations. The app supports:

🚨 SOS Emergency Alerts

📍 Live Location Sharing

📳 Shake Gesture Detection

🎙️ Automatic Audio Recording

📨 SMS Alert System

🤝 Volunteer/Responder Mode

🔔 Background Protection Service


The app can trigger SOS alerts either manually using a button or automatically through shake detection.


---

✨ Features

🚨 Emergency SOS

Sends emergency SMS alerts to saved emergency contacts.

Includes live Google Maps location link.


📳 Shake Detection

Detects strong shake gestures using the accelerometer.

Automatically triggers SOS even when app runs in background.


📍 Live Location Tracking

Fetches GPS and network location.

Continuously updates user location.


🎙️ Audio Recording

Records surrounding audio for 30 seconds during emergency.

Useful for collecting evidence.


👥 Safe-Circle Contacts

Add up to 5 emergency contacts.

Contacts are stored locally using SharedPreferences.


🤝 Volunteer Mode

Users can enable volunteer/responder mode.

Simulates verified responder participation.


🔔 Foreground Background Service

Keeps shake detection active even when app is minimized.



---

🛠️ Technologies Used

Language: Kotlin

IDE: Android Studio

Database: SharedPreferences

APIs & Services:

Android Sensor API

Location Services

SMS Manager

MediaRecorder

Firebase Cloud Messaging (FCM)




---

📂 Project Structure

com.suraksha.setu
│
├── MainActivity.kt
├── ShakeService.kt
├── activity_main.xml
├── AndroidManifest.xml
└── res/


---

⚙️ Permissions Used

The application requires the following permissions:

<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.VIBRATE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>


---

🚀 How It Works

1️⃣ Add Emergency Contacts

Users can add up to 5 trusted contacts into the Safe-Circle.

2️⃣ Trigger SOS

SOS can be activated by:

Pressing the SOS button

Shaking the phone


3️⃣ Emergency Actions

When SOS is triggered:

Device vibrates

Audio recording starts

SMS with live location is sent to all contacts



---

📱 SOS Message Example

🚨 EMERGENCY! I need help!

📍 My Location:
https://maps.google.com/?q=12.9716,77.5946

Sent via Suraksha-Setu


---

🔥 Firebase Integration

The app uses Firebase Cloud Messaging (FCM) to generate a device token.

Add Firebase to the project:

1. Create a Firebase project


2. Download google-services.json


3. Place it inside:



app/google-services.json


---

▶️ Installation Steps

Clone the Repository

git clone https://github.com/your-username/suraksha-setu.git

Open in Android Studio

Open Android Studio

Select the project folder


Sync Gradle

Allow Gradle dependencies to download.

Run the Application

Connect Android device/emulator

Click ▶ Run



---

📸 Future Enhancements

🌐 Real-time cloud database integration

📡 Internet-based emergency alerts

🧠 AI-based danger prediction

📷 Automatic photo/video capture

👮 Nearby police station alerts

☁️ Upload recorded audio to cloud



---

🧪 Testing

Tested Features:

✅ SMS Sending

✅ Shake Detection

✅ Audio Recording

✅ Location Tracking

✅ Background Service

✅ Contact Storage



---

👨‍💻 Developer

Developed as a Women Safety & Emergency Assistance Android Application using Kotlin and Android SDK.


---

📜 License

This project is developed for educational and safety purposes.
Free to use and modify for learning.
