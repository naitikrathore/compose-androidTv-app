package tv.anoki.ondemand.test_helpers

import com.google.gson.GsonBuilder
import java.io.InputStreamReader
import java.lang.reflect.Type
import kotlin.coroutines.CoroutineContext

fun <T> readFileFromResources(context: CoroutineContext, jsonFile: String, typeOfT: Type): T {
    val gsonBuilder = GsonBuilder()
        .addDeserializationExclusionStrategy(SuperclassExclusionStrategy())
        .addSerializationExclusionStrategy(SuperclassExclusionStrategy())
        .create()
    return gsonBuilder.fromJson(
        InputStreamReader(context.javaClass.getResourceAsStream(jsonFile)),
        typeOfT
    )
}