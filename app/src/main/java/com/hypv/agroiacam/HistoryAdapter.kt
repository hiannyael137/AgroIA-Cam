package com.hypv.agroiacam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private val historyList: List<HistoryItem>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {

        val tvResultType: TextView =
            view.findViewById(R.id.tvResultType)

        val tvPlantName: TextView =
            view.findViewById(R.id.tvPlantName)

        val tvBadge: TextView =
            view.findViewById(R.id.tvBadge)

        val tvPrecision: TextView =
            view.findViewById(R.id.tvPrecision)

        val progressBar: ProgressBar =
            view.findViewById(R.id.progressBar)

        val tvMethod: TextView =
            view.findViewById(R.id.tvMethod)

        val tvDate: TextView =
            view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HistoryViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_result,
                parent,
                false
            )

        return HistoryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    override fun onBindViewHolder(
        holder: HistoryViewHolder,
        position: Int
    ) {

        val item = historyList[position]

        holder.tvResultType.text =
            item.resultado

        holder.tvPlantName.text =
            "Planta: ${item.planta}"

        holder.tvBadge.text =
            item.estado

        holder.tvPrecision.text =
            "${item.confianza}%"

        holder.progressBar.progress =
            item.confianza

        holder.tvMethod.text =
            "Método: ${item.metodo}"

        holder.tvDate.text =
            item.fecha
    }
}