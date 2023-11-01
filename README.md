# TensorFlow-Lite-Object-Detection
This is an Android application that uses TensorFlow Lite to perform object detection using the device's camera. It provides real-time object detection and the ability to capture snapshots of the camera feed with detected objects.

## Getting Started

You can follow the steps below to build and run the app on your Android device or emulator.

### Prerequisites

Before getting started, make sure you have the following:

- Android Studio installed on your development machine.
- Android device or emulator for testing.

### Build and Run

1. Clone the repository to your local machine:

2. Open Android Studio and select "Open an existing Android Studio project."

3. Navigate to the cloned repository and select the "TensorFlow-Lite-Object-Detection" directory.

4. Connect your Android device to your computer or start an Android emulator.

5. Build and run the app by clicking the "Run" button in Android Studio. You can choose your connected device or emulator as the deployment target.

6. The app should launch on your device, and you can use it to perform real-time object detection and capture snapshots.

### Assumptions

This app assumes the following:
1. The app assumes that the user's Android device has a working camera.
2. The device's camera is used for capturing real-time images to perform object detection.
3. The user grants necessary permissions for camera and storage access when prompted.
4. The app assumes that a sufficient number of objects will be present in the camera's field of view for meaningful object detection.
5. The device has adequate hardware capabilities to support real-time image analysis and TensorFlow processing.
6. Users have basic familiarity with how object detection apps work and understand that the accuracy of object detection may vary depending on factors such as lighting and object size.

### Challenges Faced

During the development of this app, some challenges were encountered and addressed:

- **Permission Handling:** Obtaining camera and storage permissions requires careful handling. We used the PermissionsUtil class to manage and request necessary permissions.

- **Camera Integration:** Integrating the camera with TensorFlow Lite for real-time object detection was challenging. We used the CameraX library to simplify camera management and image analysis.

- **User Experience:** Ensuring a smooth and user-friendly experience when switching between the front and back cameras and setting the detection threshold was a significant consideration.

- **Performance:** Optimizing the app's performance and memory usage to provide real-time object detection on a variety of devices required tuning TensorFlow Lite and the camera setup.

These challenges were addressed by following best practices, thorough testing, and leveraging the capabilities of Android's camera and TensorFlow Lite.

Feel free to explore the code and make improvements or modifications as needed. Happy coding!
