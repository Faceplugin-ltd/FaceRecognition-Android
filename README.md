<div align="center">
<img alt="" src="https://github.com/Faceplugin-ltd/FaceRecognition-Javascript/assets/160750757/657130a9-50f2-486d-b6d5-b78bcec5e6e2.png" width=200/>
</div>

# Face Recognition SDK Android with 3D Passive Liveness Detection - Fully On Premise
## Overview
Explore our `face recognition SDK` Top-ranked on NIST FRVT , coupled with an advanced `iBeta level 2 liveness detection` engine that effectively safeguards against **printed photos, video replay, 3D masks, and deepfake threats**, ensuring top-tier security.
<br>This is `on-premise face recognition SDK` which means everything is processed in your phone and **NO** data leaves the device 
<br></br>

## Try this APP on Google Play
<a href="https://play.google.com/store/apps/details?id=ai.faceplugin.recognition" target="_blank">
  <img alt="" src="https://user-images.githubusercontent.com/125717930/230804673-17c99e7d-6a21-4a64-8b9e-a465142da148.png" height=80/>
</a>
<br></br>

<div align="left">
<img src="https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg" alt="Awesome Badge"/>
<img src="https://img.shields.io/static/v1?label=%F0%9F%8C%9F&message=If%20Useful&style=style=flat&color=BC4E99" alt="Star Badge"/>
<img src="https://img.shields.io/github/issues/genderev/assassin" alt="issue"/>
<img src="https://img.shields.io/github/issues-pr/genderev/assassin" alt="pr"/>
</div>

## Screenshots
<div align="left">
<img alt="" src="https://github.com/Faceplugin-ltd/FaceRecognition-LivenessDetection-Android/assets/160750757/5665b865-23fc-4c19-9663-5093a975fc66" width=200/>
<img alt="" src="https://github.com/Faceplugin-ltd/FaceRecognition-LivenessDetection-Android/assets/160750757/250ac71d-0844-4c26-b4b6-8afa6952f60e" width=200/>
<img alt="" src="https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android/assets/160750757/92f4113e-16b0-43e2-b6af-d5fa3c4e56c9" width=200/>
<img alt="" src="https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android/assets/160750757/fc5f985c-cf40-41d7-9ff9-a5aab5898a33" width=200/>
</div>

## On the Youtube
<div align="center">
<a href="http://www.youtube.com/watch?feature=player_embedded&v=qVtdkwtGtqs" target="_blank">
 <img src="http://img.youtube.com/vi/qVtdkwtGtqs/maxresdefault.jpg" alt="Watch the video" width="960" height="520" border="10" />
</a>
</div>

## Install License
  
The code below shows how to use the license: https://github.com/Faceplugin-ltd/FaceRecognition-Android/blob/370ecadae564788eaa84f288e342da742fde0c1a/app/src/main/java/com/faceplugin/facerecognition/MainActivity.kt#L30-L45

Please contact us to get the license.

## Documentation
<details>

<a name="api"><h3>APIs</h3></a>
<h4> Activate SDK using license </h4>

```java
public static native int setActivation(java.lang.String s);
```

<h4> Init model for face recognition and liveness detection </h4>

```java
public static native int init(AssetManager var0);
```
<h4> Convert camera frame in YUV to Bitmap </h4>

```java
public static native Bitmap yuv2Bitmap(byte[] var0, int var1, int var2, int var3);
```
<h4> Run face recognition and liveness detection </h4>

```java
public static native List<FaceBox> faceDetection(Bitmap var0, FaceDetectionParam var1);
```
<h4> Extract feature vector for the enrollment </h4>

```java
public static native byte[] templateExtraction(Bitmap var0, FaceBox var1);
```
<h4> Calculate cosine similarity for the matching </h4>

```java
public static native float similarityCalculation(byte[] var0, byte[] var1);
```

<a name="sdk-code"><h3>SDK Codes</h3></a>
  
  | Code | Status |
  |:------:|------|
  |0|Activate SDK successfully|
  |-1|Invalid License Key |
  |-2|Invalid AppID |
  |-3|Expired License Key|
  |-4|Activation Failed|
  |-5|SDK Failed|

<a name="classes"><h3>Classes</h3></a>
<h4>FaceResult</h4>

  | Type      | Name      | Description |
  |------------------|------------------|------------------|
  | Rect         | rect        | Bounding box for face   |
  | int          | liveness        | Liveness status: 0 for spoof, 1 for real, less than 0 for unknown    |
  | int          | gender        | Gender classification result   |
  | int          | mask        | Mask presence: 0 for no mask, 1 for mask    |
  | int          | age        | Age estimation result    |
  | float          | yaw        |  Yaw angle: -45 to 45 degrees  |
  | float          | roll        | Roll angle: -45 to 45 degrees    |
  | float          | pitch        | Pitch angle: -45 to 45 degrees    |
  | byte[]          | feature        |  2056-byte facial feature data   |
  | byte[]          | faceData        | Encrypted facial data     |
  | int          | orient        | Face orientation: 1 for no rotation, 2 for 90° rotation, 3 for 270° rotation, 4 for 180° rotation     |
  | int          | faceId        | Face ID in the tracking face mode    |

```java
public class FaceResult {
    public Rect rect;
    public int liveness;
    public int gender;
    public int mask;
    public int age;
    public float yaw;
    public float roll;
    public float pitch;
    public byte[] feature;
    public byte[] faceData;
    public int    orient;
    public int faceId;
    
    public FaceResult() {
    }
}
```
</details>

## List of our Products

* **[FaceRecognition-LivenessDetection-Android](https://github.com/Faceplugin-ltd/FaceRecognition-Android)**
* **[FaceRecognition-LivenessDetection-iOS](https://github.com/Faceplugin-ltd/FaceRecognition-iOS)**
* **[FaceRecognition-LivenessDetection-Javascript](https://github.com/Faceplugin-ltd/FaceRecognition-LivenessDetection-Javascript)**
* **[FaceLivenessDetection-Android](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android)**
* **[FaceLivenessDetection-iOS](https://github.com/Faceplugin-ltd/FaceLivenessDetection-iOS)**
* **[FaceLivenessDetection-Linux](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Linux)**
* **[FaceRecognition-LivenessDetection-React](https://github.com/Faceplugin-ltd/FaceRecognition-LivenessDetection-React)**
* **[FaceRecognition-LivenessDetection-Vue](https://github.com/Faceplugin-ltd/FaceRecognition-LivenessDetection-Vue)**
* **[Face Recognition SDK](https://github.com/Faceplugin-ltd/Face-Recognition-SDK)**
* **[Liveness Detection SDK](https://github.com/Faceplugin-ltd/Face-Liveness-Detection-SDK)**
* **[ID Card Recognition](https://github.com/Faceplugin-ltd/ID-Card-Recognition)**

## Contact
<div align="left">
<a target="_blank" href="mailto:info@faceplugin.com"><img src="https://img.shields.io/badge/email-info@faceplugin.com-blue.svg?logo=gmail " alt="faceplugin.com"></a>&emsp;
<a target="_blank" href="https://t.me/faceplugin"><img src="https://img.shields.io/badge/telegram-@faceplugin-blue.svg?logo=telegram " alt="faceplugin.com"></a>&emsp;
<a target="_blank" href="https://wa.me/+14422295661"><img src="https://img.shields.io/badge/whatsapp-faceplugin-blue.svg?logo=whatsapp " alt="faceplugin.com"></a>
</div>
