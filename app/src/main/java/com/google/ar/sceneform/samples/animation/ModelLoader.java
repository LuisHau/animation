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

import android.util.Log;
import android.util.SparseArray;
import com.google.ar.sceneform.rendering.ModelRenderable;
import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;

/**
 * Modele la clase de cargador para evitar pérdidas de memoria de la actividad. Controlador de actividad y fragmentos
 * las clases tienen un ciclo de vida controlado por el subproceso de la interfaz de usuario. Cuando una referencia a uno de estos
 * Se accede a los objetos mediante un subproceso de fondo que se "filtró". Usando esa referencia a un
 * El objeto vinculado al ciclo de vida después de que Android cree que se ha "destruido" puede producir errores. También
 * evita que la actividad o el fragmento se recolecte basura, lo que puede perder la memoria
 * permanentemente si la referencia se mantiene en el ámbito singleton.
 *
 * <p>Para evitar esto, use una clase no anidada que no sea una actividad ni un fragmento. Mantener un débil
 *  * referencia a la actividad o fragmento y úselo cuando haga llamadas que afecten a la IU.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class ModelLoader {
  private static final String TAG = "ModelLoader";
  private final SparseArray<CompletableFuture<ModelRenderable>> futureSet = new SparseArray<>();
  private final WeakReference<MainActivity> owner;

  ModelLoader(MainActivity owner) {
    this.owner = new WeakReference<>(owner);
  }

  /**
   * Comienza a cargar el modelo especificado. El resultado de la carga se devuelve de forma asincrónica a través de
   * {@link MainActivity#setRenderable(int, ModelRenderable)} or {@link
   * MainActivity#onException(int, Throwable)}.
   *
   * <p>Se pueden cargar varios modelos a la vez especificando identificadores separados para diferenciar el
   * resultado en devolución de llamada.
   *
   * @param id El ID de esta llamada a loadModel.
   * @param resourceId la identificación del recurso del .sfb para cargar.
   * @return true if Se inició la carga.
   */
  boolean loadModel(int id, int resourceId) {
    MainActivity activity = owner.get();
    if (activity == null) {
      Log.d(TAG, "Activity is null.  Cannot load model.");
      return false;
    }
    CompletableFuture<ModelRenderable> future =
        ModelRenderable.builder()
            .setSource(owner.get(), resourceId)
            .build()
            .thenApply(renderable -> this.setRenderable(id, renderable))
            .exceptionally(throwable -> this.onException(id, throwable));
    if (future != null) {
      futureSet.put(id, future);
    }
    return future != null;
  }

  ModelRenderable onException(int id, Throwable throwable) {
    MainActivity activity = owner.get();
    if (activity != null) {
      activity.onException(id, throwable);
    }
    futureSet.remove(id);
    return null;
  }

  ModelRenderable setRenderable(int id, ModelRenderable modelRenderable) {
    MainActivity activity = owner.get();
    if (activity != null) {
      activity.setRenderable(id, modelRenderable);
    }
    futureSet.remove(id);
    return modelRenderable;
  }
}
