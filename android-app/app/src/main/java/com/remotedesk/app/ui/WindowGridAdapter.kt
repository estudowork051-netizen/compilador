package com.remotedesk.app.ui

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.remotedesk.app.R
import com.remotedesk.app.databinding.ItemWindowCardBinding
import com.remotedesk.app.models.WindowItem

/**
 * Adaptador de grade responsiva para exibicao visual prioritariamente por ICONES dos programas
 * e janelas abertos no computador Windows.
 */
class WindowGridAdapter(
    private val onItemSelected: (WindowItem) -> Unit
) : RecyclerView.Adapter<WindowGridAdapter.WindowViewHolder>() {

    private var items: List<WindowItem> = emptyList()
    private var selectedPosition: Int = -1

    constructor(items: List<WindowItem>, onItemSelected: (WindowItem) -> Unit) : this(onItemSelected) {
        this.items = items
    }

    fun updateItems(newItems: List<WindowItem>) {
        items = newItems
        selectedPosition = -1
        notifyDataSetChanged()
    }

    fun getSelectedItem(): WindowItem? {
        return if (selectedPosition in items.indices) items[selectedPosition] else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowViewHolder {
        val binding = ItemWindowCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WindowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WindowViewHolder, position: Int) {
        holder.bind(items[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = items.size

    inner class WindowViewHolder(private val binding: ItemWindowCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WindowItem, isSelected: Boolean) {
            val context = itemView.context

            // 1. Vinculação do Título e Processo
            binding.tvWindowTitle.text = if (item.title.isNotBlank()) item.title else item.processName
            binding.tvProcessName.text = item.processName

            // 2. Indicador de Janela com Foco Ativo no Windows
            binding.tvActiveBadge.visibility = if (item.isActive) View.VISIBLE else View.GONE

            // 3. Estratégia Prioritária de Ícones:
            // a) Ícone real em Base64 extraído do Windows
            var iconSet = false
            if (!item.iconBase64.isNullOrBlank()) {
                try {
                    val decodedBytes = Base64.decode(item.iconBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) {
                        binding.ivWindowIcon.setImageBitmap(bitmap)
                        iconSet = true
                    }
                } catch (e: Exception) {
                    iconSet = false
                }
            }

            // b) Ícone semântico categorizado por tipo de aplicativo (fallback sempre presente)
            if (!iconSet) {
                val iconRes = when (item.iconType.lowercase()) {
                    "browser" -> R.drawable.ic_app_browser
                    "editor" -> R.drawable.ic_app_editor
                    "terminal" -> R.drawable.ic_app_terminal
                    "file_manager" -> R.drawable.ic_app_files
                    "media" -> R.drawable.ic_app_media
                    "calculator" -> R.drawable.ic_app_calc
                    "settings" -> R.drawable.ic_app_settings
                    else -> R.drawable.ic_app_generic
                }
                binding.ivWindowIcon.setImageResource(iconRes)
            }

            // 4. Efeito Visual de Seleção
            if (isSelected) {
                binding.ivSelectedBadge.visibility = View.VISIBLE
                binding.cardWindowItem.setStrokeColor(ContextCompat.getColor(context, R.color.primary))
                binding.cardWindowItem.strokeWidth = dpToPx(context, 2)
                binding.cardWindowItem.cardElevation = dpToPx(context, 4).toFloat()
            } else {
                binding.ivSelectedBadge.visibility = View.GONE
                binding.cardWindowItem.setStrokeColor(ContextCompat.getColor(context, R.color.divider))
                binding.cardWindowItem.strokeWidth = dpToPx(context, 1)
                binding.cardWindowItem.cardElevation = dpToPx(context, 1).toFloat()
            }

            // 5. Clique no ícone da grade seleciona o aplicativo sem disparar captura de tela
            binding.cardWindowItem.setOnClickListener {
                val previous = selectedPosition
                @Suppress("DEPRECATION")
                val current = if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    bindingAdapterPosition
                } else {
                    adapterPosition
                }
                if (current != RecyclerView.NO_POSITION) {
                    selectedPosition = current
                    if (previous != -1) notifyItemChanged(previous)
                    notifyItemChanged(current)
                    onItemSelected(item)
                }
            }
        }

        private fun dpToPx(context: android.content.Context, dp: Int): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }
    }
}
