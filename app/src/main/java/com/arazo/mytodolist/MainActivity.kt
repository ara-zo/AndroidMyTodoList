package com.arazo.mytodolist

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arazo.mytodolist.databinding.ActivityMainBinding
import com.arazo.mytodolist.databinding.ItemTodoBinding

class MainActivity : AppCompatActivity() {
	private lateinit var binding: ActivityMainBinding

	private val viewModel: MainViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		val view = binding.root

		binding.containerRecyclerView.apply {
			layoutManager = LinearLayoutManager(this@MainActivity)
			adapter = TodoAdapter(
				viewModel.data,
				onClickDeleteIcon = {
					viewModel.deleteTodo(it)
					binding.containerRecyclerView.adapter?.notifyDataSetChanged()
				},
				onClickItem = {
					viewModel.toggleTodo(it)
					binding.containerRecyclerView.adapter?.notifyDataSetChanged()
				}
			)
		}

		binding.btnAdd.setOnClickListener {
			val todo = Todo(binding.etText.text.toString())
			viewModel.addTodo(todo)
			binding.containerRecyclerView.adapter?.notifyDataSetChanged()
		}

		setContentView(view)
	}
}

data class Todo(
	val text: String,
	var isDone: Boolean = false,
)

class TodoAdapter(
	private val myDataset: List<Todo>,
	val onClickDeleteIcon: (todo: Todo) -> Unit, // input이 하나고 output이 없는 함수
	val onClickItem: (todo: Todo) -> Unit,
) :
	RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

	class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)

	override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TodoViewHolder {
		val view = LayoutInflater.from(viewGroup.context)
			.inflate(R.layout.item_todo, viewGroup, false)

		return TodoViewHolder(ItemTodoBinding.bind(view))
	}

	override fun onBindViewHolder(viewHolder: TodoViewHolder, position: Int) {
		val todo = myDataset[position]
		viewHolder.binding.etTodo.text = todo.text

		when (todo.isDone) {
			true -> {
				viewHolder.binding.etTodo.apply {
					paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
					setTypeface(null, Typeface.ITALIC)
				}
			}
			else -> {
				viewHolder.binding.etTodo.apply {
					paintFlags = 0
					setTypeface(null, Typeface.NORMAL)
				}
			}
		}

		viewHolder.binding.deleteBtn.setOnClickListener {
			onClickDeleteIcon.invoke(todo)
		}

		viewHolder.binding.root.setOnClickListener {
			// .invoke() : 이 함수를 실행
			onClickItem.invoke(todo)
		}
	}

	override fun getItemCount() = myDataset.size
}

class MainViewModel : ViewModel() {
	val data = arrayListOf<Todo>()

	fun toggleTodo(todo: Todo) {
		todo.isDone = !todo.isDone
	}

	fun addTodo(todo: Todo) {
		data.add(todo)
	}

	fun deleteTodo(todo: Todo) {
		data.remove(todo)
	}
}