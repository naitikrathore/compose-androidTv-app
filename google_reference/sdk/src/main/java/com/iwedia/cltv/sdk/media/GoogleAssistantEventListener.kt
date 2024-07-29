package com.iwedia.cltv.sdk.media

/**
 * Interface used to sent debug communication status, as well as additional data, that is
 * sent between GoogleAssistant and the ContentProvider.
 *
 * NOTE:
 *
 * This interface between GoogleAssistantContentProvider and MainActivity is only used
 * for debug purposes (to display the data flow between GA and ContentProvider) and is not needed
 * (nor recommended) to exist in the application in order for GA functionality to work.
 */
interface GoogleAssistantEventListener {
  /**
   * Event triggered when query received from GoogleAssistant
   */
  fun onQueryReceived(query: String)

  /**
   * Event triggered when search result is returned to GoogleAssistant
   */
  fun onQueryResults(resultsJson: String)
}
