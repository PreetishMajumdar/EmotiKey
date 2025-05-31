# EmotiKey 😄⌨️  
An intelligent Android keyboard that leverages real-time facial emotion recognition using the front camera to suggest personalized emojis and phrases.

---

## 🚀 Overview
EmotiKey is an innovative virtual keyboard designed for Android that captures user facial expressions using the front camera and uses a neural network-based emotion detection model to infer the user's emotional state. Based on the recognized emotion, the keyboard dynamically suggests appropriate emojis and context-aware phrases, enhancing expressiveness and personalization during typing.

---

## 🧠 How It Works
1. The front-facing camera captures real-time video frames.
2. Frames are preprocessed and passed to a TensorFlow Lite (TFLite) model.
3. The neural network predicts one of the predefined emotional states:
   - 😄 Happy
   - 😢 Sad
   - 😠 Angry
   - 😲 Surprised
   - 😐 Neutral  
4. Based on the detected emotion, EmotiKey provides:
   - A set of matching emojis.
   - A list of emotion-aligned phrases for quick insertion.

---

## 📱 Features
- 📷 **Real-time face emotion detection** from the front camera.
- 🤖 **On-device inference** using a lightweight TFLite neural network model.
- 😊 **Emoji suggestions** tailored to user emotion.
- 💬 **Phrase recommendations** to express current mood.
- 🔒 **All processing happens on-device** — user privacy is respected.
- 🧩 **Compatible with Android 8.0 (Oreo)** and above.

---

## 🛠️ Tech Stack
- Android (Kotlin / Java)
- TensorFlow Lite
- OpenCV (optional for image preprocessing)
- ML Model: Custom-trained CNN converted to `.tflite` format
- CameraX API (for live camera feed)

---

## 🏗️ Project Structure
Here's the complete README.md content formatted for direct copy-paste into your GitHub repository's README file.

Markdown

# EmotiKey 😄⌨️  
An intelligent Android keyboard that leverages real-time facial emotion recognition using the front camera to suggest personalized emojis and phrases.

---

## 🚀 Overview
EmotiKey is an innovative virtual keyboard designed for Android that captures user facial expressions using the front camera and uses a neural network-based emotion detection model to infer the user's emotional state. Based on the recognized emotion, the keyboard dynamically suggests appropriate emojis and context-aware phrases, enhancing expressiveness and personalization during typing.

---

## 🧠 How It Works
1. The front-facing camera captures real-time video frames.
2. Frames are preprocessed and passed to a TensorFlow Lite (TFLite) model.
3. The neural network predicts one of the predefined emotional states:
   - 😄 Happy
   - 😢 Sad
   - 😠 Angry
   - 😲 Surprised
   - 😐 Neutral  
4. Based on the detected emotion, EmotiKey provides:
   - A set of matching emojis.
   - A list of emotion-aligned phrases for quick insertion.

---

## 📱 Features
- 📷 **Real-time face emotion detection** from the front camera.
- 🤖 **On-device inference** using a lightweight TFLite neural network model.
- 😊 **Emoji suggestions** tailored to user emotion.
- 💬 **Phrase recommendations** to express current mood.
- 🔒 **All processing happens on-device** — user privacy is respected.
- 🧩 **Compatible with Android 8.0 (Oreo)** and above.

---

## 🛠️ Tech Stack
- Android (Kotlin / Java)
- TensorFlow Lite
- OpenCV (optional for image preprocessing)
- ML Model: Custom-trained CNN converted to `.tflite` format
- CameraX API (for live camera feed)

---

## 🏗️ Project Structure
EmotiKey/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/emotikey/
│   │   │   │   ├── ui/                 # Keyboard UI components
│   │   │   │   ├── ml/                 # Emotion recognition logic
│   │   │   │   ├── camera/             # Front camera handling
│   │   │   │   └── utils/              # Helper functions
│   │   │   ├── assets/
│   │   │   │   └── model.tflite        # Pretrained emotion model
│   │   │   ├── res/
│   │   │   │   └── layout/             # XML layouts
│   │   │   └── AndroidManifest.xml
├── README.md
└── build.gradle


---

## 🧪 Model Details
- **Architecture**: Convolutional Neural Network (CNN)
- **Input**: 48x48 grayscale face image
- **Output Classes**: Happy, Sad, Angry, Surprised, Neutral
- **Training Dataset**: FER-2013 or custom labeled dataset
- **Converted using**: TensorFlow Lite Converter
- **Inference Time**: ~`<100ms` on most mid-range devices

---

## 🧑‍💻 Installation & Setup
1. Clone the repository:
   ```bash
   git clone [https://github.com/your-username/emotikey.git](https://github.com/your-username/emotikey.git)
   cd emotikeyOpen in Android Studio.
2. Open in Android Studio.
3. Make sure your device has Camera Permission enabled.
4. Place your model file (model.tflite) under:
app/src/main/assets/
5. Build and run on an emulator or physical device with front camera.
