package com.example.tvapp.data

import com.example.tvapp.R

object MovieData {
    val CATEGORY = arrayOf(
        "Thrill",
        "Comedy",
        "Action",
        "Horror",
        "Drama"
    )

    val list: List<MovieDataType> by lazy {
        readyData()
    }

    private fun readyData(): List<MovieDataType> {
        val titles = arrayOf(
            "The Godfather",
            "The Dark Knight",
            "Avatar",
            "Forrest Gump",
            "Fight Club"
        )

        val description = "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son. " +
                "Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice when the Joker emerges from his mysterious past. " +
                "A paraplegic Marine dispatched to the moon Pandora on a unique mission becomes torn between following his orders and protecting the world he feels is his home." +
                "amet mi accumsan mollis sed et magna. Vivamus sed aliquam risus. Nulla eget dolor in elit " +
                "The story of Forrest Gump, a man with a low IQ, who recounts the early years of his life when he found himself in the middle of key historical events. " +
                "An insomniac office worker, looking for a way to change his life, crosses paths with a devil-may-care soap maker, forming an underground fight club that evolves into something much more."

        val studios = arrayOf(
            "Fox Studio",
            "Eros Now",
            "Dharma Production",
            "Maddock",
            "Club House"
        )

        val videoURLs = arrayOf(
            "https://commogodndatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review.mp4",
            "https://commondatastorage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft%20Search.mp4",
            "https://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail%20Blue.mp4",
            "https://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Fiber%20to%20the%20Pole.mp4",
            "https://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose.mp4"
        )

        val backImgs = arrayOf(
            R.drawable.movie1,
            R.drawable.movie2,
            R.drawable.movie3,
            R.drawable.movie4,
            R.drawable.movie5
        )

        return titles.indices.map {
            buildMovieInfo(
                titles[it],
                description,
                studios[it],
                videoURLs[it],
                backImgs[it]
            )
        }
    }

    private var count: Long = 0

    private fun buildMovieInfo(
        title: String,
        description: String,
        studio: String,
        videoUrl: String,
        backgroundImageUrl: Int
    ): MovieDataType {
        return MovieDataType(
            id = count++,
            title = title,
            description = description,
            studio = studio,
            videoURL = videoUrl,
            backgroundImage = backgroundImageUrl
        )
    }
}
