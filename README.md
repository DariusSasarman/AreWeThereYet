# AreWeThereYet

## What is it?
A **mobile application** designed to **alert you** when you're **approaching your destination**. No more missing your stop on the train, no more constantly checking maps while someone else is driving, no more anxious "are we there yet?" moments.

## Here's a video of it working (Live test) : 

During the recording of this video, I was actually moving between the two points.

### (Sound warning!)

[demo.webm](https://github.com/user-attachments/assets/630acf11-0420-4130-a872-53f768420f65)

The alarm is triggered based on minutes left - not based on percentage.

## How does it work?
The application uses **real-time GPS tracking** to monitor your **journey progress**. Here's the flow:

- **Pick your vehicle** - Choose whether you're traveling by car, train, or by whatever you're using.
- **Set your destination** - Use the integrated Google Maps search to find your target location
- **Relax and travel** - The app continuously tracks your position and calculates your ETA
- **Get alerted** - When you're within **10 minutes** of your destination, an **alarm sounds** to wake you up or grab your attention

The app runs as a **foreground service** with a **persistent notification**, ensuring it keeps working even when your phone is locked or you're using other apps.

## What technologies did I use?
This project is built entirely for **Android** using **Java** and the **Android SDK**.

For location tracking, I leveraged the **Google Play Services Location API** with the **FusedLocationProviderClient** for efficient and accurate GPS updates.

For the map interface, I integrated **Google Maps Android API** with the **MapView** component, allowing users to search and select destinations visually.

The **foreground service** architecture ensures reliable tracking even when the app is in the background, using **location updates every 5 seconds** to maintain accurate ETA calculations.

For the alarm system, I used **MediaPlayer** with the **AudioManager** to play the system alarm sound at maximum volume on the **ALARM audio stream**.

## Smart ETA calculation
The app doesn't just use static speed estimates. It employs a **dynamic ETA algorithm** that:

- **Tracks your actual movement speed** by comparing consecutive GPS positions
- **Adapts to your vehicle type** - Uses default speeds (car: ~50 km/h, train: ~36 km/h, traveling: ~5 km/h) until real movement data is available
- **Continuously recalculates** your estimated arrival time based on your current speed
- **Updates progress percentage** by comparing distance traveled vs. total journey distance

This means the ETA becomes **more accurate** as you travel, adapting to traffic conditions, train speeds, or your traveling pace.

## State machine architecture
The application is built around a **finite state machine** with five distinct states:

- **START** - Initial launch state
- **PICK_VEHICLE** - User selects their mode of transportation
- **PICK_TARGET** - User searches and confirms their destination
- **REACHING_DESTINATION** - Active tracking with live ETA updates
- **FINISHED_EXECUTION** - Destination reached, alarm triggered

This architecture ensures **predictable behavior** and prevents race conditions when transitioning between activities.

## Permission handling
The app properly requests and handles all necessary **runtime permissions**:

- **ACCESS_FINE_LOCATION** and **ACCESS_COARSE_LOCATION** for GPS tracking
- **POST_NOTIFICATIONS** (Android 13+) for foreground service notifications
- **FOREGROUND_SERVICE** and **FOREGROUND_SERVICE_LOCATION** for background tracking

All permission requests include **fallback behavior** if denied, ensuring the app remains functional even with limited permissions.

## Real-time UI updates
The tracking screen updates **every second** with:

- **Current distance** to destination in kilometers
- **Dynamic ETA** in minutes
- **Progress percentage** with visual progress bar
- **Pause/Resume** functionality for when you take breaks

All data is synchronized through **static accessors** in MainActivity, allowing the background service and UI to share state seamlessly.

## Intended behaviour
1. **Launch app** → Pick your vehicle type
2. **Search destination** → Use Google Maps integration to find your target
3. **Confirm and start** → App begins GPS tracking with foreground service
4. **Monitor progress** → View real-time distance, ETA, and progress updates
5. **Approach destination** → When ETA drops below 10 minutes, alarm sounds
6. **Dismiss alarm** → Tap "Thank You" button to stop alarm and complete journey

## Key architectural decisions
- **Foreground service** ensures tracking continues even when phone is locked
- **Static state management** in MainActivity allows cross-activity data sharing
- **Separate activities** for each major state create clear user flow
- **Service lifecycle management** properly handles start/stop/destroy scenarios
- **Notification updates** keep user informed even when app is backgrounded
