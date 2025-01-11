# IT3943 - Project 3 - SOICT - HUST

## Introduction
<ul>
    <li> Name of Project: Smart Curtain
    <li> Objective:
        <ul>
            <li> Designed and implemented a smart curtain system using ESP32 microcontroller, 
Firebase, and a Kotlin-based Android application
            <li> Provides remote control and 
automation based on environmental conditions such as light intensity. 
        </ul>
</ul>

## Features

- **Remote Curtain Control:** Control curtains from anywhere via the mobile app.
- **Environmental Monitoring:** Sensors provide real-time data on temperature, humidity, and light levels.
- **Automation:** Curtains open or close automatically based on light intensity.
- **Multi-Room Support:** Manage curtains across multiple rooms and houses.
- **Dynamic Configuration:** Add new devices and assign them to specific rooms.
- **Firebase Integration:** Real-time data synchronization between devices and the app.

## System Architecture

The system consists of three main components:

1. **Hardware (ESP32):**
   - Connects to Firebase to send and receive data.
   - Reads environmental data using DHT11 and light sensors.
   - Controls the motor to open or close curtains based on commands.

2. **Mobile Application (Kotlin):**
   - User-friendly interface for controlling curtains and monitoring data.
   - Allows users to add new devices, assign them to rooms, and configure settings.

3. **Cloud (Firebase):**
   - Real-time database for storing curtain states, room assignments, and sensor data.
   - Ensures seamless communication between the app and devices.

---

## Installation

### Prerequisites

### Hardware Requirements
- ESP32 Microcontroller.
- Motor and motor driver (e.g., L298N).
- Sensors: DHT11/DHT22 (temperature & humidity), LDR (light).
- Limit switches.

### Software Requirements
- Arduino IDE or PlatformIO for flashing ESP32 firmware.
- Android Studio for running the Kotlin application.
- Firebase account with a Realtime Database configured.

## Hardware Setup:
1. **Hardware Connections**:  
   - Connect the ESP32 to the sensors (light, temperature, and humidity) and the motor as detailed in the hardware configuration section of the report.  
   - Ensure the limit switches and motor driver are properly wired to avoid malfunction during operation.

2. **Flash the ESP32 Firmware**:  
   - Use the Arduino IDE or PlatformIO to upload the provided firmware to the ESP32.  
   - The firmware file is located at [ESP32 Firmware](ESP32/src/SmartCurtain.cpp).

3. **Configure Wi-Fi and Firebase**:  
   - Open the `SmartCurtain.cpp` file in your IDE.  
   - Update the following constants with your project details:
     ```cpp
     #define WIFI_SSID "<Your Wi-Fi SSID>"
     #define WIFI_PASSWORD "<Your Wi-Fi Password>"
     #define API_KEY "<Your Firebase API Key>"
     #define DATABASE_URL "<Your Firebase Database URL>"
4. **Power On**: Connect the ESP32 to a power source to initialize the device.

### 2. **Cloud Setup**

1. Create a Firebase project.
2. Set up the Realtime Database and note the database URL.
3. Enable Firebase Authentication for secure access.

### 3. **Android Application Setup**

1. Clone the repository and open the Android project in Android Studio.
2. Update the `google-services.json` file with your Firebase configuration.
3. Build and run the application on an Android device.
