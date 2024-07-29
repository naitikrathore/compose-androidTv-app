package com.example.devjetreplica

data class Movie(
    val id: Long = 0,
    var title: String,
    var img: Int,
    var isFav: Int = 0,
    var isRecent: Int = 0
)

data class Section(
    val title: String,
    val movieList: List<Movie> = emptyList()
)

val dummyMovies = listOf(
    Movie(id = 1, title = "Movie 1", img = R.drawable.movie1, isFav = 1, isRecent = 1),
    Movie(id = 2, title = "Movie 2", img = R.drawable.movie2, isFav = 0, isRecent = 1),
    Movie(id = 3, title = "Movie 3", img = R.drawable.movie3, isFav = 0, isRecent = 1),
    Movie(id = 4, title = "Movie 4", img = R.drawable.movie4, isFav = 0, isRecent = 1),
    Movie(id = 5, title = "Movie 5", img = R.drawable.movie5, isFav = 1, isRecent = 1),
    Movie(id = 6, title = "Movie 6", img = R.drawable.movie9, isFav = 0, isRecent = 1),
    Movie(id = 7, title = "Movie 7", img = R.drawable.movie7, isFav = 1, isRecent = 1),
    Movie(id = 8, title = "Movie 8", img = R.drawable.movie8, isFav = 0, isRecent = 1),
    Movie(id = 9, title = "Movie 9", img = R.drawable.movie9, isFav = 0, isRecent = 1),
    Movie(id = 10, title = "Movie 1", img = R.drawable.movie1, isFav = 1, isRecent = 1),
    Movie(id = 11, title = "Movie 2", img = R.drawable.movie2, isFav = 0, isRecent = 0),
    Movie(id = 12, title = "Movie 3", img = R.drawable.movie3, isFav = 0, isRecent = 1),
    Movie(id = 13, title = "Movie 4", img = R.drawable.movie4, isFav = 0, isRecent = 1),
    Movie(id = 14, title = "Movie 5", img = R.drawable.movie5, isFav = 1, isRecent = 1),
    Movie(id = 15, title = "Movie 6", img = R.drawable.movie9, isFav = 0, isRecent = 1),
    Movie(id = 16, title = "Movie 7", img = R.drawable.movie7, isFav = 1, isRecent = 1),
    Movie(id = 17, title = "Movie 8", img = R.drawable.movie8, isFav = 0, isRecent = 1),
    Movie(id = 18, title = "Movie 9", img = R.drawable.movie9, isFav = 0, isRecent = 1)
)

val dummySections = listOf(
    Section(title = "Section 1", movieList = dummyMovies),
    Section(title = "Section 2", movieList = dummyMovies.shuffled()), // Shuffle to create variety
    Section(title = "Section 3", movieList = dummyMovies),
    Section(title = "Section 4", movieList = dummyMovies.shuffled())
)
