package com.example.calculationofscaffolding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculationofscaffolding.DB.ElementsDatabase
import com.example.calculationofscaffolding.adapters.ElementAdapter
import com.example.calculationofscaffolding.adapters.WallAdapter
import com.example.calculationofscaffolding.adapters.WallElementAdapter
import com.example.calculationofscaffolding.models.Element
import com.example.calculationofscaffolding.models.Wall
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var walls:MutableList<Wall> = mutableListOf()
    private lateinit var wallAdapter: WallAdapter
    private lateinit var elementWallPriceAdapter: WallElementAdapter
    private lateinit var elementWallWeightAdapter: WallElementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val settingsbt: Button = binding.settingsbt
        val addWall:Button = binding.add

        setupRecyclers()

        settingsbt.setOnClickListener {
            settingsButtonClick(this)
        }
        addWall.setOnClickListener {
            it.isEnabled = false
            showWallInputDialog(wallAdapter)

            it.postDelayed({ it.isEnabled = true }, 500) // Разблокируем через 500 мс
        }




    }
    private fun settingsButtonClick(context: Context) {
        val intent = Intent(context, SettingsActivity::class.java)
        startActivity(intent)
    }
    private var isDialogVisible = false

    fun showWallInputDialog(wallAdapter: WallAdapter, wallToEdit: Wall? = null) {
        if (isDialogVisible) return
        isDialogVisible = true
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_wall, null)
        builder.setView(dialogView)
        val dialog = builder.create()

        val etWidth = dialogView.findViewById<EditText>(R.id.etWidth)
        val etHeight = dialogView.findViewById<EditText>(R.id.etHeight)
        val etTiers = dialogView.findViewById<EditText>(R.id.etTiers)
        val rvElements = dialogView.findViewById<RecyclerView>(R.id.rvElements)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        val database = ElementsDatabase.getDatabase(this)

        // Список элементов, загруженных из базы данных
        var elements: MutableList<Element> = mutableListOf()
        val selectedElements = mutableListOf<Element>()

        CoroutineScope(Dispatchers.IO).launch {
            // Запрос к базе данных в фоновом потоке
            val elementsFromDb = database.elementDao().getAllElements()
            withContext(Dispatchers.Main) {
                elements.clear()
                elements.addAll(elementsFromDb)
                val adapter = ElementAdapter(
                    elements,
                    onElementChanged = { element ->
                        if (selectedElements.none { it.id == element.id }) {
                            selectedElements.add(element)
                        }
                    },

                )



                rvElements.layoutManager = LinearLayoutManager(this@MainActivity)
                rvElements.adapter = adapter

                // Прокрутка списка к последнему элементу
                rvElements.post {
                    rvElements.smoothScrollToPosition(adapter.itemCount - 1)
                }
            }
        }

        // Если передана стена для редактирования, заполним поля данными этой стены


        // Обработчик кнопки "Отмена"
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setOnDismissListener { isDialogVisible = false }

        // Обработчик кнопки "Сохранить"
        btnSave.setOnClickListener {
            val width = etWidth.text.toString().toDoubleOrNull()
            val height = etHeight.text.toString().toDoubleOrNull()
            val tiers = etTiers.text.toString().toIntOrNull()

            // Чекбоксы для дополнительного выбора
            val isHeelSelected = dialogView.findViewById<CheckBox>(R.id.heel).isChecked
            val isJaskSelected = dialogView.findViewById<CheckBox>(R.id.jask).isChecked

            // Проверка на корректность введенных данных
            if (width != null && height != null && tiers != null) {
                // Проверка на делимость
                if (height % 2 != 0.0) {
                    Toast.makeText(this, "Высота должна быть кратна 2", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (width % 3 != 0.0) {
                    Toast.makeText(this, "Ширина должна быть кратна 3", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (tiers > (height / 2)) {
                    Toast.makeText(this, "Количество рабочих ярусов не может превышать высоту / 2", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Создание или обновление объекта стены
                val wall = Wall(
                    id = wallToEdit?.id ?: generateWallId(),  // Если стена редактируется, сохраняем её ID
                    width = width,
                    height = height,
                    tiers = tiers,
                    elements = selectedElements,  // Сохраняем все выбранные элементы
                    isHeelSelected = isHeelSelected,
                    isJaskSelected = isJaskSelected
                )

                // Если стена редактируется, обновляем её в списке, иначе добавляем новую
                if (wallToEdit != null) {
                    val index = walls.indexOfFirst { it.id == wallToEdit.id }
                    if (index != -1) {
                        walls[index] = wall
                    }
                } else {
                    walls.add(wall)
                }

                wallAdapter.updateWalls(walls)  // Обновляем адаптер с новыми данными
                updateTotal(walls)
                val elements = updateTotalElements(walls).toMutableList()
                elementWallPriceAdapter.submitList(elements)
                elementWallWeightAdapter.submitList(elements)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Пожалуйста, заполните все поля!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
    fun showWallEditDialog(wallAdapter: WallAdapter, wallToEdit: Wall) {
        if (isDialogVisible) return
        isDialogVisible = true
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_wall, null)
        builder.setView(dialogView)
        val dialog = builder.create()

        val etWidth = dialogView.findViewById<EditText>(R.id.etWidth)
        val etHeight = dialogView.findViewById<EditText>(R.id.etHeight)
        val etTiers = dialogView.findViewById<EditText>(R.id.etTiers)
        val rvElements = dialogView.findViewById<RecyclerView>(R.id.rvElements)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        val database = ElementsDatabase.getDatabase(this)

        // Список элементов, загруженных из базы данных
        var elements: MutableList<Element> = wallToEdit.elements.toMutableList()
        val selectedElements = mutableListOf<Element>()


        val adapter = ElementAdapter(
            elements,
            onElementChanged = { element ->
                if (selectedElements.none { it.id == element.id }) {
                    selectedElements.add(element)
                }
            },

        )


        rvElements.layoutManager = LinearLayoutManager(this)
        rvElements.adapter = adapter

        rvElements.post{
            rvElements.smoothScrollToPosition(adapter.itemCount-1)
        }

        // Заполняем поля данными стены, которую нужно редактировать
        etWidth.setText(wallToEdit.width.toString())
        etHeight.setText(wallToEdit.height.toString())
        etTiers.setText(wallToEdit.tiers.toString())
        selectedElements.addAll(wallToEdit.elements) // Заполняем выбранные элементы из стены

        // Обработчик кнопки "Отмена"
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setOnDismissListener { isDialogVisible = false }

        // Обработчик кнопки "Сохранить"
        btnSave.setOnClickListener {
            val width = etWidth.text.toString().toDoubleOrNull()
            val height = etHeight.text.toString().toDoubleOrNull()
            val tiers = etTiers.text.toString().toIntOrNull()

            // Чекбоксы для дополнительного выбора
            val isHeelSelected = dialogView.findViewById<CheckBox>(R.id.heel).isChecked
            val isJaskSelected = dialogView.findViewById<CheckBox>(R.id.jask).isChecked

            // Проверка на корректность введенных данных
            if (width != null && height != null && tiers != null) {
                // Проверка на делимость
                if (height % 2 != 0.0) {
                    Toast.makeText(this, "Высота должна быть кратна 2", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (width % 3 != 0.0) {
                    Toast.makeText(this, "Ширина должна быть кратна 3", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (tiers > (height / 2)) {
                    Toast.makeText(this, "Количество рабочих ярусов не может превышать высоту / 2", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Обновление данных стены
                val updatedWall = Wall(
                    id = wallToEdit.id,  // Оставляем старое ID, так как это редактирование
                    width = width,
                    height = height,
                    tiers = tiers,
                    elements = selectedElements,  // Сохраняем все выбранные элементы
                    isHeelSelected = isHeelSelected,
                    isJaskSelected = isJaskSelected
                )

                // Обновление стены в списке
                val index = walls.indexOfFirst { it.id == wallToEdit.id }
                if (index != -1) {
                    walls[index] = updatedWall
                }

                wallAdapter.updateWalls(walls)
                val elements = updateTotalElements(walls).toMutableList()
                elementWallPriceAdapter.submitList(elements)
                elementWallWeightAdapter.submitList(elements)

                updateTotal(walls)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Пожалуйста, заполните все поля!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    
    fun generateWallId(): Int {
        return System.currentTimeMillis().toInt()  // Генерация уникального ID для стены
    }

    fun setupRecyclers(){
        setupWallsRecycler()
        setupTotalWallPriceRecycler()
        setupTotalWallWeightRecycler()
    }
    fun setupWallsRecycler(){
        val recyclerView = binding.rvWalls
        recyclerView.layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        wallAdapter = WallAdapter(walls,
            onEdit = {
                showWallEditDialog(wallAdapter, it)
            },
            onDelete = {
                val builder = android.app.AlertDialog.Builder(this)
                builder.setTitle("Удаление элемента")
                builder.setMessage("Вы уверены, что хотите удалить эту стену}\"?")
                builder.setPositiveButton("Удалить") { _, _ ->
                    val index = walls.indexOf(it)
                    if (index != -1) {
                        walls.removeAt(index)
                        wallAdapter.notifyItemRemoved(index)
                    }
                }
                builder.setNegativeButton("Отмена ") { dialog, _ -> dialog.dismiss() }
                builder.show()

            }
        )
        recyclerView.adapter = wallAdapter


    }

    fun setupTotalWallPriceRecycler(){
        val elements = updateTotalElements(walls).toMutableList()
        elementWallPriceAdapter = WallElementAdapter(elements = elements)

        val totalWallPriceRecycler:RecyclerView = binding.totalWallsPriceRecycler
        totalWallPriceRecycler.layoutManager = LinearLayoutManager(this)
        totalWallPriceRecycler.adapter = elementWallPriceAdapter
    }
    fun setupTotalWallWeightRecycler(){
        val elements = updateTotalElements(walls).toMutableList()
        elementWallWeightAdapter = WallElementAdapter(elements = elements,false)

        val totalWallPriceRecycler:RecyclerView = binding.totalWallsWeightRecycler
        totalWallPriceRecycler.layoutManager = LinearLayoutManager(this)
        totalWallPriceRecycler.adapter = elementWallWeightAdapter
    }

    fun updateTotal(walls: List<Wall>){
        updateTotalWallSquare(walls)
        updateTotalWalsPrice(walls)
        updateTotalWallsWeight(walls)
    }
    fun updateTotalWallSquare(walls:List<Wall>){
        var totalSquare = walls.sumOf {
            it.height * it.width
        }
        var tvTotalWallSquare:TextView = binding.totalWallsSquare

        tvTotalWallSquare.text = "Общая площадь: ${totalSquare} м2"
    }
    fun updateTotalWalsPrice(walls:List<Wall>){
        val totalPrice = walls.sumOf {
            it.elements.sumOf { it.price * it.quantity }
        }
        var tvTotalWallPrice:TextView = binding.totalWallsPrice
        tvTotalWallPrice.text = "Общая цена элементов: ${totalPrice} p"

    }
    fun updateTotalWallsWeight(walls: List<Wall>){
        val totalWeight = walls.sumOf {
            it.elements.sumOf { it.weight * it.quantity}
        }
        var tvTotalWeight:TextView = binding.totalWallsWeight
        tvTotalWeight.text = "Общий вес элементов : ${totalWeight} кг"
    }
    fun updateTotalElements(walls: List<Wall>): List<Element> {
        // Создаем Map для хранения обновленных элементов с локальными полями для quantity
        val elementsMap = mutableMapOf<String, Element>()

        walls.forEach { wall ->
            wall.elements.forEach { element ->
                // Проверяем, есть ли этот элемент в нашем списке
                val existingElement = elementsMap[element.name]

                if (existingElement != null) {
                    // Если элемент уже есть, увеличиваем количество
                    existingElement.quantity += element.quantity
                } else {
                    // Если элемента нет, создаем новый элемент вручную
                    val newElement = Element(
                        id = element.id,
                        name = element.name,
                        price = element.price,
                        weight = element.weight
                    ).apply {
                        quantity = element.quantity // Устанавливаем значение quantity
                    }
                    elementsMap[element.name] = newElement
                }
            }
        }

        // Возвращаем список элементов из Map
        return elementsMap.values.toList()
    }










}
