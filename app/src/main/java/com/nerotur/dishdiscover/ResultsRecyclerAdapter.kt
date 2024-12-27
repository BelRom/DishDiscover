package com.nerotur.dishdiscover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nerotur.dishdiscover.databinding.ItemClassificationResultBinding
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.vision.classifier.Classifications
import kotlin.math.min

//Отображает результаты классификации с помощью RecyclerView.
class ResultsRecyclerAdapter : RecyclerView.Adapter<ResultsRecyclerAdapter.ViewHolder>() {
    companion object {
        private const val NO_VALUE = "--"
    }

    //Список категорий, которые будут отображены
    private var categories: MutableList<Category?> = mutableListOf()
    //Номер текущей используемой модели (например, MobileNetV1 или Food)
    private var model: Int? = null
    //Количество элементов для отображения (по умолчанию 2).
    private var adapterSize: Int = 2

    //Обновляет результаты:
    //Инициализирует список категорий пустыми значениями.
    //Сортирует категории по индексу и выбирает только те, которые вмещаются в адаптер (adapterSize).
    //Сохраняет используемую модель (model).
    //Если listClassifications пустой, категории остаются пустыми.
    fun updateResults(listClassifications: List<Classifications>?, model: Int?) {
        categories = MutableList(adapterSize) { null }
        listClassifications?.let { it ->
            if (it.isNotEmpty()) {
                val sortedCategories = it[0].categories.sortedBy { it?.index }
                val min = min(sortedCategories.size, categories.size)
                for (i in 0 until min) {
                    this.model = model
                    categories[i] = sortedCategories[i]
                }
            }
        }
    }

    //Создает ViewHolder:
    //Использует ViewBinding для инфляции макета элемента item_classification_result.xml.
    //Возвращает новый экземпляр ViewHolder.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClassificationResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    //Создает ViewHolder:
    //Использует ViewBinding для инфляции макета элемента item_classification_result.xml.
    //Возвращает новый экземпляр ViewHolder.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        categories[position].let { category ->
            if (model == ImageHelper.MODEL_FOOD) {
                holder.bind(category?.displayName, category?.score)
            } else {
                holder.bind(category?.label, category?.score)
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    //Привязывает данные к элементу списка:
    //Проверяет текущую модель (model).
    //Для модели MODEL_FOOD отображает displayName (человекочитаемое имя категории).
    //Для других моделей отображает label (имя метки).
    //Передает метку и уверенность в метод bind ViewHolder'а.
    inner class ViewHolder(private val binding: ItemClassificationResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(label: String?, score: Float?) {
            with(binding) {
                tvLabel.text = label ?: NO_VALUE
                tvScore.text = if (score != null) String.format("%.2f", score) else NO_VALUE
            }
        }
    }
}