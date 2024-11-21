package com.example.calculationofscaffolding.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calculationofscaffolding.models.Element
import com.example.myapplication.R
class WallElementAdapter(private var elements: List<Element>, private val isPriceView: Boolean = true) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_BASIC = 0
        const val TYPE_EXTENDED = 1
    }

    // ViewHolder для базового элемента (для отображения цены)
    class BasicElementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val elementName: TextView = view.findViewById(R.id.tvElementName)
        val elementQuantity: TextView = view.findViewById(R.id.tvElementQuantity)
        val elementPrice: TextView = view.findViewById(R.id.tvElementPrice)
        val elementAllPrice: TextView = view.findViewById(R.id.tv_allPrice)
    }

    // ViewHolder для расширенного элемента (для отображения веса)
    class ExtendedElementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val elementName: TextView = view.findViewById(R.id.tvElementName)
        val elementQuantity: TextView = view.findViewById(R.id.tvElementQuantity)
        val elementPrice: TextView = view.findViewById(R.id.tvElementPrice)
        val elementAllPrice: TextView = view.findViewById(R.id.tv_allPrice)
        val elementWeight: TextView = view.findViewById(R.id.tv_ElementWeight)
        val elementTotalWeight: TextView = view.findViewById(R.id.tv_ElementTotalWeight)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_BASIC -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wall_element1, parent, false)
                BasicElementViewHolder(view) // Здесь создаём BasicElementViewHolder
            }
            TYPE_EXTENDED -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wall_element2, parent, false)
                ExtendedElementViewHolder(view) // Здесь создаём ExtendedElementViewHolder
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val element = elements[position]
        when (holder) {
            is BasicElementViewHolder -> {
                holder.elementName.text = element.name
                holder.elementQuantity.text = "Кол-во: ${element.quantity}"
                holder.elementPrice.text = "Цена: ${element.price}"
                holder.elementAllPrice.text = "Сумма: ${element.price * element.quantity}"
            }
            is ExtendedElementViewHolder -> {
                holder.elementName.text = element.name
                holder.elementQuantity.text = "Кол-во: ${element.quantity}"
                holder.elementPrice.text = "Цена: ${element.price}"
                holder.elementAllPrice.text = "Сумма: ${element.price * element.quantity}"
                holder.elementWeight.text = "Вес:${element.weight}"
                holder.elementTotalWeight.text = "Общий вес:${element.weight * element.quantity}"
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        // Возвращаем тип представления в зависимости от того, нужно ли показывать вес
        return if (isPriceView) TYPE_BASIC else TYPE_EXTENDED
    }

    override fun getItemCount(): Int = elements.size

    fun submitList(newElements: List<Element>) {
        elements = newElements
        notifyDataSetChanged()
    }
}
