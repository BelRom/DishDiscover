package com.nerotur.dishdiscover

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageProxy
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nerotur.dishdiscover.databinding.FragmentCameraBinding
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

//Основной фрагмент который, отвечает за работу
//с камерой и распознавание объектов на изображении.
class CameraFragment : Fragment(), ImageHelper.Listener {

    companion object {
        private const val TAG = "CameraFragment"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    //класс для обработки изображений с помощью TensorFlow Lite.
    private lateinit var imageHelper: ImageHelper
    private lateinit var bitmapBuffer: Bitmap
    private val resultsRecyclerAdapter by lazy { ResultsRecyclerAdapter() }
    // Переменные для управления камерой
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    //Переменные фоновой обработки
    private lateinit var cameraExecutor: ExecutorService

    //Лямбда-функция для обработки результата запроса разрешений на камеру.
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(context, "Permission request granted", Toast.LENGTH_LONG).show()

            } else {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    //Проверяем, есть ли разрешение на использование камеры. Если нет, запрашивает его.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {

            } else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    //Повторная проверка разрешений при возобновлении работы фрагмента.
    override fun onResume() {
        super.onResume()
        val hasPermissions = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) {
            requestPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    //Очищает binding и завершает работу фонового потока при уничтожении представления.
    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    //Создает и возвращает привязанное представление.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    //Настраивает интерфейс: список результатов, элементы управления.
    //Запускает камеру и управление снизу через BottomSheet.
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageHelper = ImageHelper(context = requireContext(), imageHelperListener = this)

        with(fragmentCameraBinding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = resultsRecyclerAdapter
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        fragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }
        initBottomSheetControls()
    }

    //Получаем провайдера камеры и связывает его с жизненным циклом фрагмента.
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    //Настраиваем Preview и ImageAnalysis для обработки потока с камеры.
    private fun initBottomSheetControls() {
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (imageHelper.threshold >= 0.1) {
                imageHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (imageHelper.threshold < 0.9) {
                imageHelper.threshold += 0.1f
                updateControlsUi()
            }
        }
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    imageHelper.currentDelegate = position
                    updateControlsUi()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    /* no op */
                }
            }

        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    imageHelper.model = position
                    updateControlsUi()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    //Преобразуем ImageProxy в Bitmap и передает его в imageHelper для классификации.
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", imageHelper.threshold)
        imageHelper.clearImageClassifier()
    }

    //Метод вызывается при изменении конфигурации устройства, например, при повороте экрана.
    //Обновляет ориентацию камеры (targetRotation) в соответствии с новой ориентацией дисплея.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    //Настраиваем и связывает необходимые компоненты камеры, такие как
    //предварительный просмотр (Preview) и анализ изображений (ImageAnalysis).
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        //Указывает, какая камера будет использоваться
        //(например, фронтальная или основная).
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        //Настраиваем предварительный просмотр изображения с камеры.
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

        //Настраиваем обработку изображений с камеры.
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        classifyImage(image)
                    }
                }
        cameraProvider.unbindAll()

        //Связывает компоненты камеры (предварительный просмотр и анализ изображений)
        //с жизненным циклом фрагмента
        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    //Получаем оринтацию экрана
    private fun getScreenOrientation(): Int {
        val outMetrics = DisplayMetrics()

        val display: Display?
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display = requireActivity().display
            display?.getRealMetrics(outMetrics)
        } else {
            @Suppress("DEPRECATION")
            display = requireActivity().windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(outMetrics)
        }

        return display?.rotation ?: 0
    }

    //Преобразует ImageProxy в Bitmap и передает его в imageHelper для классификации.
    private fun classifyImage(image: ImageProxy) {
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        imageHelper.classify(bitmapBuffer, getScreenOrientation())
    }

    //Отображаем ошибку классификации в интерфейсе.
    @SuppressLint("NotifyDataSetChanged")
    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            resultsRecyclerAdapter.updateResults(null, null)
            resultsRecyclerAdapter.notifyDataSetChanged()
        }
    }

    //Отображает результаты классификации и время выполнения.
    @SuppressLint("NotifyDataSetChanged")
    override fun onResults(results: List<Classifications>?, inferenceTime: Long, model: Int) {
        activity?.runOnUiThread {
            resultsRecyclerAdapter.updateResults(results, model)
            resultsRecyclerAdapter.notifyDataSetChanged()
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                String.format("%d ms", inferenceTime)
        }
    }
}