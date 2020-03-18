/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.sceneform.samples.animation;

import android.content.res.Configuration;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import com.google.ar.sceneform.SceneView;
import java.io.File;
import java.io.IOException;

/**
 * La clase Video Recorder maneja la grabación del contenido de un SceneView (vista de escena). Utiliza MediaRecorder para
 * codificar el video. La configuración de calidad se puede establecer explícitamente o simplemente usar el perfil de la videocámara
 * clase para seleccionar un conjunto predefinido de parámetros.
 */
public class VideoRecorder {
  private static final String TAG = "VideoRecorder";
  private static final int DEFAULT_BITRATE = 10000000;
  private static final int DEFAULT_FRAMERATE = 30;

  // la grabación de Video Flag es verdadera cuando la grabadora de medios está capturando video.
  private boolean recordingVideoFlag;

  private MediaRecorder mediaRecorder;

  private Size videoSize;

  private SceneView sceneView;
  private int videoCodec;
  private File videoDirectory;
  private String videoBaseName;
  private File videoPath;
  private int bitRate = DEFAULT_BITRATE;
  private int frameRate = DEFAULT_FRAMERATE;
  private Surface encoderSurface;

  public VideoRecorder() {
    recordingVideoFlag = false;
  }

  public File getVideoPath() {
    return videoPath;
  }

  public void setBitRate(int bitRate) {
    this.bitRate = bitRate;
  }

  public void setFrameRate(int frameRate) {
    this.frameRate = frameRate;
  }

  public void setSceneView(SceneView sceneView) {
    this.sceneView = sceneView;
  }

  /**
   * Alterna el estado de la grabación de video.
   *
   * @return true if La grabación ya está activa.
   */
  public boolean onToggleRecord() {
    if (recordingVideoFlag) {
      stopRecordingVideo();
    } else {
      startRecordingVideo();
    }
    return recordingVideoFlag;
  }

  private void startRecordingVideo() {
    if (mediaRecorder == null) {
      mediaRecorder = new MediaRecorder();
    }

    try {
      buildFilename();
      setUpMediaRecorder();
    } catch (IOException e) {
      Log.e(TAG, "Exception setting up recorder", e);
      return;
    }

    // Configurar Surface para MediaRecorder (Grabacion de medios)
    encoderSurface = mediaRecorder.getSurface();

    sceneView.startMirroringToSurface(
        encoderSurface, 0, 0, videoSize.getWidth(), videoSize.getHeight());

    recordingVideoFlag = true;
  }

  private void buildFilename() {
    if (videoDirectory == null) {
      videoDirectory =
          new File(
              Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                  + "/Sceneform");
    }
    if (videoBaseName == null || videoBaseName.isEmpty()) {
      videoBaseName = "Sample";
    }
    videoPath =
        new File(
            videoDirectory, videoBaseName + Long.toHexString(System.currentTimeMillis()) + ".mp4");
    File dir = videoPath.getParentFile();
    if (!dir.exists()) {
      dir.mkdirs();
    }
  }

  private void stopRecordingVideo() {
    // UI
    recordingVideoFlag = false;

    if (encoderSurface != null) {
      sceneView.stopMirroringToSurface(encoderSurface);
      encoderSurface = null;
    }
    // Para de grabar
    mediaRecorder.stop();
    mediaRecorder.reset();
  }

  private void setUpMediaRecorder() throws IOException {

    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

    mediaRecorder.setOutputFile(videoPath.getAbsolutePath());
    mediaRecorder.setVideoEncodingBitRate(bitRate);
    mediaRecorder.setVideoFrameRate(frameRate);
    mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
    mediaRecorder.setVideoEncoder(videoCodec);

    mediaRecorder.prepare();

    try {
      mediaRecorder.start();
    } catch (IllegalStateException e) {
      Log.e(TAG, "Exception starting capture: " + e.getMessage(), e);
    }
  }

  public void setVideoSize(int width, int height) {
    videoSize = new Size(width, height);
  }

  public void setVideoQuality(int quality, int orientation) {
    CamcorderProfile profile = CamcorderProfile.get(quality);
    if (profile == null) {
      profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
    }
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
    } else {
      setVideoSize(profile.videoFrameHeight, profile.videoFrameWidth);
    }
    setVideoCodec(profile.videoCodec);
    setBitRate(profile.videoBitRate);
    setFrameRate(profile.videoFrameRate);
  }

  public void setVideoCodec(int videoCodec) {
    this.videoCodec = videoCodec;
  }
}
