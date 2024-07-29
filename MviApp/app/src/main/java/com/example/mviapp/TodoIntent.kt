package com.example.mviapp

sealed class TodoIntent {
    data class AddTodoItem(val item: TodoItem) : TodoIntent()
    data class UpdateTodoItem(val item: TodoItem) : TodoIntent()
}