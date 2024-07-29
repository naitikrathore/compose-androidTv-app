package com.example.mviapp
class TodoViewModel(private val repository: TodoRepository) : ViewModel() {
    private val _viewState = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> = _viewState

    init {
        _viewState.value = ViewState(repository.getTodos())
    }

    fun processIntent(intent: TodoIntent) {
        when (intent) {
            is TodoIntent.AddTodoItem -> {
                repository.addTodoItem(intent.item)
                _viewState.value = ViewState(repository.getTodos())
            }
            is TodoIntent.UpdateTodoItem -> {
                repository.updateTodoItem(intent.item)
                _viewState.value = ViewState(repository.getTodos())
            }
        }
    }

    fun getNextId(): Int {
        return repository.getTodos().size + 1
    }
}
