package com.example.calculationofscaffolding.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import android.widget.EditText
import android.widget.TextView
import com.example.calculationofscaffolding.models.Element
import com.example.myapplication.R
class ElementAdapter(
    private var elements: MutableList<Element>, // Модели элементов
    private val onElementChanged: (Element) -> Unit = {} // Колбэк для обновления
) : RecyclerView.Adapter<ElementAdapter.ElementViewHolder>() {

    inner class ElementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val etQuantity: EditText = itemView.findViewById(R.id.etQuantity)

        init {
            // Добавляем обработчик изменения количества
            etQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(editable: Editable?) {
                    val quantity = editable.toString().toIntOrNull() ?: 0
                    // Обновляем количество у элемента
                    elements[adapterPosition].quantity = quantity
                    // Оповещаем об изменении элемента
                    onElementChanged(elements[adapterPosition])
                }
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_element, parent, false)
        return ElementViewHolder(view)
    }

    override fun onBindViewHolder(holder: ElementViewHolder, position: Int) {
        val element = elements[position]
        holder.tvName.text = element.name
        holder.tvPrice.text = element.price.toString()
        holder.etQuantity.setText(element.quantity.toString()) // Отображаем текущее количество
    }

    override fun getItemCount(): Int = elements.size

    // Обновление данных в адаптере
    fun submitList(newElements: List<Element>) {

        elements = newElements.toMutableList()
        notifyDataSetChanged() // Обновление всех элементов
    }

    // Метод для обновления одного элемента в списке
    fun updateElement(element: Element) {
        val index = elements.indexOfFirst { it.id == element.id }
        if (index != -1) {
            elements[index] = element
            notifyItemChanged(index) // Обновляем конкретный элемент
        }
    }
}


