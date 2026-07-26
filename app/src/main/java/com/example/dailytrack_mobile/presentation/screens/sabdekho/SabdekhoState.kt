data class SabdekhoState(
    val isLoading: Boolean = false,
    val movies: List<Movie> = emptyList(), // list of movies
    val error: String? = null,

    // Search state
    val searchQuery: String = "",
    val isSearching: Boolean = false
)

data class Movie(
    val id: Int,
    val title: String,
    val year: Int,
    val posterUrl: String,
    val rating: Double
)