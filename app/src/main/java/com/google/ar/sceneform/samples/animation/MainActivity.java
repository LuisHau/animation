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

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SkeletonNode;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.AnimationData;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

/** Demuestra jugar modelos animados de FBX. */
public class MainActivity extends AppCompatActivity {

  private static final String TAG = "AnimationSample";
  private static final int ANDY_RENDERABLE = 1;
  private static final int HAT_RENDERABLE = 2;
  private static final String HAT_BONE_NAME = "hat_point";
  private ArFragment arFragment;
  // Modele la clase de cargador para evitar filtrar el contexto de actividad.
  private ModelLoader modelLoader;
  private ModelRenderable andyRenderable;
  private AnchorNode anchorNode;
  private SkeletonNode andy;
  // Controla la reproducción de animación.
  private ModelAnimator animator;
  // Índice de la animación actual en reproducción.
  private int nextAnimation;
  // La interfaz de usuario para reproducir la próxima animación.
  private FloatingActionButton animationButton;
  // La interfaz de usuario para alternar con el sombrero.
  private FloatingActionButton hatButton;
  private Node hatNode;
  private ModelRenderable hatRenderable;

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);

    modelLoader = new ModelLoader(this);

    modelLoader.loadModel(ANDY_RENDERABLE, R.raw.andy_dance);
    modelLoader.loadModel(HAT_RENDERABLE, R.raw.baseball_cap);

    // Cuando se toca un plano, el modelo se coloca en un nodo de anclaje anclado al plano.
    arFragment.setOnTapArPlaneListener(this::onPlaneTap);

    // Agregue un oyente de actualización de cuadros a la escena para controlar el estado de los botones.
    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onFrameUpdate);

    // Una vez que el modelo se coloca en un plano, este botón reproduce las animaciones.
    animationButton = findViewById(R.id.animate);
    animationButton.setEnabled(false);
    animationButton.setOnClickListener(this::onPlayAnimation);

    // Coloca o quítate un sombrero en la cabeza de Andy que muestre cómo usar los Nodos Esqueleto.
    hatButton = findViewById(R.id.hat);
    hatButton.setEnabled(false);
    hatButton.setOnClickListener(this::onToggleHat);
  }

  private void onPlayAnimation(View unusedView) {
    if (animator == null || !animator.isRunning()) {
      AnimationData data = andyRenderable.getAnimationData(nextAnimation);
      nextAnimation = (nextAnimation + 1) % andyRenderable.getAnimationDataCount();
      animator = new ModelAnimator(data, andyRenderable);
      animator.start();
      Toast toast = Toast.makeText(this, data.getName(), Toast.LENGTH_SHORT);
      Log.d(
          TAG,
          String.format(
              "Starting animation %s - %d ms long", data.getName(), data.getDurationMs()));
      toast.setGravity(Gravity.CENTER, 0, 0);
      toast.show();
    }
  }

  /*
   * Se utiliza como escucha para setOnTapArPlaneListener.
   */
  private void onPlaneTap(HitResult hitResult, Plane unusedPlane, MotionEvent unusedMotionEvent) {
    if (andyRenderable == null || hatRenderable == null) {
      return;
    }
    // Crea el ancla.
    Anchor anchor = hitResult.createAnchor();

    if (anchorNode == null) {
      anchorNode = new AnchorNode(anchor);
      anchorNode.setParent(arFragment.getArSceneView().getScene());

      andy = new SkeletonNode();

      andy.setParent(anchorNode);
      andy.setRenderable(andyRenderable);
      hatNode = new Node();

      // Adjunta un nodo al hueso. Este nodo toma la escala interna del hueso, por lo que cualquier
      // los renderables deben agregarse a los nodos secundarios con el reinicio de pose mundial.
      // Esto también permite ajustar la posición relativa al hueso.
      Node boneNode = new Node();
      boneNode.setParent(andy);
      andy.setBoneAttachment(HAT_BONE_NAME, boneNode);
      hatNode.setRenderable(hatRenderable);
      hatNode.setParent(boneNode);
      hatNode.setWorldScale(Vector3.one());
      hatNode.setWorldRotation(Quaternion.identity());
      Vector3 pos = hatNode.getWorldPosition();

      // Baje el sombrero sobre las antenas.
      pos.y -= .1f;

      hatNode.setWorldPosition(pos);
    }
  }

  //Llamado en cada cuadro, controla el estado de los botones.

  private void onFrameUpdate(FrameTime unusedframeTime) {
    // Si el modelo aún no se ha colocado, deshabilite los botones.
    if (anchorNode == null) {
      if (animationButton.isEnabled()) { //El botón de animación está habilitado
        //botón de animación establece la lista de tonos de fondo (valor de la lista de estado de color de
        // (gráficos de Android Color GRIS));
        animationButton.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.GRAY));
        //conjunto de botones de animación habilitado --> falso;
        animationButton.setEnabled(false);
        //conjunto de botones de sombrero Lista de tonos de fondo
        //valor de la lista de estado de color de (gráficos de Android Color GRIS.
        hatButton.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.GRAY));
        //conjunto de botones de sombrero habilitado --> falso;
        hatButton.setEnabled(false);
      }
    } else {
      if (!animationButton.isEnabled()) {
        animationButton.setBackgroundTintList(
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent)));
        animationButton.setEnabled(true);
        hatButton.setEnabled(true);
        hatButton.setBackgroundTintList(
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));
      }
    }
  }

  private void onToggleHat(View unused) {
    if (hatNode != null) {
      hatNode.setEnabled(!hatNode.isEnabled());

      // Establezca el estado del botón del sombrero en función del nodo del sombrero.
      if (hatNode.isEnabled()) { //el nodo del sombrero esta habilitado
        hatButton.setBackgroundTintList(
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));
      } else {
        hatButton.setBackgroundTintList(
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent)));
      }
    }
  }

  void setRenderable(int id, ModelRenderable renderable) {
    if (id == ANDY_RENDERABLE) {
      this.andyRenderable = renderable;
    } else {
      this.hatRenderable = renderable;
    }
  }

  void onException(int id, Throwable throwable) {
    Toast toast = Toast.makeText(this, "Unable to load renderable: " + id, Toast.LENGTH_LONG);
    toast.setGravity(Gravity.CENTER, 0, 0);
    toast.show();
    Log.e(TAG, "Unable to load andy renderable", throwable);
  }
}
