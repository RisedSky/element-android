# Discord UI Build and Testing Guide

## Build Requirements

### Prerequisites
- **Android Studio**: Latest stable version (Hedgehog 2023.1.1 or newer)
- **JDK 17**: Required for this project
- **Android SDK**: API 34 (with minimum API 21 support)
- **Internet Connection**: Required for downloading dependencies

### Network Dependencies
This project requires access to:
- `dl.google.com` - Android Gradle Plugin
- `maven.google.com` - Android dependencies
- `repo1.maven.org` - Maven Central
- Firebase and Google Services repositories

## Build Steps

### 1. Local Environment Setup
```bash
git clone https://github.com/RisedSky/element-android.git
cd element-android
```

### 2. Android Studio Setup
1. Open Android Studio
2. Select "Open an existing project" 
3. Navigate to the `element-android` folder
4. Wait for Gradle sync to complete (first sync may take 5-10 minutes)

### 3. Build Variant Selection
In **View > Tool Windows > Build Variants**:
- **Recommended**: `vector-app` module → `fdroidDebug` (no Google services required)
- **Alternative**: `vector-app` module → `gplayDebug` (requires Google Play Services)

### 4. Build Commands
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleFdroidDebug

# Install to connected device
./gradlew installFdroidDebug

# Build and run
./gradlew installFdroidDebug && adb shell am start -n im.vector.app.fdroid.debug/.features.MainActivity
```

## Discord UI Testing Checklist

### 🔍 Visual Verification
- [ ] Three-panel layout displays correctly
- [ ] Dark Discord color scheme applies
- [ ] Server list shows on left (72dp width)
- [ ] Channel list shows in middle (240dp width)
- [ ] Main content area shows on right

### 🖱️ Interactive Testing

#### Server Panel (Left)
- [ ] Home button responds to taps
- [ ] Add server button (+) opens dialog
- [ ] Server icons display with proper circular styling
- [ ] Server selection changes channel list content
- [ ] Unread badges show on servers when applicable

#### Channel Panel (Middle)  
- [ ] Server header shows current server name
- [ ] Text channels display with # icon
- [ ] Voice channels display with 🔊 icon
- [ ] Channel categories group channels properly
- [ ] Channel selection updates main content area
- [ ] User panel shows at bottom with avatar/status

#### Main Content (Right)
- [ ] Channel header updates when switching channels
- [ ] Search button opens search dialog
- [ ] Members button opens members list
- [ ] Search dialog filters by type (channels/users/messages)
- [ ] Members list groups by online status

#### Dialog Testing
- [ ] **Add Server Dialog**: Join by invite code + Create server options
- [ ] **Search Dialog**: Search across channels, users, messages with proper filtering
- [ ] **Members Dialog**: Members grouped by online/away/busy/offline status

### ⚠️ Known Testing Limitations
- **Network Environment**: CI environments may block required dependencies
- **Matrix Integration**: Discord UI overlays existing Matrix functionality
- **Real Data**: Testing requires actual Matrix account and server data

### 🐛 Common Build Issues

#### Gradle Sync Failures
```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/

# Invalidate Android Studio caches
File > Invalidate Caches and Restart
```

#### Dependency Resolution Issues
- Ensure stable internet connection
- Check firewall/proxy settings
- Verify Android SDK is properly installed

#### Runtime Errors
- Check device/emulator API level (minimum API 21)
- Verify app permissions in device settings
- Check Android Studio logcat for detailed error messages

## File Structure Created

### Main Activity
- `vector/src/main/java/im/vector/app/features/home/HomeDiscordActivity.kt`

### Layouts
- `vector/src/main/res/layout/activity_home_discord.xml`
- `vector/src/main/res/layout/fragment_home_drawer.xml` 
- `vector/src/main/res/layout/fragment_new_home_detail.xml`

### Discord Components
- `vector/src/main/java/im/vector/app/features/home/discord/DiscordServerAdapter.kt`
- `vector/src/main/java/im/vector/app/features/home/discord/DiscordChannelAdapter.kt`
- `vector/src/main/java/im/vector/app/features/home/discord/DiscordSearchDialog.kt`
- `vector/src/main/java/im/vector/app/features/home/discord/DiscordMembersDialog.kt`
- `vector/src/main/java/im/vector/app/features/home/discord/DiscordAddServerDialog.kt`

### Resources
- `vector/src/main/res/values/colors_discord.xml`
- Multiple dialog and item layout files
- Vector drawable icons

## Expected Behavior

The Discord UI should provide:
1. **Familiar Discord Experience**: Three-panel layout with server navigation
2. **Full Interactivity**: Clickable servers, channels, search, and member management  
3. **Visual Authenticity**: Dark theme with Discord's exact color palette
4. **Matrix Compatibility**: All existing Element functionality preserved

## Reporting Issues

If you encounter build or runtime issues:
1. Include full error logs
2. Specify build variant used
3. Include device/emulator information
4. Attach screenshots of any UI issues

This implementation bridges Discord's beloved UX with Element's Matrix capabilities!