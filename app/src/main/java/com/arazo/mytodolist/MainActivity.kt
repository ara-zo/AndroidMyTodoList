package com.arazo.mytodolist

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arazo.mytodolist.databinding.ActivityMainBinding
import com.arazo.mytodolist.databinding.ItemTodoBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
	private lateinit var binding: ActivityMainBinding

	private val viewModel: MainViewModel by viewModels()

	private val signInLauncher = registerForActivityResult(
		FirebaseAuthUIActivityResultContract()
	) { res ->
		this.onSignInResult(res)
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		val inflater: MenuInflater = menuInflater
		inflater.inflate(R.menu.main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle item selection
		return when (item.itemId) {
			R.id.action_log_out -> {
				logout()
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		val view = binding.root
		setContentView(view)

		// 로그인을 안했을 때
		if (FirebaseAuth.getInstance().currentUser == null) {
			login()
		}

		binding.containerRecyclerView.apply {
			layoutManager = LinearLayoutManager(this@MainActivity)
			adapter = TodoAdapter(
				emptyList(),
				onClickDeleteIcon = {
					viewModel.deleteTodo(it)
				},
				onClickItem = {
					viewModel.toggleTodo(it)
				}
			)
		}

		binding.btnAdd.setOnClickListener {
			val todo = Todo(binding.etText.text.toString())
			viewModel.addTodo(todo)
		}

		// 관찰 UI 업데이트
		viewModel.todoLiveData.observe(this@MainActivity, Observer {
			(binding.containerRecyclerView.adapter as TodoAdapter).setData(it)
		})
	}


	private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
		val response = result.idpResponse
		if (result.resultCode == RESULT_OK) {
			// Successfully signed in
			viewModel.fetchData()
		} else {
			// 로그인 실패
			finish()
		}
	}

	fun login() {
		val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build())

		val signInIntent = AuthUI.getInstance()
			.createSignInIntentBuilder()
			.setAvailableProviders(providers)
			.build()
		signInLauncher.launch(signInIntent)
	}

	fun logout() {
		AuthUI.getInstance()
			.signOut(this)
			.addOnCompleteListener {
				login()
			}
	}
}

data class Todo(
	val text: String,
	var isDone: Boolean = false,
)

class TodoAdapter(
	private var myDataset: List<Todo>,
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

	fun setData(newData: List<Todo>) {
		myDataset = newData
		notifyDataSetChanged()
	}
}

class MainViewModel : ViewModel() {
	// Firebase DB
	val db = Firebase.firestore

	// 변경가능하고 관찰가능한 라이브데이터
	val todoLiveData = MutableLiveData<List<Todo>>()

	private val data = arrayListOf<Todo>()

	init {
		fetchData()
	}

	fun fetchData() {
		val user = FirebaseAuth.getInstance().currentUser
		if (user != null) {
			db.collection(user.uid)
				.addSnapshotListener { value, e ->
					if (e != null) {
						return@addSnapshotListener
					}

					data.clear()
					for (document in value!!) {
						val todo = Todo(
							document.getString("text") ?: "",
							document.getBoolean("isDone") ?: false
						)
						data.add(todo)
					}
					todoLiveData.value = data
				}
		}
	}

	fun toggleTodo(todo: Todo) {
		todo.isDone = !todo.isDone
		todoLiveData.value = data
	}

	fun addTodo(todo: Todo) {
		FirebaseAuth.getInstance().currentUser?.let { user ->
			db.collection(user.uid).add(todo)
		}
	}

	fun deleteTodo(todo: Todo) {
		data.remove(todo)
		todoLiveData.value = data
	}
}

