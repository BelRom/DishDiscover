# DishDiscover

## Описание
DishDiscover — мобильное приложение, предназначенное для распознавания блюд по изображению и определения их названий на английском языке.
Презентация https://docs.google.com/presentation/d/1xJG-0VkK6VOS1aDI_kT5pceEy5xWaYrJ/edit?usp=share_link&ouid=100419496333606363332&rtpof=true&sd=true

## Цель

Распознавание с помощью мобильного устройства изображения блюда для определения его названия на английском языке.

## Задачи
1. Выбрать предобученную модель на [Kaggle](https://www.kaggle.com/).
2. Изучить библиотеки, которые запускают ML-модели на мобильном устройстве, и выбрать оптимальную.
3. Разработать мобильное приложение.
4. Интегрировать ML-модель в мобильное приложение.
5. Проанализировать классификацию изображений в модели.
6. Запустить ML-модель.
7. Протестировать распознавание блюд ML-моделью.

## Выбранные модели которые использованы в приложении

1. Модель классификации пищевых продуктов AIY

**Описание**:  
Эта модель предназначена для идентификации различных видов пищи на изображениях. Она основана на архитектуре MobileNet и обучена распознавать более 2000 типов блюд.  
[AIY Projects](https://aiyprojects.withgoogle.com/models/)  
[Модель на Kaggle](https://www.kaggle.com/models/google/aiy)

**Применение**:  
Используется для разработки приложений, способных определять название блюда по его изображению, что полезно в кулинарных приложениях и при путешествиях.

2. Модель классификации различных изображений MobileNet V1

**Описание**:  
MobileNet V1 — это семейство нейронных сетевых архитектур, разработанных для эффективной классификации изображений на мобильных устройствах. Они характеризуются небольшим размером, низкой задержкой и малым потреблением энергии, что делает их идеальными для встроенных систем и мобильных приложений.  
[GitHub](https://github.com/tensorflow/models/blob/master/research/slim/nets/mobilenet_v1.md)  
[Модель на Kaggle](https://www.kaggle.com/models/google/mobilenet-v1)

**Особенности**:
- Использует глубинно-разделяемые свёртки (depthwise separable convolutions) для уменьшения количества параметров и вычислительных операций.
- Параметризуется с помощью коэффициентов ширины и разрешения, позволяя адаптировать модель под конкретные ограничения ресурсов устройства.

**Применение**:  
Широко используется в задачах классификации изображений, детекции объектов и других компьютерных зрительных приложениях на мобильных и встроенных устройствах.

## Выбранная платформа

### TensorFlow Lite (TFLite)

**Описание**:  
TensorFlow Lite (TFLite) — это облегчённая версия платформы TensorFlow, оптимизированная для запуска моделей машинного обучения на мобильных устройствах и встроенных системах, включая Android. Это библиотека, которая позволяет интегрировать и использовать готовые или кастомные модели машинного обучения прямо в Android-приложении.

**Особенности TensorFlow Lite**:

- **Компактность**:
    - Оптимизирован для работы на устройствах с ограниченными ресурсами, таких как смартфоны, IoT-устройства и микроконтроллеры.
    - Размер моделей значительно меньше по сравнению с TensorFlow.

- **Высокая производительность**:
    - Использует оптимизированные библиотеки для быстрого выполнения предсказаний.
    - Поддерживает аппаратное ускорение с помощью GPU, DSP и других процессоров.

- **Универсальность**:
    - Поддерживает широкий спектр задач: классификация изображений, обработка текста, распознавание объектов, генерация речи и т.д.

- **Лёгкая интеграция**:
    - Простые встраиваемые API для запуска моделей в Android-приложениях.

## Области применения

- Знакомство с местной кухней в туристических поездках в экзотических странах.
- Устранение языковых барьеров при поездке в другую страну.

## Инструкции

- В репозитории лежит папка `DishDiscover` с исходным кодом для Android Studio.
- Основной код находится в пакете `com.nerotur.dishdiscover` (классы `CameraFragment` и `ImageHelper`).
- Установочный файл для мобильного устройства — `app-debug.apk`.

### Установка APK файла
Apk https://drive.google.com/file/d/1iGiuAQ2Hzt4zqrf5-d8u2izt1kkKK2nT/view?usp=sharing
1. Скопируйте файл `app-debug.apk` на ваше устройство Android.
2. На устройстве откройте файл `app-debug.apk` для установки.
3. Если появляется сообщение о необходимости разрешить установку из неизвестных источников:
    - Перейдите в настройки устройства.
    - Найдите раздел "Безопасность" или "Приложения" (в зависимости от версии Android).
    - Включите опцию "Разрешить установку из неизвестных источников".
    - Повторите попытку установки.

### Разрешение доступа

- При первом запуске приложение запросит доступ к:
    - Камере: для распознавания блюд в реальном времени.
    - Папке с фотографиями: для анализа изображений из галереи.
- Нажмите "Разрешить", чтобы приложение могло корректно работать.

## Авторы проекта
- Белянин Р.С.
- Бархатова Г.В.
- Борщевский А.В.
- Железнякова Н.В.
- Мищенко Е.А.
- Помазкин М.А.
- Шмелев Д.М.
- Шаповалова Е.В.