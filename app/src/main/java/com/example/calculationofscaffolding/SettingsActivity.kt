package com.example.calculationofscaffolding

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calculationofscaffolding.DB.ElementsDatabase
import com.example.calculationofscaffolding.adapters.ElementsSettingsAdapter
import com.example.calculationofscaffolding.models.Element
import com.example.myapplication.databinding.ActivitySettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var adapter: ElementsSettingsAdapter
    private var elementToEdit: Element? = null // Хранит элемент, который редактируется

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val settingsRecycler = binding.settingsRecycler
        val db = ElementsDatabase.getDatabase(this)

        settingsRecycler.layoutManager = LinearLayoutManager(this)
        adapter = ElementsSettingsAdapter(
            emptyList(),
            onEditClick = { element:Element ->
                // Подготовка к редактированию элемента
                elementToEdit = element
                binding.etName.setText(element.name)
                binding.etPrice.setText(element.price.toString())
                binding.etWeight.setText(element.weight.toString())
                binding.savebt.text = "Сохранить изменения" // Изменяем текст кнопки
            },
            onDeleteClick = { element:Element ->
                // Удаление элемента
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Удаление элемента")
                builder.setMessage("Вы уверены, что хотите удалить элемент \"${element.name}\"?")
                builder.setPositiveButton("Удалить") { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        db.elementDao().deleteElement(element)
                        val updatedElements = db.elementDao().getAllElements()
                        withContext(Dispatchers.Main) {
                            adapter.updateData(updatedElements)
                        }
                    }
                }
                builder.setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
                builder.show()
            }
        )

        settingsRecycler.adapter = adapter

        // Загрузка данных из базы
        CoroutineScope(Dispatchers.IO).launch {
            val elements = db.elementDao().getAllElements()
            withContext(Dispatchers.Main) {
                adapter.updateData(elements)
            }
        }

        binding.savebt.setOnClickListener {
            val name = binding.etName.text.toString()
            val priceText = binding.etPrice.text.toString()
            val weightText = binding.etWeight.text.toString()
            // Валидация ввода
            var hasError = false
            if (name.isEmpty()) {
                binding.etName.error = "Имя не может быть пустым"
                hasError = true
            }
            val price = priceText.toIntOrNull()
            if (price == null) {
                binding.etPrice.error = "Введите корректное число"
                hasError = true
            }
            val weight = weightText.toDoubleOrNull()
            if (weight == null) {
                binding.etWeight.error = "Введите корректное число"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            CoroutineScope(Dispatchers.IO).launch {
                if (elementToEdit != null) {
                    // Редактирование элемента
                    val updatedElement = elementToEdit!!.copy(name = name, price = price!!, weight = weight!!)
                    db.elementDao().updateElement(updatedElement)
                    val updatedElements = db.elementDao().getAllElements()
                    withContext(Dispatchers.Main) {
                        adapter.updateData(updatedElements)
                        resetInputFields()
                    }
                } else {
                    // Добавление нового элемента
                    val newElement = Element(null, name, price!!,weight!!)
                    db.elementDao().insertElement(newElement)
                    val updatedElements = db.elementDao().getAllElements()
                    withContext(Dispatchers.Main) {
                        adapter.updateData(updatedElements)
                        resetInputFields()
                    }
                }
            }
        }
    }

    private fun resetInputFields() {
        binding.etName.text.clear()
        binding.etPrice.text.clear()
        binding.etWeight.text.clear()
        binding.savebt.text = "Добавить" // Сбрасываем текст кнопки
        elementToEdit = null // Сбрасываем текущий элемент для редактирования
    }
}
