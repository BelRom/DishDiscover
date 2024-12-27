package com.nerotur.dishdiscover

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

//отвечает за обработку изображений с помощью TensorFlow Lite (TFLite)
//и классификацию объектов на изображении.
class ImageHelper(
    var threshold: Float = 0.5f, // минимальная уверенность классификации, ниже которой результаты игнорируются.
    var currentDelegate: Int = 0, // определяет, используется ли GPU или CPU.
    var model: Int = 0, // выбор конкретной модели классификации.
    val context: Context,
    val imageHelperListener: Listener?
) {
    //Храним объект ImageClassifier, который используется для классификации изображений.
    private var imageClassifier: ImageClassifier? = null

    //Автоматически вызываем метод setupImageClassifier при создании объекта ImageHelper.
    init {
        setupImageClassifier()
    }

    //Удаляет текущий классификатор, чтобы можно было создать новый
    //(например, при изменении модели или делегата).
    fun clearImageClassifier() {
        imageClassifier = null
    }

    //Создаем параметры для классификатора:
    //threshold — порог уверенности.
    //MAX_RESULT — максимальное количество результатов.
    //Используем 4 потока для обработки.
    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(MAX_RESULT)
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(4)

        //Проверяет, какой делегат используется:
        //CPU (по умолчанию).
        //GPU (если устройство поддерживает).
        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    imageHelperListener?.onError("GPU is not supported on this device")
                }
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        //Выбираем файл модели на основе выбранного типа
        //(например, MobileNetV1 или модель для классификации еды).
        val modelName =
            when (model) {
                MODEL_MOBILENETV1 -> "mobilenetv1.tflite"
                MODEL_FOOD -> "food4.tflite"
                else -> "mobilenetv1.tflite"
            }

        //Создаем экземпляр ImageClassifier с указанной моделью и параметрами.
        //Если возникает ошибка, передает её через onError.
        try {
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            imageHelperListener?.onError(
                "Image classifier failed to initialize. See error logs for details"
            )
        }
    }

    //Метод для выполнения классификации изображения:
    //Подготавливает изображение (TensorImage).
    //Учитывает ориентацию экрана.
    //Передает результаты и время выполнения через onResults.
    fun classify(image: Bitmap, rotation: Int) {
        if (imageClassifier == null) {
            setupImageClassifier()
        }
        var inferenceTime = SystemClock.uptimeMillis()
        val imageProcessor = ImageProcessor.Builder().build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))
        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(rotation))
            .build()

        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        imageHelperListener?.onResults(
            results,
            inferenceTime,
            model
        )
    }

    //Преобразуем ориентацию экрана (Surface.ROTATION_*) в формат, понятный TensorFlow Lite.
    private fun getOrientationFromRotation(rotation: Int) : ImageProcessingOptions.Orientation {
        when (rotation) {
            Surface.ROTATION_270 ->
                return ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 ->
                return ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            Surface.ROTATION_90 ->
                return ImageProcessingOptions.Orientation.TOP_LEFT
            else ->
                return ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

    //Интерфейс для взаимодействия с внешним кодом:
    // обработка ошибок и передача результатов классификации.
    interface Listener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,
            inferenceTime: Long,
            model: Int,
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_FOOD = 1
        const val MAX_RESULT = 2
    }
}