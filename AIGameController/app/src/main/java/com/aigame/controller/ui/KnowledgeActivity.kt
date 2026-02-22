package com.aigame.controller.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aigame.controller.GameControllerApp
import com.aigame.controller.R
import com.aigame.controller.databinding.ActivityKnowledgeBinding
import com.aigame.controller.data.entity.ItemEntity
import com.aigame.controller.data.entity.SkillEntity
import com.aigame.controller.model.Knowledge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KnowledgeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKnowledgeBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    private var skills: List<SkillEntity> = emptyList()
    private var items: List<ItemEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKnowledgeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.knowledge_title)

        setupViews()
        loadData()
    }

    private fun setupViews() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.tabSkills.setOnClickListener {
            showSkills()
        }

        binding.tabItems.setOnClickListener {
            showItems()
        }

        // 默认显示技能
        binding.tabSkills.isSelected = true
    }

    private fun loadData() {
        scope.launch {
            val database = (application as GameControllerApp).knowledgeDatabase

            skills = withContext(Dispatchers.IO) {
                database.skillDao().getAll()
            }

            items = withContext(Dispatchers.IO) {
                database.itemDao().getAll()
            }

            updateCount()
            showSkills()
        }
    }

    private fun updateCount() {
        binding.tabSkills.text = "${getString(R.string.knowledge_tab_skills)} (${skills.size})"
        binding.tabItems.text = "${getString(R.string.knowledge_tab_items)} (${items.size})"
    }

    private fun showSkills() {
        binding.tabSkills.isSelected = true
        binding.tabItems.isSelected = false

        if (skills.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.text = getString(R.string.knowledge_empty)
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.adapter = SkillAdapter(skills)
        }
    }

    private fun showItems() {
        binding.tabSkills.isSelected = false
        binding.tabItems.isSelected = true

        if (items.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.emptyView.text = getString(R.string.knowledge_empty)
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.adapter = ItemAdapter(items)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // 技能适配器
    inner class SkillAdapter(private val data: List<SkillEntity>) :
        RecyclerView.Adapter<SkillAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val skill = data[position]
            holder.title.text = skill.name
            holder.subtitle.text = skill.description.take(50) +
                    if (skill.description.length > 50) "..." else ""
        }

        override fun getItemCount() = data.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(android.R.id.text1)
            val subtitle: TextView = view.findViewById(android.R.id.text2)
        }
    }

    // 物品适配器
    inner class ItemAdapter(private val data: List<ItemEntity>) :
        RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = data[position]
            holder.title.text = item.name
            holder.subtitle.text = item.description?.take(50) ?: "无描述"
        }

        override fun getItemCount() = data.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(android.R.id.text1)
            val subtitle: TextView = view.findViewById(android.R.id.text2)
        }
    }
}
